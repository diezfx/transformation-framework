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
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TerraformOrchestratorVisitor implements GroupVisitor {

    private static final Logger logger = LoggerFactory.getLogger(TerraformOrchestratorVisitor.class);
    protected final OrchestrationContext orchContext;

    public TerraformOrchestratorVisitor(OrchestrationContext orchContext) {
        this.orchContext=orchContext;
    }

    @SneakyThrows
    public void visit(List<DeploymentModelInfo> deployInfos) {
        Gson gson = new Gson();
        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        pb.directory(orchContext.getDirAccess());


        for (var info : deployInfos) {
            List<Artifact> providerInfo = info.component.getArtifacts().stream().filter(a -> a.getName().equals("provider"))
                    .collect(Collectors.toList());
            // todo clean solution
            if (providerInfo.isEmpty()) {
                throw new IllegalArgumentException("The providerinfo for openstack was not provided");
            }
            // read input variables
            String basename = FilenameUtils.getName(providerInfo.stream().findFirst().get().getValue());
            String newPath = "./files/" + info.component.getNormalizedName() + "/" + basename;
            File file = new File(orchContext.getDirAccess(),newPath );
            String openstackProviderInfo=FileUtils.readFileToString(file,StandardCharsets.UTF_8);
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

            File propFile = new File(orchContext.getDirAccess(), info.component.getName() + "_computed_properties" + ".json");
            String computeInfo= FileUtils.readFileToString(propFile, StandardCharsets.UTF_8);
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
