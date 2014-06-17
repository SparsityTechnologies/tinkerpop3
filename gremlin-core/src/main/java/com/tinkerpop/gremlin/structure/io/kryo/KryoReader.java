package com.tinkerpop.gremlin.structure.io.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.tinkerpop.gremlin.structure.Direction;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Property;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.io.GraphReader;
import com.tinkerpop.gremlin.structure.util.batch.BatchGraph;
import com.tinkerpop.gremlin.util.function.QuadConsumer;
import com.tinkerpop.gremlin.util.function.QuintFunction;
import com.tinkerpop.gremlin.util.function.TriFunction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

/**
 * The {@link GraphReader} for the Gremlin Structure serialization format based on Kryo.  The format is meant to be
 * non-lossy in terms of Gremlin Structure to Gremlin Structure migrations (assuming both structure implementations
 * support the same graph features).
 * <br/>
 * This implementation is not thread-safe.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class KryoReader implements GraphReader {
    private final Kryo kryo;
    private final GremlinKryo.HeaderReader headerReader;

    private final long batchSize;
    private final String vertexIdKey;
    private final String edgeIdKey;

    private final File tempFile;

    final AtomicLong counter = new AtomicLong(0);

    private KryoReader(final File tempFile, final long batchSize,
                       final String vertexIdKey, final String edgeIdKey,
                       final GremlinKryo gremlinKryo) {
        this.kryo = gremlinKryo.createKryo();
        this.headerReader = gremlinKryo.getHeaderReader();
        this.vertexIdKey = vertexIdKey;
        this.edgeIdKey = edgeIdKey;
        this.tempFile = tempFile;
        this.batchSize = batchSize;
    }

    @Override
    public Vertex readVertex(final InputStream inputStream,
                             final Direction directionRequested,
                             final TriFunction<Object, String, Object[], Vertex> vertexMaker,
                             final QuintFunction<Object, Object, Object, String, Object[], Edge> edgeMaker) throws IOException {
        final Input input = new Input(inputStream);
		return readVertex(directionRequested, vertexMaker, edgeMaker, input);
    }

	@Override
	public Iterator<Vertex> readVertices(final InputStream inputStream, final Direction direction,
							   			 final TriFunction<Object, String, Object[], Vertex> vertexMaker,
							   			 final QuintFunction<Object, Object, Object, String, Object[], Edge> edgeMaker) throws IOException {
		final Input input = new Input(inputStream);
		return new Iterator<Vertex>() {
			@Override
			public boolean hasNext() {
				return !input.eof();
			}

			@Override
			public Vertex next() {
				try {
					final Vertex v = readVertex(direction, vertexMaker, edgeMaker, input);

					// read the vertex terminator
					kryo.readClassAndObject(input);

					return v;
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		};
	}

	@Override
    public Vertex readVertex(final InputStream inputStream, final TriFunction<Object, String, Object[], Vertex> vertexMaker) throws IOException {
        return readVertex(inputStream, null, vertexMaker, null);
    }

    @Override
    public Edge readEdge(final InputStream inputStream, final QuintFunction<Object, Object, Object, String, Object[], Edge> edgeMaker) throws IOException {
        final Input input = new Input(inputStream);
        this.headerReader.read(kryo, input);
        final Object outId = kryo.readClassAndObject(input);
        final Object inId = kryo.readClassAndObject(input);
        final Object edgeId = kryo.readClassAndObject(input);
        final String label = input.readString();
        final List<Object> edgeArgs = new ArrayList<>();
        readElementProperties(input, edgeArgs);

        return edgeMaker.apply(edgeId, outId, inId, label, edgeArgs.toArray());
    }

    @Override
    public void readGraph(final InputStream inputStream, final Graph graphToWriteTo) throws IOException {
        this.counter.set(0);
        final Input input = new Input(inputStream);
        this.headerReader.read(kryo, input);

        // will throw an exception if not constructed properly
        final BatchGraph graph = BatchGraph.create(graphToWriteTo)
                .vertexIdKey(vertexIdKey)
                .edgeIdKey(edgeIdKey)
                .bufferSize(batchSize).build();

        try (final Output output = new Output(new FileOutputStream(tempFile))) {
            final boolean supportedMemory = input.readBoolean();
            if (supportedMemory) {
                // if the graph that serialized the data supported memory then the memory needs to be read
                // to advance the reader forward.  if the graph being read into doesn't support the memory
                // then we just setting the data to memory.
                final Map<String, Object> memMap = (Map<String, Object>) kryo.readObject(input, HashMap.class);
                if (graphToWriteTo.getFeatures().graph().memory().supportsVariables()) {
                    final Graph.Variables variables = graphToWriteTo.variables();
                    memMap.forEach(variables::set);
                }
            }

            final boolean hasSomeVertices = input.readBoolean();
            if (hasSomeVertices) {
                while (!input.eof()) {
                    final List<Object> vertexArgs = new ArrayList<>();
                    final Object current = kryo.readClassAndObject(input);
                    vertexArgs.addAll(Arrays.asList(Element.ID, current));

                    vertexArgs.addAll(Arrays.asList(Element.LABEL, input.readString()));
                    readElementProperties(input, vertexArgs);

                    final Vertex v = graph.addVertex(vertexArgs.toArray());

                    // the gio file should have been written with a direction specified
                    final boolean hasDirectionSpecified = input.readBoolean();
                    final Direction directionInStream = kryo.readObject(input, Direction.class);
                    final Direction directionOfEdgeBatch = kryo.readObject(input, Direction.class);

                    // graph serialization requires that a direction be specified in the stream and that the
                    // direction of the edges be OUT
                    if (!hasDirectionSpecified || directionInStream != Direction.OUT || directionOfEdgeBatch != Direction.OUT)
                        throw new IllegalStateException(String.format("Stream must specify edge direction and that direction must be %s", Direction.OUT));

                    // if there are edges then read them to end and write to temp, otherwise read what should be
                    // the vertex terminator
                    if (!input.readBoolean())
                        kryo.readClassAndObject(input);
                    else {
                        // writes the real new id of the outV to the temp.  only need to write vertices to temp that
                        // have edges.  no need to reprocess those that don't again.
                        kryo.writeClassAndObject(output, v.id());
                        readToEndOfEdgesAndWriteToTemp(input, output);
                    }

                }
            }
        }
        // done writing to temp

        // start reading in the edges now from the temp file
        try (final Input edgeInput = new Input(new FileInputStream(tempFile))) {
            readFromTempEdges(edgeInput, graph);
        } finally {
            if (graph.getFeatures().graph().supportsTransactions())
                graph.tx().commit();

            deleteTempFileSilently();
        }
    }

	private Vertex readVertex(final Direction directionRequested, final TriFunction<Object, String, Object[], Vertex> vertexMaker,
							  final QuintFunction<Object, Object, Object, String, Object[], Edge> edgeMaker, final Input input) throws IOException {
		if (null != directionRequested && null == edgeMaker)
			throw new IllegalArgumentException("If a directionRequested is specified then an edgeAdder function should also be specified");

		this.headerReader.read(kryo, input);

		final List<Object> vertexArgs = new ArrayList<>();

		final Object vertexId = kryo.readClassAndObject(input);
		final String label = input.readString();

		readElementProperties(input, vertexArgs);
		final Vertex v = vertexMaker.apply(vertexId, label, vertexArgs.toArray());

		final boolean streamContainsEdgesInSomeDirection = input.readBoolean();
		if (!streamContainsEdgesInSomeDirection && directionRequested != null)
			throw new IllegalStateException(String.format("The direction %s was requested but no attempt was made to serialize edges into this stream", directionRequested));

		// if there are edges in the stream and the direction is not present then the rest of the stream is
		// simply ignored
		if (directionRequested != null) {
			final Direction directionsInStream = kryo.readObject(input, Direction.class);
			if (directionsInStream != Direction.BOTH && directionsInStream != directionRequested)
				throw new IllegalStateException(String.format("Stream contains %s edges, but requesting %s", directionsInStream, directionRequested));

			final Direction firstDirection = kryo.readObject(input, Direction.class);
			if (firstDirection == Direction.OUT && (directionRequested == Direction.BOTH || directionRequested == Direction.OUT))
				readEdges(input, (eId, vId, l, properties) -> edgeMaker.apply(eId, v.id(), vId, l, properties));
			else {
				// requested direction in, but BOTH must be serialized so skip this.  the illegalstateexception
				// prior to this IF should  have caught a problem where IN is not supported at all
				if (firstDirection == Direction.OUT && directionRequested == Direction.IN)
					skipEdges(input);
			}

			if (directionRequested == Direction.BOTH || directionRequested == Direction.IN) {
				// if the first direction was OUT then it was either read or skipped.  in that case, the marker
				// of the stream is currently ready to read the IN direction. otherwise it's in the perfect place
				// to start reading edges
				if (firstDirection == Direction.OUT)
					kryo.readObject(input, Direction.class);

				readEdges(input, (eId, vId, l, properties) -> edgeMaker.apply(eId, vId, v.id(), l, properties));
			}
		}

		return v;
	}

    private void readEdges(final Input input, final QuadConsumer<Object, Object, String, Object[]> edgeMaker) {
        if (input.readBoolean()) {
            Object inOrOutVId = kryo.readClassAndObject(input);
            while (!inOrOutVId.equals(EdgeTerminator.INSTANCE)) {
                final List<Object> edgeArgs = new ArrayList<>();
                final Object edgeId = kryo.readClassAndObject(input);
                final String edgeLabel = input.readString();
                readElementProperties(input, edgeArgs);

                edgeMaker.accept(edgeId, inOrOutVId, edgeLabel, edgeArgs.toArray());

                inOrOutVId = kryo.readClassAndObject(input);
            }
        }
    }

    private void skipEdges(final Input input) {
        if (input.readBoolean()) {
            Object inOrOutId = kryo.readClassAndObject(input);
            while (!inOrOutId.equals(EdgeTerminator.INSTANCE)) {
                // skip edgeid
                kryo.readClassAndObject(input);

                // skip label
                input.readString();

                // read property count so we know how many properties to skip
                final int numberOfProperties = input.readInt();
                IntStream.range(0, numberOfProperties).forEach(i -> {
                    input.readString();
                    kryo.readClassAndObject(input);
                });

				// read hidden count so we know how many properties to skip
				final int numberOfHiddens = input.readInt();
				IntStream.range(0, numberOfHiddens).forEach(i -> {
					input.readString();
					kryo.readClassAndObject(input);
				});

                // next in/out id to skip
                inOrOutId = kryo.readClassAndObject(input);
            }
        }
    }

    /**
     * Reads through the all the edges for a vertex and writes the edges to a temp file which will be read later.
     */
    private void readToEndOfEdgesAndWriteToTemp(final Input input, final Output output) throws IOException {
        Object inId = kryo.readClassAndObject(input);
        while (!inId.equals(EdgeTerminator.INSTANCE)) {
            kryo.writeClassAndObject(output, inId);

            // edge id
            kryo.writeClassAndObject(output, kryo.readClassAndObject(input));

            // label
            output.writeString(input.readString());

			// standard properties
            final int props = input.readInt();
            output.writeInt(props);
            IntStream.range(0, props).forEach(i -> {
                // key
                output.writeString(input.readString());

                // value
                kryo.writeClassAndObject(output, kryo.readClassAndObject(input));
            });

			// hidden properties
			final int hiddens = input.readInt();
			output.writeInt(hiddens);
			IntStream.range(0, hiddens).forEach(i -> {
				// key
				output.writeString(input.readString());

				// value
				kryo.writeClassAndObject(output, kryo.readClassAndObject(input));
			});

            // next inId or terminator
            inId = kryo.readClassAndObject(input);
        }

        // this should be the vertex terminator
        kryo.readClassAndObject(input);

        kryo.writeClassAndObject(output, EdgeTerminator.INSTANCE);
        kryo.writeClassAndObject(output, VertexTerminator.INSTANCE);
    }

    /**
     * Read the edges from the temp file and load them to the graph.
     */
    private void readFromTempEdges(final Input input, final Graph graphToWriteTo) {
        while (!input.eof()) {
            // in this case the outId is the id assigned by the graph
            final Object outId = kryo.readClassAndObject(input);
            Object inId = kryo.readClassAndObject(input);
            while (!inId.equals(EdgeTerminator.INSTANCE)) {
                final List<Object> edgeArgs = new ArrayList<>();
                final Vertex vOut = graphToWriteTo.v(outId);

                final Object edgeId = kryo.readClassAndObject(input);
                edgeArgs.addAll(Arrays.asList(Element.ID, edgeId));

                final String edgeLabel = input.readString();
                final Vertex inV = graphToWriteTo.v(inId);
                readElementProperties(input, edgeArgs);

                vOut.addEdge(edgeLabel, inV, edgeArgs.toArray());

                inId = kryo.readClassAndObject(input);
            }

            // vertex terminator
            kryo.readClassAndObject(input);
        }
    }


    private void readElementProperties(final Input input, final List<Object> elementArgs) {
        final int numberOfProperties = input.readInt();
        IntStream.range(0, numberOfProperties).forEach(i -> {
            final String key = input.readString();
            elementArgs.add(key);
			elementArgs.add(kryo.readClassAndObject(input));
        });

		final int numberOfHiddens = input.readInt();
		IntStream.range(0, numberOfHiddens).forEach(i -> {
			final String key = input.readString();
			elementArgs.add(Property.hidden(key));
			elementArgs.add(kryo.readClassAndObject(input));
		});
    }

    private void deleteTempFileSilently() {
        try {
            tempFile.delete();
        } catch (Exception ex) {
        }
    }

    public static Builder create() {
        return new Builder();
    }

    public static class Builder {
        private File tempFile;
        private long batchSize = BatchGraph.DEFAULT_BUFFER_SIZE;
        private String vertexIdKey = Element.ID;
        private String edgeIdKey = Element.ID;

        /**
         * Always use the most recent kryo version by default
         */
        private GremlinKryo gremlinKryo = GremlinKryo.create().build();

        private Builder() {
            this.tempFile = new File(UUID.randomUUID() + ".tmp");
        }

        public Builder batchSize(final long batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        public Builder custom(final GremlinKryo gremlinKryo) {
            this.gremlinKryo = gremlinKryo;
            return this;
        }

        public Builder vertexIdKey(final String vertexIdKey) {
            this.vertexIdKey = vertexIdKey;
            return this;
        }

        public Builder edgeIdKey(final String edgeIdKey) {
            this.edgeIdKey = edgeIdKey;
            return this;
        }

        /**
         * The reader requires a working directory to write temp files to.  If this value is not set, it will write
         * the temp file to the local directory.
         */
        public Builder setWorkingDirectory(final String workingDirectory) {
            final File f = new File(workingDirectory);
            if (!f.exists() || !f.isDirectory())
                throw new IllegalArgumentException("The workingDirectory is not a directory or does not exist");

            tempFile = new File(workingDirectory + File.separator + UUID.randomUUID() + ".tmp");
            return this;
        }

        public KryoReader build() {
            return new KryoReader(tempFile, batchSize, this.vertexIdKey, this.edgeIdKey, this.gremlinKryo);
        }
    }
}
