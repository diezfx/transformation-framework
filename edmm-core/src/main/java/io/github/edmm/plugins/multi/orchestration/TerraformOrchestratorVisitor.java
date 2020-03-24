package io.github.edmm.plugins.multi.orchestration;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.Artifact;
import io.github.edmm.model.Property;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.relation.RootRelation;
import lombok.SneakyThrows;
import lombok.var;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TerraformOrchestratorVisitor implements GroupVisitor {

    private static final Logger logger = LoggerFactory.getLogger(TerraformOrchestratorVisitor.class);
    protected final TransformationContext context;
    protected final Graph<RootComponent, RootRelation> graph;

    public TerraformOrchestratorVisitor(TransformationContext context) {
        this.context = context;
        this.graph = context.getTopologyGraph();
    }

    @SneakyThrows
    public void visit(List<DeploymentModelInfo> deployInfos) {
        Gson gson = new Gson();
        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        pb.directory(context.getSubDirAccess().getTargetDirectory());


        for (var info : deployInfos) {
            List<Artifact> providerInfo = info.component.getArtifacts().stream().filter(a -> a.getName().equals("provider"))
                    .collect(Collectors.toList());
            // todo clean solution
            if (providerInfo.isEmpty()) {
                throw new IllegalArgumentException("The providerinfo for openstack was not provided");
            }
            // read input variables
            String openstackProviderInfo = context.getSubDirAccess().readToString(providerInfo.iterator().next().getValue());
            HashMap<String, String> obj = gson.fromJson(openstackProviderInfo, HashMap.class);

            Map<String, String> env = pb.environment();
            for (String key : obj.keySet()) {
                env.put("TF_VAR_" + key, obj.get(key));
            }

            // deploy
            pb.command("terraform", "init");
            Process init = pb.start();
            init.waitFor();

            pb.command("terraform", "apply", "-auto-approve", "-input=false");

            Process apply = pb.start();
            apply.waitFor();


            // read output variables and write back in model
            String computeInfo = context.getSubDirAccess().readToStringTargetDir(info.component.getName() + "_computed_properties" + ".json");
            HashMap<String, String> output = gson.fromJson(computeInfo, new TypeToken<HashMap<String, String>>() {
            }.getType());

            Map<String, Property> properties = info.component.getProperties();


            // set all properties
            // object is a map
            for (var computed_prop : output.entrySet()) {

                if (!properties.containsKey(computed_prop.getKey())) {
                    logger.warn(String.format("The property(%s) is not there, so it was added to props", computed_prop.getKey()));
                    info.component.addProperty(computed_prop.getKey(), computed_prop.getValue());
                } else {
                    var prop = properties.get(computed_prop.getKey());
                    prop.setValue(computed_prop.getValue());
                }
            }
        }


    }


}
