package io.github.edmm.core.plugin;

import io.github.edmm.model.Property;
import io.github.edmm.model.PropertyBlocks;
import io.github.edmm.model.component.Compute;
import io.github.edmm.model.component.Dbaas;
import io.github.edmm.model.component.Paas;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.HostedOn;
import io.github.edmm.model.relation.RootRelation;
import lombok.var;
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
     * resolve all capabilities that are provided and are "inherited" through hosted on
     * @param graph
     * @param component
     * @return
     */
    public static PropertyBlocks resolveHostCapabilities(Graph<RootComponent, RootRelation> graph, RootComponent component) {
        var myCapabilities = new PropertyBlocks(new HashMap<>());
        Optional<RootComponent> host = TopologyGraphHelper.resolveHostingComponent(graph, component);
        while (host.isPresent()) {
            myCapabilities = myCapabilities.mergeBlocks(host.get().getCapabilities());


            host = TopologyGraphHelper.resolveHostingComponent(graph, host.get());
        }
        return myCapabilities;
    }


    /**
     * @param reqs
     * @return all matched values; only returns if all values could be matched otherwise empty
     */
    public static Optional<Map<String, Property>> findMatchingProperties(Graph<RootComponent, RootRelation> graph, Map<String, Property> reqs, RootComponent component) {
        Map<String, Property> matchedProperties = new HashMap<>();
        for (var req : reqs.entrySet()) {

            var prop = resolveCapabilityWithHosting(graph, req.getKey(), component);
            if (!prop.isPresent()) {
                return Optional.empty();
            }
            matchedProperties.put(req.getKey(), prop.get());

        }
        return Optional.of(matchedProperties);

    }

    /**
     * resolve capability with the name; prefer the one from the component if not fulfillable look through hosted_on relatuions
     *
     * @param graph
     * @param propName
     * @param component
     * @return
     */
    public static Optional<Property> resolveCapabilityWithHosting(Graph<RootComponent, RootRelation> graph, String propName, RootComponent component) {

        Optional<Property> prop = component.getCapability(propName);

        if (prop.isPresent()) {
            return prop;
        }

        PropertyBlocks hostBlocks = resolveHostCapabilities(graph, component);


        return hostBlocks.getPropertyByName(propName);

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
     * @param graph
     * @param component
     * @return
     */
    public static List<RootComponent> resolveAllHostingComponents(Graph<RootComponent, RootRelation> graph,RootComponent component){

        List<RootComponent> componentStack= new ArrayList<>();
        componentStack.add(component);


        Optional<RootComponent> host = TopologyGraphHelper.resolveComponentHostedOn(graph, component);
        while (host.isPresent()) {
            componentStack.add(host.get());


            host = TopologyGraphHelper.resolveComponentHostedOn(graph, host.get());
        }
        Collections.reverse(componentStack);
        return componentStack;

    }

    public static Optional<RootComponent> resolveComponentHostedOn(Graph<RootComponent, RootRelation> graph,RootComponent component){

        Set<RootComponent> targetComponents = getTargetComponents(graph, component, HostedOn.class);
        return targetComponents.stream().findFirst();
    }
    /**
     * leaf in the sense of no other comp is hosted on this one
     * @return true if no other component is hosted on this one
     */
    public static boolean isComponentHostedOnLeaf(Graph<RootComponent, RootRelation> graph,RootComponent component){
        Set<RootComponent> sourceComponents = getSourceComponents(graph, component, HostedOn.class);
        if(sourceComponents.isEmpty()){
            return true;
        }
        return false;

    }
}
