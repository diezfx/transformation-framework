package io.github.edmm.plugins.multi.orchestration;

import freemarker.template.Configuration;
import io.github.edmm.core.plugin.TemplateHelper;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.Property;
import io.github.edmm.model.PropertyBlocks;
import io.github.edmm.model.component.*;
import io.github.edmm.model.relation.ConnectsTo;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.plugins.multi.MultiPlugin;
import lombok.var;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;


public class AnsibleOrchestratorVisitor implements ComponentVisitor {
    private static final Logger logger = LoggerFactory.getLogger(AnsibleOrchestratorVisitor.class);
    protected final TransformationContext context;
    protected final Configuration cfg = TemplateHelper.forClasspath(MultiPlugin.class, "/plugins/multi");
    protected final Graph<RootComponent, RootRelation> graph;

    public AnsibleOrchestratorVisitor(TransformationContext context) {
        this.context = context;
        this.graph = context.getTopologyGraph();
    }


    @Override
    public void visit(RootComponent component) {

        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        pb.directory(context.getSubDirAccess().getTargetDirectory());


        PropertyBlocks requiredProps = findHostRequirements(component);
        logger.info(requiredProps.getBlockByName("host").toString());
        requiredProps.mergeBlocks(findAllRequirements(component));





        // look for all other requirements through relations


        try {
            context.getSubDirAccess().write(component.getNormalizedName() + ".json", requiredProps.toJson().toString());
            pb.command("ansible-playbook", component.getNormalizedName() + ".yml");

            Process apply = pb.start();
            apply.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.error(e.toString());
        }
    }

    /**
     * look for host requirements; special case at the moment
     *
     * @param component the component which requirements are looked for elsewhere
     * @return a jsonobject with all found requirements filled in
     * todo write the values in the model as well?
     */
    private PropertyBlocks findHostRequirements(RootComponent component) {
        PropertyBlocks result = new PropertyBlocks(new HashMap<>());
        PropertyBlocks requirements = component.getRequirements();
        HashMap<String, Property> hostR = new HashMap<>();
        Optional<Map<String, Property>> hostRequirements = requirements.getBlockByName("host");

        if (!hostRequirements.isPresent()) {
            return result;
        }

        hostRequirements.get().forEach((propName, propValue) -> {
            //todo error checking if required stuff is not there
            var prop = TopologyGraphHelper.resolveCapabilityWithHosting(graph, propName, component);
            prop.ifPresent(p -> hostR.put(propName, p));
        });
        result.addBlock("host",hostR);
        return result;
    }

    /**
     * for every property group look through all connects_to connections
     * @param component
     * @return
     */
    private PropertyBlocks findAllRequirements(RootComponent component) {
        PropertyBlocks requirements = component.getRequirements();
        PropertyBlocks result = new PropertyBlocks(new HashMap<>());

        for (var block : requirements.getBlocks().entrySet()) {
            if(block.getKey().equals("host")){
                continue;
            }
            // iterate over all connects_to relations
            Set<RootComponent> targetComponents = TopologyGraphHelper.getTargetComponents(graph, component, ConnectsTo.class);
            Optional<Map<String, Property>> match = Optional.empty();
            for (var targetComp : targetComponents) {
                match = TopologyGraphHelper.findMatchingProperties(graph, block.getValue(), targetComp);
                if (match.isPresent()) {
                    break;
                }

            }
            if (match.isPresent()) {
                var blockMap = new HashMap<String, Property>();
                for (var prop : match.get().entrySet()) {

                    blockMap.put(prop.getKey(), prop.getValue());
                }
                result.addBlock(block.getKey(), blockMap);
            } else {
                logger.warn("No fitting block found for {}", block.getKey());
            }

        }
        return result;

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




