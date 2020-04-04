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
import io.github.edmm.utils.Consts;
import lombok.var;
import org.apache.commons.io.FileUtils;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class AnsibleOrchestratorVisitor implements GroupVisitor {
    private static final Logger logger = LoggerFactory.getLogger(AnsibleOrchestratorVisitor.class);
    private final OrchestrationContext context;

    public AnsibleOrchestratorVisitor(OrchestrationContext context) {
                this.context=context;
    }


    @Override
    public void execute(List<DeploymentModelInfo> deployInfos) {
        File directory=context.getDirAccess();
        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        pb.directory(directory);

        //important for ip-address/hostname/ssh-port...
        Set<Compute> hosts = new HashSet<>();
        try {
            for (var info : deployInfos) {
                Compute host = TopologyGraphHelper.resolveHostingComputeComponent(context.getDeploymentModel().getTopology(), info.component)
                        .orElseThrow(() -> new IllegalArgumentException("can't find the hosting component"));
                hosts.add(host);
                var json = convertPropsToJson(info.properties);
                context.write(info.component.getName() + "_requiredProps.json", json.toString());
            }
            for (var compute : hosts) {
                var json = convertPropsToJson(compute.getProperties());
                context.write(compute.getName() + "_host.json", json.toString());

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




