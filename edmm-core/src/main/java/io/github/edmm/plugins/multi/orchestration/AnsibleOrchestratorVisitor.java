package io.github.edmm.plugins.multi.orchestration;

import com.google.gson.JsonObject;
import freemarker.template.Configuration;
import io.github.edmm.core.plugin.TemplateHelper;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.Property;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.plugins.multi.MultiPlugin;
import lombok.var;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class AnsibleOrchestratorVisitor implements GroupVisitor {
    private static final Logger logger = LoggerFactory.getLogger(AnsibleOrchestratorVisitor.class);
    protected final TransformationContext context;
    protected final Configuration cfg = TemplateHelper.forClasspath(MultiPlugin.class, "/plugins/multi");
    protected final Graph<RootComponent, RootRelation> graph;

    public AnsibleOrchestratorVisitor(TransformationContext context) {
        this.context = context;
        this.graph = context.getTopologyGraph();
    }


    @Override
    public void visit(List<RootComponent> components) {

        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        pb.directory(context.getSubDirAccess().getTargetDirectory());
        try {
            for (var component : components) {


                Map<String, Property> allProps = TopologyGraphHelper.findAllProperties(graph, component);
                Map<String, Property> computedProps = new HashMap<>();

                // filter runtime properties and add them to the json
                for (var prop : allProps.entrySet()) {
                    if (prop.getValue().getValue() == null) {
                        continue;
                    }
                    if (prop.getValue().isComputed() || prop.getValue().getValue().startsWith("${")) {
                        computedProps.put(prop.getKey(), prop.getValue());
                    }
                }

                var resolvedComputedProps = TopologyGraphHelper.resolveAllPropertyReferences(graph, component, computedProps);

                var json = new JsonObject();

                for (var prop : resolvedComputedProps.entrySet()) {
                    json.addProperty(prop.getKey().toUpperCase(), prop.getValue().getValue());
                }

                context.getSubDirAccess().write(component.getName() + "_requiredProps.json", json.toString());
            }
            pb.command("ansible-playbook", "deployment.yml");

            Process apply = pb.start();
            apply.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.error(e.toString());
        }
    }


}




