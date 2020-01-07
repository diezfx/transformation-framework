package io.github.edmm.plugins.multi.orchestration;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import com.google.gson.JsonObject;

import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Configuration;
import io.github.edmm.core.plugin.TemplateHelper;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.component.Compute;
import io.github.edmm.model.component.Database;
import io.github.edmm.model.component.Dbms;
import io.github.edmm.model.component.MysqlDatabase;
import io.github.edmm.model.component.MysqlDbms;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.component.Tomcat;
import io.github.edmm.model.component.WebApplication;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.plugins.multi.MultiPlugin;

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

        Optional<Compute> computeSource = TopologyGraphHelper.resolveHostingComputeComponent(graph, component);

        JsonObject connection = new JsonObject();
        connection.addProperty("address", computeSource.get().getHostAddress().get());

        try {
            context.getSubDirAccess().write("compute_" + computeSource.get().getNormalizedName() + ".json", connection.toString());
            pb.command("ansible-playbook", component.getNormalizedName() + ".yml");

            Process apply = pb.start();
            apply.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        File computeInfo = new File(context.getSubDirAccess().getTargetDirectory(), "compute_" + component.getName() + ".json");
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




