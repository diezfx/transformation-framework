package io.github.edmm.plugins.multi.orchestration;

import com.google.gson.JsonObject;
import freemarker.template.Configuration;
import io.github.edmm.core.parser.support.GraphHelper;
import io.github.edmm.core.plugin.TemplateHelper;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.DeploymentModel;
import io.github.edmm.model.Property;
import io.github.edmm.model.component.Compute;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.plugins.multi.MultiPlugin;
import lombok.var;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;


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
    public void visit(List<DeploymentModelInfo> deployInfos) {

        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        pb.directory(context.getSubDirAccess().getTargetDirectory());

        //important for ip-address/hostname/ssh-port...
        Set<Compute> hosts = new HashSet<>();
        try {
            for (var info : deployInfos) {
                Compute host = TopologyGraphHelper.resolveHostingComputeComponent(graph, info.component)
                        .orElseThrow(() -> new IllegalArgumentException("can't find the hosting component"));
                hosts.add(host);
                var json = convertPropsToJson(info.properties);
                context.getSubDirAccess().write(info.component.getName() + "_requiredProps.json", json.toString());
            }


            for (var compute : hosts) {
                var json = convertPropsToJson(compute.getProperties());
                context.getSubDirAccess().write(compute.getName() + "_host.json", json.toString());

            }

            pb.command("ansible-playbook", "deployment.yml");

            Process apply = pb.start();
            apply.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.error(e.toString());
        }
    }

    public JsonObject convertPropsToJson(Map<String, Property> resolvedComputedProps) {


        var json = new JsonObject();

        for (var prop : resolvedComputedProps.entrySet()) {
            json.addProperty(prop.getKey().toUpperCase(), prop.getValue().getValue());
        }
        return json;
    }


}




