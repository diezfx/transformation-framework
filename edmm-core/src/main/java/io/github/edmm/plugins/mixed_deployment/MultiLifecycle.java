package io.github.edmm.plugins.mixed_deployment;

import io.github.edmm.core.plugin.AbstractLifecycle;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.component.Compute;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.VisitorHelper;


import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiLifecycle extends AbstractLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(MultiLifecycle.class);

    private final TransformationContext context;

    public MultiLifecycle(TransformationContext context) {
        this.context = context;
    }

    // prepare the graph
    @Override
    public void prepare() {

    }

    @Override
    public void transform() {
        logger.info("Begin transformation to Multi...");
        MultiVisitor visitor = new MultiVisitor(context);


        // Reverse the graph to find sources
        EdgeReversedGraph<RootComponent, RootRelation> dependencyGraph = new EdgeReversedGraph<>(
                context.getModel().getTopology());
        // Apply the topological sort
        TopologicalOrderIterator<RootComponent, RootRelation> iterator = new TopologicalOrderIterator<>(
                dependencyGraph);

        while (iterator.hasNext()) {

            RootComponent comp=iterator.next();

            comp.accept(visitor);

            logger.info("{}",comp.getName());
            

        }

        VisitorHelper.visit(context.getModel().getComponents(), visitor, component -> component instanceof Compute);
        // ... then all others
        VisitorHelper.visit(context.getModel().getComponents(), visitor);
        VisitorHelper.visit(context.getModel().getRelations(), visitor);
        logger.info("Transformation to Multi successful");
    }

}
