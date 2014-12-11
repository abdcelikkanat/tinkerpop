package com.tinkerpop.gremlin.structure.strategy;

import com.tinkerpop.gremlin.AbstractGremlinTest;
import com.tinkerpop.gremlin.FeatureRequirement;
import com.tinkerpop.gremlin.FeatureRequirementSet;
import com.tinkerpop.gremlin.process.graph.GraphTraversal;
import com.tinkerpop.gremlin.process.graph.util.DefaultGraphTraversal;
import com.tinkerpop.gremlin.structure.Direction;
import com.tinkerpop.gremlin.structure.Edge;
import com.tinkerpop.gremlin.structure.Graph;
import com.tinkerpop.gremlin.structure.Property;
import com.tinkerpop.gremlin.structure.Vertex;
import com.tinkerpop.gremlin.structure.VertexProperty;
import com.tinkerpop.gremlin.util.function.TriFunction;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class SequenceStrategyTest extends AbstractGremlinTest {

    @Test
    @FeatureRequirementSet(FeatureRequirementSet.Package.VERTICES_ONLY)
    @FeatureRequirement(featureClass = Graph.Features.VertexFeatures.class, feature = Graph.Features.VertexFeatures.FEATURE_MULTI_PROPERTIES)
    public void shouldAppendMultiPropertyValuesToVertex() {
        final StrategyGraph swg = new StrategyGraph(g);
        swg.getStrategy().setGraphStrategy(new SequenceStrategy(
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.addAll(Arrays.asList("anonymous", "working1"));
                            return f.apply(o.toArray());
                        };
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.addAll(Arrays.asList("anonymous", "working2"));
                            o.addAll(Arrays.asList("try", "anything"));
                            return f.apply(o.toArray());
                        };
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return UnaryOperator.identity();
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.addAll(Arrays.asList("anonymous", "working3"));
                            return f.apply(o.toArray());
                        };
                    }
                }
        ));

        final Vertex v = swg.addVertex("any", "thing");

        assertNotNull(v);
        assertEquals("thing", v.property("any").value());
        assertEquals(3, v.values("anonymous").toList().size());
        assertTrue(v.values("anonymous").toList().contains("working1"));
        assertTrue(v.values("anonymous").toList().contains("working2"));
        assertTrue(v.values("anonymous").toList().contains("working3"));
        assertEquals("anything", v.property("try").value());
    }

    @Test
    @FeatureRequirementSet(FeatureRequirementSet.Package.VERTICES_ONLY)
    public void shouldOverwritePropertyValuesToVertex() {
        final StrategyGraph swg = new StrategyGraph(g);
        swg.getStrategy().setGraphStrategy(new SequenceStrategy(
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.addAll(Arrays.asList("anonymous", "working1"));
                            return f.apply(o.toArray());
                        };
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.remove("anonymous");
                            o.remove("working1");
                            o.addAll(Arrays.asList("anonymous", "working2"));
                            o.addAll(Arrays.asList("try", "anything"));
                            return f.apply(o.toArray());
                        };
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return UnaryOperator.identity();
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.remove("anonymous");
                            o.remove("working2");
                            o.addAll(Arrays.asList("anonymous", "working3"));
                            return f.apply(o.toArray());
                        };
                    }
                }
        ));

        final Vertex v = swg.addVertex("any", "thing");

        assertNotNull(v);
        assertEquals("thing", v.property("any").value());
        assertEquals(1, v.values("anonymous").toList().size());
        assertTrue(v.values("anonymous").toList().contains("working3"));
        assertEquals("anything", v.property("try").value());
    }

    @Test
    @FeatureRequirementSet(FeatureRequirementSet.Package.VERTICES_ONLY)
    public void shouldAllowForALocalGraphStrategyCallInSequence() {
        final StrategyGraph swg = new StrategyGraph(g);
        swg.getStrategy().setGraphStrategy(new SequenceStrategy(
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.addAll(Arrays.asList("anonymous", "working"));
                            return f.apply(o.toArray());
                        };
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context<StrategyGraph> ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.addAll(Arrays.asList("ts", "timestamped"));
                            return f.apply(o.toArray());
                        };
                    }

                    @Override
                    public <V> UnaryOperator<BiFunction<String, V, VertexProperty<V>>> getVertexPropertyStrategy(final Strategy.Context<StrategyVertex> ctx) {
                        return (f) -> (k, v) -> {
                            ctx.getCurrent().getBaseVertex().property("timestamp", "timestamped");

                            // dynamically construct a strategy to force this call to addVertex to stay localized
                            // to this GraphStrategy (i.e. the first GraphStrategy in the sequence is ignored)
                            // note that this will not call GraphStrategy implementations after this one in the
                            // sequence either
                            this.getAddVertexStrategy(ctx.getStrategyGraph().getGraphContext())
                                    .apply(ctx.getBaseGraph()::addVertex)
                                    .apply(Arrays.asList("strategy", "bypassed").toArray());

                            return f.apply(k, v);
                        };
                    }
                }
        ));

        final Vertex v = swg.addVertex("any", "thing");
        v.property("set", "prop");

        assertNotNull(v);
        assertEquals("thing", v.property("any").value());
        assertEquals(1, v.values("anonymous").toList().size());
        assertTrue(v.values("ts").toList().contains("timestamped"));
        assertTrue(v.values("set").toList().contains("prop"));

        final Vertex vStrat = (Vertex) g.V().has("strategy", "bypassed").next();
        assertEquals(0, vStrat.values("anonymous").toList().size());
        assertTrue(v.values("ts").toList().contains("timestamped"));
        assertTrue(v.values("timestamp").toList().contains("timestamped"));
    }

    @Test
    @FeatureRequirementSet(FeatureRequirementSet.Package.VERTICES_ONLY)
    public void shouldAllowForANeighborhoodGraphStrategyCallInSequence() {
        final StrategyGraph swg = new StrategyGraph(g);
        final SequenceStrategy innerSequenceStrategy = new SequenceStrategy();

        innerSequenceStrategy.add(new GraphStrategy() {
            @Override
            public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context<StrategyGraph> ctx) {
                return (f) -> (args) -> {
                    final List<Object> o = new ArrayList<>(Arrays.asList(args));
                    o.addAll(Arrays.asList("ts1", "timestamped"));
                    return f.apply(o.toArray());
                };
            }

            @Override
            public <V> UnaryOperator<BiFunction<String, V, VertexProperty<V>>> getVertexPropertyStrategy(final Strategy.Context<StrategyVertex> ctx) {
                return (f) -> (k, v) -> {
                    ctx.getCurrent().getBaseVertex().property("timestamp", "timestamped");

                    // dynamically construct a strategy to force this call to addVertex to stay localized
                    // to the innerSequenceGraphStrategy. note that this will only call GraphStrategy implementations
                    // in this sequence
                    innerSequenceStrategy.getAddVertexStrategy(ctx.getStrategyGraph().getGraphContext())
                            .apply(ctx.getBaseGraph()::addVertex)
                            .apply(Arrays.asList("strategy", "bypassed").toArray());

                    return f.apply(k, v);
                };
            }
        });

        innerSequenceStrategy.add(new GraphStrategy() {
            @Override
            public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context<StrategyGraph> ctx) {
                return (f) -> (args) -> {
                    final List<Object> o = new ArrayList<>(Arrays.asList(args));
                    o.addAll(Arrays.asList("ts2", "timestamped"));
                    return f.apply(o.toArray());
                };
            }
        });

        swg.getStrategy().setGraphStrategy(new SequenceStrategy(
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.addAll(Arrays.asList("anonymous", "working"));
                            return f.apply(o.toArray());
                        };
                    }
                },
                innerSequenceStrategy
        ));

        final Vertex v = swg.addVertex("any", "thing");
        v.property("set", "prop");

        assertNotNull(v);
        assertEquals("thing", v.property("any").value());
        assertEquals(1, v.values("anonymous").toList().size());
        assertTrue(v.values("ts1").toList().contains("timestamped"));
        assertTrue(v.values("ts2").toList().contains("timestamped"));
        assertTrue(v.values("set").toList().contains("prop"));

        final Vertex vStrat = (Vertex) g.V().has("strategy", "bypassed").next();
        assertEquals(0, vStrat.values("anonymous").toList().size());
        assertTrue(v.values("ts1").toList().contains("timestamped"));
        assertTrue(v.values("ts2").toList().contains("timestamped"));
        assertTrue(v.values("timestamp").toList().contains("timestamped"));
    }

    @Test
    @FeatureRequirementSet(FeatureRequirementSet.Package.VERTICES_ONLY)
    public void shouldAlterArgumentsToAddVertexInOrderOfSequence() {
        final StrategyGraph swg = new StrategyGraph(g);
        swg.getStrategy().setGraphStrategy(new SequenceStrategy(
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.addAll(Arrays.asList("anonymous", "working1"));
                            return f.apply(o.toArray());
                        };
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.replaceAll(it -> it.equals("working1") ? "working2" : it);
                            o.addAll(Arrays.asList("try", "anything"));
                            return f.apply(o.toArray());
                        };
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return UnaryOperator.identity();
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.replaceAll(it -> it.equals("working2") ? "working3" : it);
                            return f.apply(o.toArray());
                        };
                    }
                }
        ));

        final Vertex v = swg.addVertex("any", "thing");

        assertNotNull(v);
        assertEquals("thing", v.property("any").value());
        assertEquals("working3", v.property("anonymous").value());
        assertEquals("anything", v.property("try").value());
    }

    @Test(expected = RuntimeException.class)
    @FeatureRequirementSet(FeatureRequirementSet.Package.VERTICES_ONLY)
    public void shouldShortCircuitStrategyWithException() {
        final StrategyGraph swg = new StrategyGraph(g);
        swg.getStrategy().setGraphStrategy(new SequenceStrategy(
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.addAll(Arrays.asList("anonymous", "working1"));
                            return f.apply(o.toArray());
                        };
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            throw new RuntimeException("test");
                        };
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.addAll(Arrays.asList("anonymous", "working3"));
                            return f.apply(o.toArray());
                        };
                    }
                }
        ));

        swg.addVertex("any", "thing");
    }

    @Test
    @FeatureRequirementSet(FeatureRequirementSet.Package.VERTICES_ONLY)
    public void shouldShortCircuitStrategyWithNoOp() {
        final StrategyGraph swg = new StrategyGraph(g);
        swg.getStrategy().setGraphStrategy(new SequenceStrategy(
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.addAll(Arrays.asList("anonymous", "working1"));
                            return f.apply(o.toArray());
                        };
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        // if "f" is not applied then the next step and following steps won't process
                        return (f) -> (args) -> null;
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.addAll(Arrays.asList("anonymous", "working3"));
                            return f.apply(o.toArray());
                        };
                    }
                }
        ));

        assertNull(swg.addVertex("any", "thing"));
    }

    @Test
    @FeatureRequirementSet(FeatureRequirementSet.Package.VERTICES_ONLY)
    public void shouldDoSomethingBeforeAndAfter() {
        final StrategyGraph swg = new StrategyGraph(g);
        swg.getStrategy().setGraphStrategy(new SequenceStrategy(
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final Vertex v = f.apply(args);
                            // this  means that the next strategy and those below it executed including
                            // the implementation
                            assertEquals("working2", v.property("anonymous").value());
                            // now do something with that vertex after the fact
                            v.properties("anonymous").remove();
                            v.property("anonymous", "working1");
                            return v;
                        };

                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final Vertex v = f.apply(args);
                            // this  means that the next strategy and those below it executed including
                            // the implementation
                            assertEquals("working3", v.property("anonymous").value());
                            // now do something with that vertex after the fact
                            v.properties("anonymous").remove();
                            v.property("anonymous", "working2");
                            return v;
                        };
                    }
                },
                new GraphStrategy() {
                    @Override
                    public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context ctx) {
                        return (f) -> (args) -> {
                            final List<Object> o = new ArrayList<>(Arrays.asList(args));
                            o.addAll(Arrays.asList("anonymous", "working3"));
                            return f.apply(o.toArray());
                        };
                    }
                }
        ));

        final Vertex v = swg.addVertex("any", "thing");

        assertNotNull(v);
        assertEquals("thing", v.property("any").value());
        assertEquals("working1", v.property("anonymous").value());
    }

    @Test
    public void shouldHaveAllMethodsImplemented() throws Exception {
        final Method[] methods = GraphStrategy.class.getDeclaredMethods();
        final SpyGraphStrategy spy = new SpyGraphStrategy();
        final SequenceStrategy strategy = new SequenceStrategy(spy);

        // invoke all the strategy methods
        Stream.of(methods).forEach(method -> {
            try {
                if (method.getName().equals("applyStrategyToTraversal"))
                    method.invoke(strategy, new DefaultGraphTraversal<>());
                else
                    method.invoke(strategy, new Strategy.Context(new StrategyGraph(g), new StrategyWrapped() {
                    }));

            } catch (Exception ex) {
                ex.printStackTrace();
                fail("Should be able to invoke function");
                throw new RuntimeException("fail");
            }
        });

        // check the spy to see that all methods were executed
        assertEquals(methods.length, spy.getCount());
    }

    public class SpyGraphStrategy implements GraphStrategy {

        private int count = 0;

        public int getCount() {
            return count;
        }

        private UnaryOperator spy() {
            count++;
            return UnaryOperator.identity();
        }

        @Override
        public UnaryOperator<Function<Object[], Iterator<Vertex>>> getGraphIteratorsVertexIteratorStrategy(final Strategy.Context<StrategyGraph> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Function<Object[], Iterator<Edge>>> getGraphIteratorsEdgeIteratorStrategy(final Strategy.Context<StrategyGraph> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Function<Object[], Vertex>> getAddVertexStrategy(final Strategy.Context<StrategyGraph> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<TriFunction<String, Vertex, Object[], Edge>> getAddEdgeStrategy(final Strategy.Context<StrategyVertex> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<Void>> getRemoveEdgeStrategy(final Strategy.Context<StrategyEdge> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<Void>> getRemoveVertexStrategy(final Strategy.Context<StrategyVertex> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Supplier<Void>> getRemovePropertyStrategy(final Strategy.Context<StrategyProperty<V>> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Function<String, VertexProperty<V>>> getVertexGetPropertyStrategy(final Strategy.Context<StrategyVertex> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Function<String, Property<V>>> getEdgeGetPropertyStrategy(final Strategy.Context<StrategyEdge> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<BiFunction<String, V, VertexProperty<V>>> getVertexPropertyStrategy(final Strategy.Context<StrategyVertex> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<BiFunction<String, V, Property<V>>> getEdgePropertyStrategy(final Strategy.Context<StrategyEdge> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<Object>> getVertexIdStrategy(final Strategy.Context<StrategyVertex> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<Graph>> getVertexGraphStrategy(final Strategy.Context<StrategyVertex> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<Object>> getEdgeIdStrategy(final Strategy.Context<StrategyEdge> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<Graph>> getEdgeGraphStrategy(final Strategy.Context<StrategyEdge> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<String>> getVertexLabelStrategy(final Strategy.Context<StrategyVertex> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<String>> getEdgeLabelStrategy(final Strategy.Context<StrategyEdge> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<Set<String>>> getVertexKeysStrategy(final Strategy.Context<StrategyVertex> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<Set<String>>> getEdgeKeysStrategy(final Strategy.Context<StrategyEdge> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Function<String, V>> getVertexValueStrategy(final Strategy.Context<StrategyVertex> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Function<String, V>> getEdgeValueStrategy(final Strategy.Context<StrategyEdge> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<Set<String>>> getVariableKeysStrategy(final Strategy.Context<StrategyVariables> ctx) {
            return spy();
        }

        @Override
        public <R> UnaryOperator<Function<String, Optional<R>>> getVariableGetStrategy(final Strategy.Context<StrategyVariables> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<BiConsumer<String, Object>> getVariableSetStrategy(final Strategy.Context<StrategyVariables> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Consumer<String>> getVariableRemoveStrategy(final Strategy.Context<StrategyVariables> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<Map<String, Object>>> getVariableAsMapStrategy(final Strategy.Context<StrategyVariables> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<Void>> getGraphCloseStrategy(final Strategy.Context<StrategyGraph> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Function<String[], Iterator<VertexProperty<V>>>> getVertexIteratorsPropertyIteratorStrategy(final Strategy.Context<StrategyVertex> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Function<String[], Iterator<Property<V>>>> getEdgeIteratorsPropertyIteratorStrategy(final Strategy.Context<StrategyEdge> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Function<String[], Iterator<V>>> getVertexIteratorsValueIteratorStrategy(final Strategy.Context<StrategyVertex> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Function<String[], Iterator<V>>> getEdgeIteratorsValueIteratorStrategy(final Strategy.Context<StrategyEdge> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<BiFunction<Direction, String[], Iterator<Vertex>>> getVertexIteratorsVertexIteratorStrategy(final Strategy.Context<StrategyVertex> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<BiFunction<Direction, String[], Iterator<Edge>>> getVertexIteratorsEdgeIteratorStrategy(final Strategy.Context<StrategyVertex> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Supplier<Void>> getRemoveVertexPropertyStrategy(final Strategy.Context<StrategyVertexProperty<V>> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Function<Direction, Iterator<Vertex>>> getEdgeIteratorsVertexIteratorStrategy(final Strategy.Context<StrategyEdge> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Supplier<Object>> getVertexPropertyIdStrategy(final Strategy.Context<StrategyVertexProperty<V>> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Supplier<String>> getVertexPropertyLabelStrategy(final Strategy.Context<StrategyVertexProperty<V>> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Supplier<Graph>> getVertexPropertyGraphStrategy(final Strategy.Context<StrategyVertexProperty<V>> ctx) {
            return spy();
        }


        @Override
        public <V> UnaryOperator<Supplier<Set<String>>> getVertexPropertyKeysStrategy(final Strategy.Context<StrategyVertexProperty<V>> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Supplier<Vertex>> getVertexPropertyGetElementStrategy(final Strategy.Context<StrategyVertexProperty<V>> ctx) {
            return spy();
        }

        @Override
        public <V, U> UnaryOperator<BiFunction<String, V, Property<V>>> getVertexPropertyPropertyStrategy(final Strategy.Context<StrategyVertexProperty<U>> ctx) {
            return spy();
        }

        @Override
        public <V, U> UnaryOperator<Function<String[], Iterator<Property<V>>>> getVertexPropertyIteratorsPropertyIteratorStrategy(final Strategy.Context<StrategyVertexProperty<U>> ctx) {
            return spy();
        }

        @Override
        public <V, U> UnaryOperator<Function<String[], Iterator<V>>> getVertexPropertyIteratorsValueIteratorStrategy(final Strategy.Context<StrategyVertexProperty<U>> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Function<Object[],GraphTraversal<Vertex, Vertex>>> getGraphVStrategy(final Strategy.Context<StrategyGraph> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Function<Object[],GraphTraversal<Edge, Edge>>> getGraphEStrategy(final Strategy.Context<StrategyGraph> ctx) {
            return spy();
        }

        @Override
        public UnaryOperator<Supplier<GraphTraversal>> getGraphOfStrategy(final Strategy.Context<StrategyGraph> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Supplier<V>> getVertexPropertyValueStrategy(final Strategy.Context<StrategyVertexProperty<V>> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Supplier<V>> getPropertyValueStrategy(final Strategy.Context<StrategyProperty<V>> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Supplier<String>> getVertexPropertyKeyStrategy(final Strategy.Context<StrategyVertexProperty<V>> ctx) {
            return spy();
        }

        @Override
        public <V> UnaryOperator<Supplier<String>> getPropertyKeyStrategy(final Strategy.Context<StrategyProperty<V>> ctx) {
            return spy();
        }
    }
}