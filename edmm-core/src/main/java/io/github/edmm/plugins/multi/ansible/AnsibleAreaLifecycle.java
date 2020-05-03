package io.github.edmm.plugins.multi.ansible;

import io.github.edmm.core.plugin.AbstractLifecycle;
import io.github.edmm.core.plugin.support.CheckModelResult;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.VisitorHelper;
import io.github.edmm.plugins.ComputeAreaSupportVisitor;
import org.jgrapht.traverse.TopologicalOrderIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnsibleAreaLifecycle extends AbstractLifecycle {

    public static final String FILE_NAME = "deployment.yml";

    private static final Logger logger = LoggerFactory.getLogger(io.github.edmm.plugins.ansible.AnsibleLifecycle.class);

    public AnsibleAreaLifecycle(TransformationContext context) {
        super(context);
    }

    @Override
    public CheckModelResult checkModel() {
        ComputeAreaSupportVisitor visitor = new ComputeAreaSupportVisitor(context);
        VisitorHelper.visit(context.getGroup().getGroupComponents(), visitor);
        return visitor.getResult();
    }

    @Override
    public void prepare() {

    }

    @Override
    public void transform() {
        logger.info("Begin transformation to Ansible...");
        AnsibleVisitor visitor = new AnsibleVisitor(context);

        TopologicalOrderIterator<RootComponent, RootRelation> subIterator = new TopologicalOrderIterator<>(
            context.getGroup().getSubGraph());

        while (subIterator.hasNext()){
            subIterator.next().accept(visitor);
        }
        visitor.populate();
        logger.info("Transformation to Ansible successful");
    }


    @Override
    public void cleanup() {

    }
}
