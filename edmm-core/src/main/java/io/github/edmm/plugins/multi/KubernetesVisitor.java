package io.github.edmm.plugins.multi;

import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.docker.Container;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.model.visitor.RelationVisitor;
import io.github.edmm.plugins.multi.support.kubernetes.DockerfileBuildingVisitor;
import io.github.edmm.plugins.multi.support.kubernetes.ImageMappingVisitor;
import io.github.edmm.plugins.multi.support.kubernetes.KubernetesResourceBuilder;
import lombok.var;
import io.github.edmm.model.component.*;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.core.plugin.TopologyGraphHelper;

import java.util.List;

import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class KubernetesVisitor implements ComponentVisitor, RelationVisitor {

    protected final TransformationContext context;
    protected final Graph<RootComponent, RootRelation> graph;
    private static final Logger logger = LoggerFactory.getLogger(KubernetesVisitor.class);

    public KubernetesVisitor(TransformationContext context) {
        this.context = context;
        this.graph = context.getTopologyGraph();
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
            logger.info("is not leaf");
            return;
        }
        // announce that this will be set later
        component.addProperty("hostname", null);
        PluginFileAccess fileAccess = context.getSubDirAccess();
        Container stack = new Container();

        List<RootComponent> compStack = TopologyGraphHelper.resolveAllHostingComponents(graph, component);
        for (var comp : compStack) {
            logger.info("add comp:{} to stack", comp.getName());
            stack.addComponent(comp);
        }

        resolveBaseImage(stack);
        buildDockerfile(stack, fileAccess);

        KubernetesResourceBuilder resourceBuilder = new KubernetesResourceBuilder(stack, component,context.getTopologyGraph(), fileAccess);
        resourceBuilder.populateResources();

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