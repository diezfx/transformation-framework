package io.github.edmm.plugins.multi.orchestration;

import com.google.gson.JsonObject;
import freemarker.template.Configuration;
import io.github.edmm.core.plugin.TemplateHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.Property;
import io.github.edmm.model.component.*;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.plugins.multi.MultiPlugin;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

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

        JsonObject requiredProps = new JsonObject();


        // look for host requirements; special case
        component.getInterface(context.getTopologyGraph()).ifPresent(i -> {
            JsonObject hostR= new JsonObject();
            Map<String,Property> hostRequirements=i.getRequires().getHostingRequirements();

            hostRequirements.forEach((prop_name, prop_value) -> {
                    //todo error checking if required stuff is not there
                    Optional<String> prop = component.getProvidedProperty(prop_name, context.getTopologyGraph()).map(Property::getValue);
                    prop.ifPresent(s -> hostR.addProperty(prop_name, s));
                });
            requiredProps.add("host",hostR);
        });


        // look for all other requirements through relations



        try {
            context.getSubDirAccess().write(component.getNormalizedName() + ".json", requiredProps.toString());
            pb.command("ansible-playbook", component.getNormalizedName() + ".yml");

            Process apply = pb.start();
            apply.waitFor();
        } catch (IOException | InterruptedException e) {
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




