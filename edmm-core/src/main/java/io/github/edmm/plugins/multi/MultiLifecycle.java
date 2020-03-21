package io.github.edmm.plugins.multi;

import com.google.gson.Gson;
import io.github.edmm.core.plugin.AbstractLifecycle;
import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.HostedOn;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.plugins.multi.model_extensions.OrchestrationTechnologyMapping;
import io.github.edmm.plugins.multi.orchestration.*;
import lombok.var;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.TransitiveClosure;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MultiLifecycle extends AbstractLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(MultiLifecycle.class);

    public MultiLifecycle(TransformationContext context) {
        super(context);
    }

    // prepare the graph
    @Override
    public void prepare() {

    }

    private Map<String, Set<RootComponent>> initGPONodes(Graph<Set<RootComponent>, DefaultEdge> graph, Set<RootComponent> components) {
        Map<String, Set<RootComponent>> compSets = new HashMap<>();
        components.forEach((component) -> {
            Set<RootComponent> compSet = new HashSet<>();
            compSet.add(component);
            compSets.put(component.getName(), compSet);
            graph.addVertex(compSet);
        });

        return compSets;


    }

    private void initGPOEdges(Graph<Set<RootComponent>, DefaultEdge> graph, Map<String, Set<RootComponent>> compSets) {
        for (Set<RootComponent> sourceComponent : graph.vertexSet()) {
            for (RootRelation relation : sourceComponent.stream().findFirst().get().getRelations()) {
                Set<RootComponent> targetComponent = compSets.get(relation.getTarget());
                graph.addEdge(targetComponent, sourceComponent);
            }
        }
    }

    /**
     * merge g1 and g2
     * remove nodes redirect edges to new node
     *
     * @param graph
     * @param g1
     * @param g2
     * @param compSets
     */
    public DirectedAcyclicGraph<Set<RootComponent>, DefaultEdge> mergeGroups(DirectedAcyclicGraph<Set<RootComponent>, DefaultEdge> graph, Set<RootComponent> g1, Set<RootComponent> g2, Map<String, Set<RootComponent>> compSets) {
        // get all incoming and outgoing edges

        //make a backup
        DirectedAcyclicGraph<Set<RootComponent>, DefaultEdge> newGraph = new DirectedAcyclicGraph<>(DefaultEdge.class);
        Graphs.addGraph(newGraph, graph);


        System.out.println("merge " + g1 + "with " + g2);
        var edgesIncoming = Stream.concat(newGraph.incomingEdgesOf(g1).stream(), newGraph.incomingEdgesOf(g2).stream())
                .filter(edge -> !newGraph.getEdgeSource(edge).containsAll(g1) && !newGraph.getEdgeSource(edge).containsAll(g2))
                .collect(Collectors.toSet());
        var edgesOutgoing = Stream.concat(newGraph.outgoingEdgesOf(g1).stream(), newGraph.outgoingEdgesOf(g2).stream())
                .filter(edge -> !newGraph.getEdgeTarget(edge).containsAll(g1) && !newGraph.getEdgeTarget(edge).containsAll(g2))
                .collect(Collectors.toSet());

        var newGroup = new HashSet<RootComponent>();
        newGroup.addAll(g1);
        newGroup.addAll(g2);

        newGraph.addVertex(newGroup);


        for (var edge : edgesOutgoing) {
            var target = newGraph.getEdgeTarget(edge);
            newGraph.removeEdge(edge);
            newGraph.addEdge(newGroup, target, edge);
        }

        for (var edge : edgesIncoming) {
            var source = newGraph.getEdgeSource(edge);
            newGraph.removeEdge(edge);
            newGraph.addEdge(source, newGroup, edge);
        }


        for (var node : newGroup) {
            compSets.put(node.getName(), newGroup);
        }

        CycleDetector<Set<RootComponent>, DefaultEdge> cycleDetector = new CycleDetector<>(newGraph);
        //rollback cycles detected
        if (cycleDetector.detectCycles()) {

            for (var node : g1) {
                compSets.put(node.getName(), g1);
            }
            for (var node : g2) {
                compSets.put(node.getName(), g2);
            }
            return graph;
        }
        newGraph.removeVertex(g1);
        newGraph.removeVertex(g2);
        return newGraph;
    }


    public Graph<Set<RootComponent>, DefaultEdge> DetermineGroupProvisioningOrder(Graph<RootComponent, RootRelation> graph) {
        DirectedAcyclicGraph<Set<RootComponent>, DefaultEdge> targetGraph = new DirectedAcyclicGraph<Set<RootComponent>, DefaultEdge>(DefaultEdge.class);
        Map<String, Set<RootComponent>> componentSets = initGPONodes(targetGraph, graph.vertexSet());
        initGPOEdges(targetGraph, componentSets);

        var order = new HashSet<>(targetGraph.edgeSet());

        TransitiveReduction.INSTANCE.reduce(targetGraph);


        for (var e : order) {
            try {
                System.out.println(targetGraph);
                var source = targetGraph.getEdgeSource(e).stream().findFirst().get();
                var target = targetGraph.getEdgeTarget(e).stream().findFirst().get();
                if (getTechnology(source) != getTechnology(target)) {
                    continue;
                }
                targetGraph = mergeGroups(targetGraph, componentSets.get(source.getName()), componentSets.get(target.getName()), componentSets);


            } catch (IllegalArgumentException err) {
                err.printStackTrace();
            }

        }
        for (var tech : Technology.values()) {
            var nodes = new HashSet<>(targetGraph.vertexSet());
            for (var gk : nodes) {
                if (getTechnology(gk) != tech) {
                    continue;
                }
                TransitiveClosure.INSTANCE.closeDirectedAcyclicGraph(targetGraph);
                boolean b1 = true;
                boolean b2 = true;
                // no incoming edge exists

                var nodeSet = new HashSet<>(targetGraph.vertexSet());
                for (var gz : nodeSet) {
                    if (gk.containsAll(gz)) {
                        continue;
                    }
                    if (!targetGraph.containsVertex(gk)) {
                        continue;
                    }
                    for (var e : targetGraph.outgoingEdgesOf(gk)) {
                        if (targetGraph.getEdgeTarget(e).containsAll(gz)) {
                            b1 = false;
                        }
                    }
                    for (var e : targetGraph.outgoingEdgesOf(gz)) {
                        if (targetGraph.getEdgeTarget(e).containsAll(gk)) {
                            b2 = false;
                        }
                    }
                    if (getTechnology(gk) == getTechnology(gz) && (b1 || b2)) {
                        targetGraph = mergeGroups(targetGraph, gz, gk, componentSets);
                    }
                }

            }
            System.out.println(targetGraph);
        }


        return targetGraph;
    }


    public void createPlan(Graph<Set<RootComponent>, DefaultEdge> graph) {

        EdgeReversedGraph<RootComponent, RootRelation> dependencyGraph = new EdgeReversedGraph<>(
                context.getModel().getTopology());
        TopologicalOrderIterator<RootComponent, RootRelation> iterator = new TopologicalOrderIterator<>(
                dependencyGraph);

        TopologicalOrderIterator<Set<RootComponent>, DefaultEdge> iteratorgroupGraph = new TopologicalOrderIterator<>(

                graph);


        List<PlanStep> stepList = new ArrayList<>();
        List<MultiVisitor> contextList = new ArrayList<>();
        List<Set<RootComponent>> groups = new ArrayList<>();

        //init contexts
        MultiVisitor visitorContext;
        while (iteratorgroupGraph.hasNext()) {
            var group = iteratorgroupGraph.next();
            if (getTechnology(group) == Technology.ANSIBLE) {
                visitorContext = new AnsibleVisitor(context);
            } else if (getTechnology(group) == Technology.TERRAFORM) {
                visitorContext = new TerraformVisitor(context);
            } else if (getTechnology(group) == Technology.KUBERNETES) {
                visitorContext = new KubernetesVisitor(context);
            } else {
                String error = String.format("could not find technology: %s for components %s", getTechnology(group), group);
                throw new IllegalArgumentException(error);
            }
            groups.add(group);
            contextList.add(visitorContext);
            stepList.add(new PlanStep(getTechnology(group)));
        }


        while (iterator.hasNext()) {

            RootComponent comp = iterator.next();
            int index = -1;
            for (int i = 0; i < groups.size(); i++) {
                if (groups.get(i).contains(comp)) {
                    index = i;
                    break;
                }
            }
            context.setSubFileAcess("step" + index + "_" + getTechnology(comp).toString());
            comp.accept(contextList.get(index));
            stepList.get(index).components.add(comp.getNormalizedName());

        }

        Plan plan = new Plan();
        plan.steps = stepList;

        for (var cont : contextList) {
            cont.populate();
        }
        Writer writer = new StringWriter();
        context.getModel().getGraph().generateYamlOutput(writer);

        try {
            context.getFileAccess().write("state.yaml", writer.toString());
            context.getFileAccess().write("execution.plan.json", plan.toJson());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void transform() {
        logger.info("Begin transformation to Multi...");
        PluginFileAccess fileAccess = context.getFileAccess();

        // Reverse the graph to find sources
        EdgeReversedGraph<RootComponent, RootRelation> dependencyGraph = new EdgeReversedGraph<>(
                context.getModel().getTopology());
        // Apply the topological sort
        TopologicalOrderIterator<RootComponent, RootRelation> iterator = new TopologicalOrderIterator<>(
                dependencyGraph);


        //new Groupprovisioning
        Graph<Set<RootComponent>, DefaultEdge> groupGraph = (DetermineGroupProvisioningOrder(dependencyGraph));
        createPlan(groupGraph);


        // VisitorHelper.visit(context.getModel().getComponents(), visitor, component ->
        // component instanceof Compute);
        // ... then all others
        // VisitorHelper.visit(context.getModel().getComponents(), visitor);
        // VisitorHelper.visit(context.getModel().getRelations(), visitor);


        logger.info("Transformation to Multi successful");
    }

    @Override
    public void orchestrate() {
        PluginFileAccess fileAccess = context.getFileAccess();
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter y to continue with orchestration");
        String input = scanner.next();

        if (!input.equals("y")) {
            return;
        }
        var gson = new Gson();
        try {

            Plan plan = gson.fromJson(context.getFileAccess().readToStringTargetDir("execution.plan.json"), Plan.class);


            logger.info("Begin orchestration ...");
            // Reverse the graph to find sources
            EdgeReversedGraph<RootComponent, RootRelation> dependencyGraph = new EdgeReversedGraph<>(
                    context.getModel().getTopology());
            // Apply the topological sort
            TopologicalOrderIterator<RootComponent, RootRelation> iterator = new TopologicalOrderIterator<>(
                    dependencyGraph);


            for (int i = 0; i < plan.steps.size(); i++) {
                List<RootComponent> components = new ArrayList<>();
                for (var c : plan.steps.get(i).components) {
                    Optional<RootComponent> comp = context.getModel().getComponent(c);
                    comp.ifPresent(j -> components.add(j));
                }
                Technology tech = getTechnology(components.get(0));
                context.setSubFileAcess("step" + i + "_" + tech.toString());

                GroupVisitor visitorContext;
                logger.info("deployment_tool: {} ", tech);
                if (tech == Technology.ANSIBLE) {
                    visitorContext = new AnsibleOrchestratorVisitor(context);
                } else if (tech == Technology.TERRAFORM) {
                    visitorContext = new TerraformOrchestratorVisitor(context);
                } else if (tech == Technology.KUBERNETES) {
                    visitorContext = new KubernetesOrchestratorVisitor(context);
                } else {
                    String error = String.format("could not find technology: %s for component %s", tech, components);
                    throw new IllegalArgumentException(error);
                }
                visitorContext.visit(components);
            }
            Writer writer = new StringWriter();
            context.getModel().getGraph().generateYamlOutput(writer);
            fileAccess.write("state.yaml", writer.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private Technology getTechnology(RootComponent component) {
        Optional<Map<RootComponent, Technology>> deploymentTechList = context.getModel().getTechnologyMapping()
                .map(OrchestrationTechnologyMapping::getTechForComponents);
        return deploymentTechList.map(c -> c.get(component)).orElse(Technology.UNDEFINED);
    }

    private Technology getTechnology(Set<RootComponent> components) {
        return getTechnology(components.stream().findFirst().get());
    }


}
