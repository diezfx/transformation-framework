package io.github.edmm.plugins.multi.orchestration;

import freemarker.template.Configuration;
import io.github.edmm.core.plugin.TemplateHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.Artifact;
import io.github.edmm.model.component.*;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.plugins.multi.MultiLifecycle;
import io.github.edmm.plugins.multi.MultiPlugin;
import lombok.SneakyThrows;
import org.jgrapht.Graph;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.json.simple.parser.JSONParser;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "dir");
        pb.inheritIO();
        pb.directory(context.getSubDirAccess().getTargetDirectory());
        try {
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
*/
    }

    @SneakyThrows
    @Override
    public void visit(Compute component) {
        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        pb.directory(context.getSubDirAccess().getTargetDirectory());

        List<Artifact> providerInfo = component.getArtifacts().stream()
                .filter(a -> a.getName().equals("provider"))
                .collect(Collectors.toList());

        if (providerInfo.isEmpty()) {
            throw new IllegalArgumentException("The providerinfo for openstack was not provided");
        }
        JSONParser parser = new JSONParser();
        File openstackProviderInfo = new File(context.getSubDirAccess().getTargetDirectory(), providerInfo.iterator().next().getValue());
        try {
            JSONObject obj = (JSONObject) parser.parse(new FileReader(openstackProviderInfo));
            Map<String, String> env = pb.environment();

            for (Object key : obj.keySet()) {
                logger.info(key.toString());
                String keyString = (String) key;
                env.put("TF_VAR_" + keyString, (String) obj.get(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
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


        File computeInfo = new File(context.getSubDirAccess().getTargetDirectory(), "compute_"+component.getName()+".json");
        try {
            JSONObject obj = (JSONObject) parser.parse(new FileReader(computeInfo));
            String address=(String)obj.get("address");
            logger.info(address);
            component.setHostAddress(address);





        } catch (Exception e) {
            e.printStackTrace();
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
