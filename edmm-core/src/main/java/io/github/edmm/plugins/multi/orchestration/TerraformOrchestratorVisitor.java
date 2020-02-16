package io.github.edmm.plugins.multi.orchestration;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import freemarker.template.Configuration;
import io.github.edmm.core.plugin.TemplateHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.Artifact;
import io.github.edmm.model.Property;
import io.github.edmm.model.component.*;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.plugins.multi.MultiPlugin;
import lombok.SneakyThrows;
import lombok.var;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class TerraformOrchestratorVisitor implements ComponentVisitor {

    private static final Logger logger = LoggerFactory.getLogger(TerraformOrchestratorVisitor.class);
    protected final TransformationContext context;
    protected final Configuration cfg = TemplateHelper.forClasspath(MultiPlugin.class, "/plugins/multi");
    protected final Graph<RootComponent, RootRelation> graph;

    public TerraformOrchestratorVisitor(TransformationContext context) {
        this.context = context;
        this.graph = context.getTopologyGraph();
    }

    @Override
    public void visit(RootComponent component) {

        /*
         * ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "dir");
         * pb.inheritIO(); pb.directory(context.getSubDirAccess().getTargetDirectory());
         * try { pb.start(); } catch (IOException e) { e.printStackTrace(); }
         */
    }

    @SneakyThrows
    @Override
    public void visit(Compute component) {
        Gson gson = new Gson();
        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        pb.directory(context.getSubDirAccess().getTargetDirectory());

        List<Artifact> providerInfo = component.getArtifacts().stream().filter(a -> a.getName().equals("provider"))
                .collect(Collectors.toList());

        // todo clean solution
        if (providerInfo.isEmpty()) {
            throw new IllegalArgumentException("The providerinfo for openstack was not provided");
        }

        String openstackProviderInfo = context.getSubDirAccess().readToString(providerInfo.iterator().next().getValue());

        HashMap<String, String> obj = gson.fromJson(openstackProviderInfo, HashMap.class);

        Map<String, String> env = pb.environment();

        for (String key : obj.keySet()) {
            env.put("TF_VAR_" + key, obj.get(key));
        }

        try {
            pb.command("terraform", "init");
            Process init = pb.start();
            init.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        pb.command("terraform", "apply", "-auto-approve", "-input=false");

        Process apply = pb.start();
        apply.waitFor();

        String computeInfo = context.getSubDirAccess().readToStringTargetDir(component.getName() + "_computed_properties" + ".json");
        HashMap<String, String> output = gson.fromJson(computeInfo, new TypeToken<HashMap<String, String>>() {
        }.getType());

        Map<String, Property> properties = component.getProperties();

        // set all properties
        // object is a map of maps
        for (var computed_prop : output.entrySet()) {

            if (!properties.containsKey(computed_prop.getKey())) {
                logger.warn(String.format("The property(%s) is not there, so it was added to props", computed_prop.getKey()));
                component.addProperty(computed_prop.getKey(), computed_prop.getValue());
            } else {
                var prop = properties.get(computed_prop.getKey());
                prop.setValue(computed_prop.getValue());
            }
        }


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
