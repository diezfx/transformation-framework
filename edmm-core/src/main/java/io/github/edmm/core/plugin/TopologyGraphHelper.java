package io.github.edmm.core.plugin;

import io.github.edmm.core.parser.support.GraphHelper;
import io.github.edmm.model.Property;
import io.github.edmm.model.component.Compute;
import io.github.edmm.model.component.Dbaas;
import io.github.edmm.model.component.Paas;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.ConnectsTo;
import io.github.edmm.model.relation.HostedOn;
import io.github.edmm.model.relation.RootRelation;
import org.jgrapht.Graph;

import java.util.*;
import java.util.stream.Collectors;

public abstract class TopologyGraphHelper {

    /**
     * Find the leafs in the graph (vertex that have no outgoing edges).
     *
     * @param graph The graph to search
     * @return mutable snapshot of all leaf vertices.
     */
    public static <V, E> Set<V> getLeafComponents(Graph<V, E> graph) {
        Set<V> vertexSet = graph.vertexSet();
        Set<V> leaves = new HashSet<>(vertexSet.size() * 2);
        for (V vertex : vertexSet) {
            if (graph.outgoingEdgesOf(vertex).isEmpty()) {
                leaves.add(vertex);
            }
        }
        return leaves;
    }

    /**
     * Fetch all of the dependents of the given target.
     *
     * @return mutable snapshot of the sources of all incoming edges
     */
    public static <V, E> Set<V> getSourceComponents(Graph<V, E> graph, V target, Class<? extends E> clazz) {
        Set<E> edges = graph.incomingEdgesOf(target);
        Set<V> sources = new LinkedHashSet<>();
        for (E edge : edges) {
            if (clazz.isInstance(edge)) {
                sources.add(graph.getEdgeSource(edge));
            }
        }
        return sources;
    }

    /**
     * Fetch all of the dependencies of the given source.
     *
     * @return mutable snapshot of the targets of all outgoing edges
     */
    public static <V, E> Set<V> getTargetComponents(Graph<V, E> graph, V source, Class<? extends E> clazz) {
        Set<E> edges = graph.outgoingEdgesOf(source);
        Set<V> targets = new LinkedHashSet<>();
        for (E edge : edges) {
            if (clazz.isInstance(edge)) {
                targets.add(graph.getEdgeTarget(edge));
            }
        }
        return targets;
    }

    @SuppressWarnings("unchecked")
    public static <V, E, T> List<T> getVertices(Graph<V, E> graph, Class<T> clazz) {
        return (List<T>) graph.vertexSet()
            .stream()
            .filter(clazz::isInstance)
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public static <V, E, T> List<T> getEdges(Graph<V, E> graph, Class<T> clazz) {
        return (List<T>) graph.edgeSet()
            .stream()
            .filter(clazz::isInstance)
            .collect(Collectors.toList());
    }

    public static Optional<RootComponent> resolveHostingComponent(Graph<RootComponent, RootRelation> graph, RootComponent component) {
        Set<RootComponent> targetComponents = getTargetComponents(graph, component, HostedOn.class);
        Optional<RootComponent> optionalComponent = targetComponents.stream().findFirst();
        if (optionalComponent.isPresent()) {
            RootComponent hostingComponent = optionalComponent.get();
            if (hostingComponent instanceof Compute
                || hostingComponent instanceof Dbaas
                || hostingComponent instanceof Paas) {
                return Optional.of(hostingComponent);
            } else {
                return resolveHostingComponent(graph, hostingComponent);
            }
        } else {
            // Leaf reached
            return Optional.empty();
        }
    }


    /**
     * find all properties that are only known during runtime
     * at the moment just return everything
     * otherwise give a warning that some are not set
     *
     * @param component
     * @return
     */
    public static Map<String, Property> findCompStackProperties(Graph<RootComponent, RootRelation> graph, RootComponent component) {
        Map<String, Property> result = new HashMap<>();
        var hosts = TopologyGraphHelper.resolveAllHostingComponents(graph, component);

        for (var host : hosts) {
            for (var prop : host.getProperties().entrySet()) {
                result.put(prop.getKey(), prop.getValue());
            }
        }
        return result;
    }

    public static Map<String, Property> resolveAllPropertyReferences(Graph<RootComponent, RootRelation> graph, RootComponent component, Map<String, Property> props) {

        Map<String, Property> result = new HashMap<>();
        for (var prop : props.entrySet()) {
            if (prop.getValue().getValue() != null && prop.getValue().getValue().contains("${")) {
                result.put(prop.getKey(), resolveReferencedProperty(graph, component, prop.getValue()));
            } else {
                result.put(prop.getKey(), prop.getValue());
            }
        }
        return result;


    }

    /**
     *
     * @param graph
     * @param component
     * @return all Properties this component has and its hosts
     */
    public static Map<String, Property> findAllProperties(Graph<RootComponent, RootRelation> graph, RootComponent component) {
        Map<String, Property> result = findCompStackProperties(graph, component);

        /* // find all properties that are known through connectsto connections.
        Set<RootComponent> targets = TopologyGraphHelper.getTargetComponents(graph, component, ConnectsTo.class);

        for(var target:targets){
            var properties=findCompStackProperties(graph,target);
            for(var prop: properties.entrySet()){
                var  envName = (target.getNormalizedName() + "_" + prop.getValue().getNormalizedName());
                result.put(envName,prop.getValue());
            }

        }
         */
        return result;
    }

    /**
     * @param graph
     * @param comp
     * @param prop
     * @return the referenced property
     */
    public static Property resolveReferencedProperty(Graph<RootComponent, RootRelation> graph, RootComponent comp, Property prop) {
        Set<RootComponent> targets = TopologyGraphHelper.getTargetComponents(graph, comp, ConnectsTo.class);

        String[] name_var = prop.getValue().
                replace("${", "")
                .replace("}", "").split("\\.");
        String compName = name_var[0];
        String varName = name_var[1];


        for (var target : targets) {
            if (target.getName().equals(compName)) {
                if (!findCompStackProperties(graph, target).containsKey(varName)) {
                    throw new IllegalStateException(String.format("the reference %s is not valid", prop.getValue()));
                }
                return findCompStackProperties(graph, target).get(varName);
            }
        }
        throw new IllegalStateException(String.format("the reference %s is not valid", prop.getValue()));
    }


    public static Optional<Compute> resolveHostingComputeComponent(Graph<RootComponent, RootRelation> graph, RootComponent component) {
        Set<RootComponent> targetComponents = getTargetComponents(graph, component, HostedOn.class);
        Optional<RootComponent> optionalComponent = targetComponents.stream().findFirst();
        if (optionalComponent.isPresent()) {
            RootComponent hostingComponent = optionalComponent.get();
            if (hostingComponent instanceof Compute) {
                return Optional.of((Compute) hostingComponent);
            } else {
                return resolveHostingComputeComponent(graph, hostingComponent);
            }
        } else {
            // Leaf reached
            return Optional.empty();
        }
    }

    public static void resolveChildComponents(Graph<RootComponent, RootRelation> graph, List<RootComponent> children, RootComponent component) {
        Set<RootComponent> targetComponents = getTargetComponents(graph, component, HostedOn.class);
        Optional<RootComponent> optionalComponent = targetComponents.stream().findFirst();
        if (optionalComponent.isPresent()) {
            RootComponent child = optionalComponent.get();
            children.add(child);
            resolveChildComponents(graph, children, child);
        }
    }

    /**
     * get the complete stack this component is hosted on including itself
     * e.g. compute -> tomcat -> application
     *
     * @param graph
     * @param component
     * @return
     */
    public static List<RootComponent> resolveAllHostingComponents(Graph<RootComponent, RootRelation> graph, RootComponent component) {

        List<RootComponent> componentStack = new ArrayList<>();
        componentStack.add(component);


        Optional<RootComponent> host = TopologyGraphHelper.resolveComponentHostedOn(graph, component);
        while (host.isPresent()) {
            componentStack.add(host.get());
            host = TopologyGraphHelper.resolveComponentHostedOn(graph, host.get());
        }
        Collections.reverse(componentStack);
        return componentStack;

    }

    public static Optional<RootComponent> resolveComponentHostedOn(Graph<RootComponent, RootRelation> graph, RootComponent component) {

        Set<RootComponent> targetComponents = getTargetComponents(graph, component, HostedOn.class);
        return targetComponents.stream().findFirst();
    }

    /**
     * leaf in the sense of no other comp is hosted on this one
     *
     * @return true if no other component is hosted on this one
     */
    public static boolean isComponentHostedOnLeaf(Graph<RootComponent, RootRelation> graph, RootComponent component) {
        Set<RootComponent> sourceComponents = getSourceComponents(graph, component, HostedOn.class);
        return sourceComponents.isEmpty();

    }

}
