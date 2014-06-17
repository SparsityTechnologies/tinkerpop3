package com.tinkerpop.gremlin.structure;

import com.tinkerpop.gremlin.AbstractGremlinTest;
import com.tinkerpop.gremlin.structure.util.StringFactory;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

/**
 * Blueprints Test Suite for {@link com.tinkerpop.gremlin.structure.Graph.Variables} operations.
 *
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
@RunWith(Enclosed.class)
public class MemoryTest {

    /**
     * Basic tests to ensure that {@link com.tinkerpop.gremlin.structure.Graph.Variables} have
     * appropriate {@link String} representations.
     */
    public static class StringRepresentationTest extends AbstractGremlinTest {
        @Test
        @FeatureRequirement(featureClass = Graph.Features.VariableFeatures.class, feature = Graph.Features.VariableFeatures.FEATURE_STRING_VALUES)
        public void testMemory() {
            final Graph.Variables variables = g.variables();
            variables.set("xo", "test1");
            variables.set("yo", "test2");
            variables.set("zo", "test3");

            tryCommit(g, graph -> assertEquals(StringFactory.graphVariablesString(variables), variables.toString()));
        }
    }

    /**
     * Ensure that the {@link com.tinkerpop.gremlin.structure.Graph.Variables#asMap()} method returns some basics.
     * Other tests will enforce that all types are properly covered in {@link com.tinkerpop.gremlin.structure.Graph.Variables}.
     */
    public static class MemoryAsMapTest extends AbstractGremlinTest {
        @Test
        @FeatureRequirement(featureClass = Graph.Features.VariableFeatures.class, feature = Graph.Features.VariableFeatures.FEATURE_STRING_VALUES)
        public void testNone() {
            final Graph.Variables variables = g.variables();
            final Map<String,Object> mapOfAnnotations = variables.asMap();
            assertNotNull(mapOfAnnotations);
            assertEquals(0, mapOfAnnotations.size());
            try {
                mapOfAnnotations.put("something", "can't do this");
                fail("Should not be able to mutate the Map returned from Graph.variables.getAnnotations()");
            } catch (UnsupportedOperationException uoe) {

            }
        }

        @Test
        @FeatureRequirement(featureClass = Graph.Features.VariableFeatures.class, feature = Graph.Features.VariableFeatures.FEATURE_STRING_VALUES)
        public void testMemoryString() {
            final Graph.Variables variables = g.variables();
            variables.set("test1", "1");
            variables.set("test2", "2");
            variables.set("test3", "3");

            tryCommit(g, graph -> {
                final Map<String, Object> m = variables.asMap();
                assertEquals("1", m.get("test1"));
                assertEquals("2", m.get("test2"));
                assertEquals("3", m.get("test3"));
            });
        }

        @Test
        @FeatureRequirement(featureClass = Graph.Features.VariableFeatures.class, feature = Graph.Features.VariableFeatures.FEATURE_INTEGER_VALUES)
        public void testMemoryInteger() {
            final Graph.Variables variables = g.variables();
            variables.set("test1", 1);
            variables.set("test2", 2);
            variables.set("test3", 3);

            tryCommit(g, graph -> {
                final Map<String, Object> m = variables.asMap();
                assertEquals(1, m.get("test1"));
                assertEquals(2, m.get("test2"));
                assertEquals(3, m.get("test3"));
            });
        }

        @Test
        @FeatureRequirement(featureClass = Graph.Features.VariableFeatures.class, feature = Graph.Features.VariableFeatures.FEATURE_LONG_VALUES)
        public void testMemoryLong() {
            final Graph.Variables variables = g.variables();
            variables.set("test1", 1l);
            variables.set("test2", 2l);
            variables.set("test3", 3l);

            tryCommit(g, graph -> {
                final Map<String, Object> m = variables.asMap();
                assertEquals(1l, m.get("test1"));
                assertEquals(2l, m.get("test2"));
                assertEquals(3l, m.get("test3"));
            });
        }

        @Test
        @FeatureRequirement(featureClass = Graph.Features.VariableFeatures.class, feature = Graph.Features.VariableFeatures.FEATURE_STRING_VALUES)
        @FeatureRequirement(featureClass = Graph.Features.VariableFeatures.class, feature = Graph.Features.VariableFeatures.FEATURE_INTEGER_VALUES)
        @FeatureRequirement(featureClass = Graph.Features.VariableFeatures.class, feature = Graph.Features.VariableFeatures.FEATURE_LONG_VALUES)
        public void testMemoryMixed() {
            final Graph.Variables variables = g.variables();
            variables.set("test1", "1");
            variables.set("test2", 2);
            variables.set("test3", 3l);

            tryCommit(g, graph -> {
                final Map<String, Object> m = variables.asMap();
                assertEquals("1", m.get("test1"));
                assertEquals(2, m.get("test2"));
                assertEquals(3l, m.get("test3"));
            });
        }
    }

    /**
     * Tests for feature support on {@link com.tinkerpop.gremlin.structure.Graph.Variables}.  The tests validate if
     * {@link com.tinkerpop.gremlin.structure.Graph.Features.AnnotationFeatures} should be turned on or off and if the
     * enabled features are properly supported by the implementation.  Note that these tests are run in a separate
     * test class as they are "parameterized" tests.
     */
    @RunWith(Parameterized.class)
    public static class MemoryFeatureSupportTest extends AbstractGremlinTest {
        private static final Map testMap = new HashMap() {{
            put("testString", "try");
            put("testInteger", 123);
        }};

        private static final ArrayList mixedList = new ArrayList() {{
            add("try1");
            add(2);
        }};

        private static final ArrayList uniformStringList = new ArrayList() {{
            add("try1");
            add("try2");
        }};

        private static final ArrayList uniformIntegerList = new ArrayList() {{
            add(100);
            add(200);
            add(300);
        }};

        @Parameterized.Parameters(name = "{index}: supports{0}({1})")
        public static Iterable<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {Graph.Features.VariableFeatures.FEATURE_BOOLEAN_VALUES, true},
                    {Graph.Features.VariableFeatures.FEATURE_BOOLEAN_VALUES, false},
                    {Graph.Features.VariableFeatures.FEATURE_DOUBLE_VALUES, Double.MIN_VALUE},
                    {Graph.Features.VariableFeatures.FEATURE_DOUBLE_VALUES, Double.MAX_VALUE},
                    {Graph.Features.VariableFeatures.FEATURE_DOUBLE_VALUES, 0.0d},
                    {Graph.Features.VariableFeatures.FEATURE_DOUBLE_VALUES, 0.5d},
                    {Graph.Features.VariableFeatures.FEATURE_DOUBLE_VALUES, -0.5d},
                    {Graph.Features.VariableFeatures.FEATURE_FLOAT_VALUES, Float.MIN_VALUE},
                    {Graph.Features.VariableFeatures.FEATURE_FLOAT_VALUES, Float.MAX_VALUE},
                    {Graph.Features.VariableFeatures.FEATURE_FLOAT_VALUES, 0.0f},
                    {Graph.Features.VariableFeatures.FEATURE_FLOAT_VALUES, 0.5f},
                    {Graph.Features.VariableFeatures.FEATURE_FLOAT_VALUES, -0.5f},
                    {Graph.Features.VariableFeatures.FEATURE_INTEGER_VALUES, Integer.MIN_VALUE},
                    {Graph.Features.VariableFeatures.FEATURE_INTEGER_VALUES, Integer.MAX_VALUE},
                    {Graph.Features.VariableFeatures.FEATURE_INTEGER_VALUES, 0},
                    {Graph.Features.VariableFeatures.FEATURE_INTEGER_VALUES, 10000},
                    {Graph.Features.VariableFeatures.FEATURE_INTEGER_VALUES, -10000},
                    {Graph.Features.VariableFeatures.FEATURE_LONG_VALUES, Long.MIN_VALUE},
                    {Graph.Features.VariableFeatures.FEATURE_LONG_VALUES, Long.MAX_VALUE},
                    {Graph.Features.VariableFeatures.FEATURE_LONG_VALUES, 0l},
                    {Graph.Features.VariableFeatures.FEATURE_LONG_VALUES, 10000l},
                    {Graph.Features.VariableFeatures.FEATURE_LONG_VALUES, -10000l},
                    {Graph.Features.VariableFeatures.FEATURE_MAP_VALUES, testMap},
                    {Graph.Features.VariableFeatures.FEATURE_MIXED_LIST_VALUES, mixedList},
                    {Graph.Features.VariableFeatures.FEATURE_PRIMITIVE_ARRAY_VALUES, new boolean[]{true, false}},
                    {Graph.Features.VariableFeatures.FEATURE_PRIMITIVE_ARRAY_VALUES, new double[]{1d, 2d}},
                    {Graph.Features.VariableFeatures.FEATURE_PRIMITIVE_ARRAY_VALUES, new float[]{1f, 2f}},
                    {Graph.Features.VariableFeatures.FEATURE_PRIMITIVE_ARRAY_VALUES, new int[]{1, 2}},
                    {Graph.Features.VariableFeatures.FEATURE_PRIMITIVE_ARRAY_VALUES, new long[]{1l, 2l}},
                    {Graph.Features.VariableFeatures.FEATURE_PRIMITIVE_ARRAY_VALUES, new String[]{"try1", "try2"}},
                    {Graph.Features.VariableFeatures.FEATURE_PRIMITIVE_ARRAY_VALUES, new int[1]},
                    {Graph.Features.VariableFeatures.FEATURE_SERIALIZABLE_VALUES, new MockSerializable("testing")},
                    {Graph.Features.VariableFeatures.FEATURE_STRING_VALUES, "short string"},
                    {Graph.Features.VariableFeatures.FEATURE_UNIFORM_LIST_VALUES, uniformIntegerList},
                    {Graph.Features.VariableFeatures.FEATURE_UNIFORM_LIST_VALUES, uniformStringList}
            });
        }

        @Parameterized.Parameter(value = 0)
        public String featureName;

        @Parameterized.Parameter(value = 1)
        public Object value;

        @Test
        public void shouldSetValueOnGraph() throws Exception {
            assumeThat(g.getFeatures().supports(Graph.Features.VariableFeatures.class, featureName), is(true));
            final Graph.Variables variables = g.variables();
            variables.set("key", value);

            if (value instanceof Map)
                tryCommit(g, graph -> {
                    final Map map = variables.<Map>get("key");
                    assertEquals(((Map) value).size(), map.size());
                    ((Map) value).keySet().forEach(k -> assertEquals(((Map) value).get(k), map.get(k)));
                });
            else if (value instanceof List)
                tryCommit(g, graph -> {
                    final List l = variables.<List>get("key");
                    assertEquals(((List) value).size(), l.size());
                    for (int ix = 0; ix < ((List) value).size(); ix++) {
                        assertEquals(((List) value).get(ix), l.get(ix));
                    }
                });
            else if (value instanceof MockSerializable)
                tryCommit(g, graph -> {
                    final MockSerializable mock = variables.<MockSerializable>get("key");
                    assertEquals(((MockSerializable) value).getTestField(), mock.getTestField());
                });
            else if (value instanceof boolean[])
                tryCommit(g, graph -> {
                    final boolean[] l = variables.<boolean[]>get("key");
                    assertEquals(((boolean[]) value).length, l.length);
                    for (int ix = 0; ix < ((boolean[]) value).length; ix++) {
                        assertEquals(((boolean[]) value)[ix], l[ix]);
                    }
                });
            else if (value instanceof double[])
                tryCommit(g, graph -> {
                    final double[] l = variables.<double[]>get("key");
                    assertEquals(((double[]) value).length, l.length);
                    for (int ix = 0; ix < ((double[]) value).length; ix++) {
                        assertEquals(((double[]) value)[ix], l[ix], 0.0d);
                    }
                });
            else if (value instanceof float[])
                tryCommit(g, graph -> {
                    final float[] l = variables.<float[]>get("key");
                    assertEquals(((float[]) value).length, l.length);
                    for (int ix = 0; ix < ((float[]) value).length; ix++) {
                        assertEquals(((float[]) value)[ix], l[ix], 0.0f);
                    }
                });
            else if (value instanceof int[])
                tryCommit(g, graph -> {
                    final int[] l = variables.<int[]>get("key");
                    assertEquals(((int[]) value).length, l.length);
                    for (int ix = 0; ix < ((int[]) value).length; ix++) {
                        assertEquals(((int[]) value)[ix], l[ix]);
                    }
                });
            else if (value instanceof long[])
                tryCommit(g, graph -> {
                    final long[] l = variables.<long[]>get("key");
                    assertEquals(((long[]) value).length, l.length);
                    for (int ix = 0; ix < ((long[]) value).length; ix++) {
                        assertEquals(((long[]) value)[ix], l[ix]);
                    }
                });
            else if (value instanceof String[])
                tryCommit(g, graph -> {
                    final String[] l = variables.<String[]>get("key");
                    assertEquals(((String[]) value).length, l.length);
                    for (int ix = 0; ix < ((String[]) value).length; ix++) {
                        assertEquals(((String[]) value)[ix], l[ix]);
                    }
                });
            else
                tryCommit(g, graph -> assertEquals(value, variables.get("key")));
        }
    }

    private static class MockSerializable implements Serializable {
        private String testField;

        public MockSerializable(final String testField) {
            this.testField = testField;
        }

        public String getTestField() {
            return this.testField;
        }

        public void setTestField(final String testField) {
            this.testField = testField;
        }

        @Override
        public boolean equals(Object oth) {
            if (this == oth) return true;
            else if (oth == null) return false;
            else if (!getClass().isInstance(oth)) return false;
            MockSerializable m = (MockSerializable) oth;
            if (testField == null) {
                return (m.testField == null);
            } else return testField.equals(m.testField);
        }
    }
}
