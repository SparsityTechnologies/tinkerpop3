package com.tinkerpop.gremlin.structure.io.graphson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdKeySerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Element;
import com.tinkerpop.gremlin.structure.Vertex;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class GraphSONModule extends SimpleModule {

    public GraphSONModule(final boolean normalize) {
        super("graphson");
        addSerializer(Edge.class, new EdgeJacksonSerializer());
        addSerializer(Vertex.class, new VertexJacksonSerializer());
        addSerializer(GraphSONVertex.class, new GraphSONVertex.VertexJacksonSerializer());
        addSerializer(GraphSONGraph.class, new GraphSONGraph.GraphJacksonSerializer(normalize));
    }

    static class EdgeJacksonSerializer extends StdSerializer<Edge> {
        public EdgeJacksonSerializer() {
            super(Edge.class);
        }

        @Override
        public void serialize(final Edge edge, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider)
                throws IOException {
            ser(edge, jsonGenerator);
        }

        @Override
        public void serializeWithType(final Edge edge, final JsonGenerator jsonGenerator,
                                      final SerializerProvider serializerProvider, final TypeSerializer typeSerializer) throws IOException {
            ser(edge, jsonGenerator);
        }

        private void ser(final Edge edge, final JsonGenerator jsonGenerator) throws IOException {
            final Map<String,Object> m = new HashMap<>();
            m.put(GraphSONTokens.ID, edge.id());
            m.put(GraphSONTokens.LABEL, edge.label());
            m.put(GraphSONTokens.TYPE, GraphSONTokens.EDGE);

            final Vertex inV = edge.inV().next();
            m.put(GraphSONTokens.IN, inV.id());
            m.put(GraphSONTokens.IN_LABEL, inV.label());

            final Vertex outV = edge.outV().next();
            m.put(GraphSONTokens.OUT, outV.id());
            m.put(GraphSONTokens.OUT_LABEL, outV.label());
            m.put(GraphSONTokens.PROPERTIES, edge.values());
			m.put(GraphSONTokens.HIDDENS, edge.hiddenValues());

            jsonGenerator.writeObject(m);
        }
    }

    static class VertexJacksonSerializer extends StdSerializer<Vertex> {

        public VertexJacksonSerializer() {
            super(Vertex.class);
        }

        @Override
        public void serialize(final Vertex vertex, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider)
                throws IOException {
            ser(vertex, jsonGenerator);
        }

        @Override
        public void serializeWithType(final Vertex vertex, final JsonGenerator jsonGenerator,
                                      final SerializerProvider serializerProvider, final TypeSerializer typeSerializer) throws IOException {
            ser(vertex, jsonGenerator);

        }

        private void ser(final Vertex vertex, final JsonGenerator jsonGenerator)
                throws IOException {
            final Map<String,Object> m = new HashMap<>();
            m.put(GraphSONTokens.ID, vertex.id());
            m.put(GraphSONTokens.LABEL, vertex.label());
            m.put(GraphSONTokens.TYPE, GraphSONTokens.VERTEX);
            m.put(GraphSONTokens.PROPERTIES,  vertex.values());
			m.put(GraphSONTokens.HIDDENS, vertex.hiddenValues());

            jsonGenerator.writeObject(m);
        }

    }

    /**
     * Maps in the JVM can have {@link Object} as a key, but in JSON they must be a {@link String}.
     */
    static class GraphSONKeySerializer extends StdKeySerializer {
        @Override
        public void serialize(final Object o, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider) throws IOException {
            ser(o, jsonGenerator, serializerProvider);
        }

        @Override
        public void serializeWithType(final Object o, final JsonGenerator jsonGenerator,
                                      final SerializerProvider serializerProvider, final TypeSerializer typeSerializer) throws IOException {
            ser(o, jsonGenerator, serializerProvider);
        }

        private void ser(final Object o, final JsonGenerator jsonGenerator,
                         final SerializerProvider serializerProvider) throws IOException {
            if (Element.class.isAssignableFrom(o.getClass()))
                jsonGenerator.writeFieldName((((Element) o).id()).toString());
            else
                super.serialize(o, jsonGenerator, serializerProvider);
        }
    }

    /*
    static class EdgeJacksonDeserializer extends StdDeserializer<CachedEdge> {

        public EdgeJacksonDeserializer() {
            super(CachedEdge.class);
        }

        @Override
        public CachedEdge deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            final ObjectCodec oc = jsonParser.getCodec();

            Object id = null;
            String label = null;
            Map properties = null;
            String inLabel = null;
            String outLabel = null;
            Object in = null;
            Object out = null;
            while (!jsonParser.nextToken().equals(JsonToken.END_OBJECT)) {
                final String name = jsonParser.getCurrentName();
                if (name != null && name.equals(GraphSONTokens.ID))
                    id = oc.readValue(jsonParser, Object.class);
                else if (name != null && name.equals(GraphSONTokens.LABEL))
                    label = jsonParser.getValueAsString();
                else if (name != null && name.equals(GraphSONTokens.PROPERTIES)) {
                    jsonParser.nextToken();
                    properties = (Map) oc.readValue(jsonParser, Object.class);
                } else if (name != null && name.equals(GraphSONTokens.IN)) {
                    jsonParser.nextToken();
                    in = oc.readValue(jsonParser, Object.class);
                } else if (name != null && name.equals(GraphSONTokens.OUT)) {
                    jsonParser.nextToken();
                    out = oc.readValue(jsonParser, Object.class);
                } else if (name != null && name.equals(GraphSONTokens.IN_LABEL))
                    inLabel = jsonParser.getValueAsString();
                else if (name != null && name.equals(GraphSONTokens.OUT_LABEL))
                    outLabel = jsonParser.getValueAsString();
            }

            return new CachedEdge(id, label, properties,
                    Pair.with(out, outLabel), Pair.with(in, inLabel));

        }
    }

    static class VertexJacksonDeserializer extends StdDeserializer<CachedVertex> {

        public VertexJacksonDeserializer() {
            super(CachedVertex.class);
        }

        @Override
        public CachedVertex deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            final ObjectCodec oc = jsonParser.getCodec();

            Object id = null;
            String label = null;
            Map properties = null;
            while (!jsonParser.nextToken().equals(JsonToken.END_OBJECT)) {
                final String name = jsonParser.getCurrentName();
                if (name != null && name.equals(GraphSONTokens.ID))
                    id = oc.readValue(jsonParser, Object.class);
                else if (name != null && name.equals(GraphSONTokens.LABEL))
                    label = jsonParser.getValueAsString();
                else if (name != null && name.equals(GraphSONTokens.PROPERTIES)) {
                    jsonParser.nextToken();
                    properties = (Map) oc.readValue(jsonParser, Object.class);
                }
            }

            return new CachedVertex(id, label, properties);
        }
    }
     */
}
