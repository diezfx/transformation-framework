package io.github.edmm.plugins.multi.orchestration;

import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.model.Property;
import io.github.edmm.model.PropertyBlocks;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.ConnectsTo;
import io.github.edmm.model.relation.RootRelation;
import lombok.var;
import org.jgrapht.Graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;

public class OrchestrationHelper {

    /**
     * for every property group look through all connects_to connections
     *
     * @param component
     * @return
     */
    public static PropertyBlocks findAllRequirements(Graph<RootComponent, RootRelation> graph, RootComponent component,
                                                     Logger logger) {
        PropertyBlocks requirements = component.getRequirements();
        PropertyBlocks result = new PropertyBlocks(new HashMap<>());

        for (var block : requirements.getBlocks().entrySet()) {
            if (block.getKey().equals("host")) {
                continue;
            }
            // iterate over all connects_to relations
            Set<RootComponent> targetComponents = TopologyGraphHelper.getTargetComponents(graph, component,
                    ConnectsTo.class);
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

    /**
     * look for host requirements; special case at the moment
     *
     * @param component the component which requirements are looked for elsewhere
     * @return a jsonobject with all found requirements filled in todo write the
     * values in the model as well?
     */
    public static PropertyBlocks findHostRequirements(Graph<RootComponent, RootRelation> graph, RootComponent component,
                                                      Logger logger) {
        PropertyBlocks result = new PropertyBlocks(new HashMap<>());
        PropertyBlocks requirements = component.getRequirements();
        HashMap<String, Property> hostR = new HashMap<>();
        Optional<Map<String, Property>> hostRequirements = requirements.getBlockByName("host");

        if (!hostRequirements.isPresent()) {
            return result;
        }

        hostRequirements.get().forEach((propName, propValue) -> {
            var prop = TopologyGraphHelper.resolveCapabilityWithHostingByType(graph, propValue.getType(), component);
            if (prop.isPresent()) {
                hostR.put(propName, prop.get());
            } else {
                logger.warn("the property {} could not be fulfilled", propName);
            }
        });
        result.addBlock("host", hostR);
        return result;
    }
}
