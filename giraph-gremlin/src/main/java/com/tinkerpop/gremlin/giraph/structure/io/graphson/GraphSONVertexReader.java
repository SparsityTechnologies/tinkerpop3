package com.tinkerpop.gremlin.giraph.structure.io.graphson;

import org.apache.giraph.graph.Vertex;
import org.apache.giraph.io.VertexReader;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class GraphSONVertexReader extends VertexReader {
    private final GraphSONRecordReader recordReader;

    public GraphSONVertexReader() {
        recordReader = new GraphSONRecordReader();
    }

    public void initialize(final InputSplit inputSplit,
                           final TaskAttemptContext context) throws IOException, InterruptedException {
        recordReader.initialize(inputSplit, context);
    }

    public boolean nextVertex() throws IOException, InterruptedException {
        return recordReader.nextKeyValue();
    }

    public Vertex getCurrentVertex() throws IOException, InterruptedException {
        return recordReader.getCurrentValue();
    }

    public void close() throws IOException {
        recordReader.close();
    }

    public float getProgress() throws IOException, InterruptedException {
        return recordReader.getProgress();
    }
}
