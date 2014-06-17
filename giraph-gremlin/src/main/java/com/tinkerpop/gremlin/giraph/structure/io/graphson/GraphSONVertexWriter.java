package com.tinkerpop.gremlin.giraph.structure.io.graphson;

import com.tinkerpop.gremlin.giraph.structure.GiraphVertex;
import org.apache.giraph.graph.Vertex;
import org.apache.giraph.io.VertexWriter;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.RecordWriter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public class GraphSONVertexWriter extends VertexWriter {

    private final GraphSONOutputFormat outputFormat;
    private RecordWriter<NullWritable, GiraphVertex> recordWriter;

    public GraphSONVertexWriter() {
        outputFormat = new GraphSONOutputFormat();
    }

    public void initialize(TaskAttemptContext context) throws IOException, InterruptedException {
        recordWriter = outputFormat.getRecordWriter(context);
    }

    public void close(TaskAttemptContext context) throws IOException, InterruptedException {
        recordWriter.close(context);
    }

    public void writeVertex(Vertex vertex) throws IOException, InterruptedException {
        recordWriter.write(NullWritable.get(), (GiraphVertex) vertex);
    }
}
