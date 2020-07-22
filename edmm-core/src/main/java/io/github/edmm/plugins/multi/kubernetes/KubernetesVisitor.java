package io.github.edmm.plugins.multi.kubernetes;

import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.docker.Container;
import io.github.edmm.model.component.*;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.RelationVisitor;
import io.github.edmm.plugins.kubernetes.support.ImageMappingVisitor;
import io.github.edmm.plugins.multi.MultiVisitor;
import lombok.var;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class KubernetesVisitor implements MultiVisitor, RelationVisitor {

    private static final Logger logger = LoggerFactory.getLogger(KubernetesVisitor.class);
    protected final TransformationContext context;
    protected final Graph<RootComponent, RootRelation> graph;
    private final List<Container> stacks;


    public KubernetesVisitor(TransformationContext context) {
        this.context = context;
        this.graph = context.getTopologyGraph();
        this.stacks = new ArrayList<>();
    }


    @Override
    public void populate() {

        for (Container stack : stacks) {
            PluginFileAccess fileAccess = context.getFileAccess();
            buildDockerfile(stack, fileAccess);
            RootComponent component = stack.getTop();
            KubernetesResourceBuilder resourceBuilder = new KubernetesResourceBuilder(stack, graph, fileAccess);
            resourceBuilder.populateResources();
        }

    }

    protected void resolveBaseImage(Container stack) {
        ImageMappingVisitor imageMapper = new ImageMappingVisitor();
        stack.getComponents().forEach(component -> component.accept(imageMapper));
        stack.setBaseImage(imageMapper.getBaseImage());
    }

    protected void buildDockerfile(Container stack, PluginFileAccess fileAccess) {
        DockerfileBuildingVisitor dockerfileBuilder = new DockerfileBuildingVisitor(stack, fileAccess);
        dockerfileBuilder.populateDockerfile();
    }

    //for now: lazy evaluatuion; do nothing until leaf is reached
    @Override
    public void visit(RootComponent component) {

        if (!TopologyGraphHelper.isComponentHostedOnLeaf(graph, component)) {
            return;
        }
        // announce that this will be set later
        component.addProperty("hostname", null);


        Container stack = new Container();
        List<RootComponent> compStack = TopologyGraphHelper.resolveAllHostingComponents(graph, component);
        for (var comp : compStack) {
            logger.info("add comp:{} to stack", comp.getName());
            stack.addComponent(comp);
        }

        resolveBaseImage(stack);
        stacks.add(stack);
    }

    @Override
    public void visit(Compute component) {
        visit((RootComponent) component);
    }

    @Override
    public void visit(Tomcat component) {
        visit((RootComponent) component);
    }

    @Override
    public void visit(MysqlDbms component) {
        visit((RootComponent) component);
    }

    @Override
    public void visit(Database component) {
        visit((RootComponent) component);
    }

    @Override
    public void visit(Dbms component) {
        visit((RootComponent) component);
    }

    @Override
    public void visit(MysqlDatabase component) {
        visit((RootComponent) component);
    }

    @Override
    public void visit(WebApplication component) {
        visit((RootComponent) component);
    }


}
