package io.github.edmm.plugins.multi.support.kubernetes;

import io.github.edmm.core.plugin.PluginFileAccess;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.core.transformation.TransformationException;
import io.github.edmm.docker.Container;
import io.github.edmm.docker.DependencyGraph;
import io.github.edmm.model.relation.ConnectsTo;
import io.github.edmm.plugins.kubernetes.model.DeploymentResource;
import io.github.edmm.plugins.kubernetes.model.KubernetesResource;
import io.github.edmm.plugins.kubernetes.model.ServiceResource;
import lombok.var;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.Property;
import io.github.edmm.model.PropertyBlocks;
import io.github.edmm.model.component.*;
import org.jgrapht.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class KubernetesResourceBuilder {

    private static final Logger logger = LoggerFactory.getLogger(DockerfileBuildingVisitor.class);

    private final List<KubernetesResource> resources = new ArrayList<>();

    private final Container stack;
    private final RootComponent topComp;
    private final Graph<RootComponent, RootRelation> dependencyGraph;
    private final PluginFileAccess fileAccess;

    public KubernetesResourceBuilder(Container stack, RootComponent topComp, Graph<RootComponent, RootRelation> dependencyGraph, PluginFileAccess fileAccess) {
        this.stack = stack;
        this.dependencyGraph = dependencyGraph;
        this.fileAccess = fileAccess;
        this.topComp = topComp;
    }

    public void populateResources() {
        resolveEnvVars();
        resources.add(new DeploymentResource(stack));
        if (stack.getPorts().size() > 0) {
            resources.add(new ServiceResource(stack));
        }
        resources.forEach(KubernetesResource::build);
        try {
            String targetDirectory = stack.getName();
            for (KubernetesResource resource : resources) {
                fileAccess.write(targetDirectory + "/" + resource.getName() + ".yaml", resource.toYaml());
            }
        } catch (Exception e) {
            logger.error("Failed to create Kubernetes resource files for stack '{}'", stack.getName(), e);
            throw new TransformationException(e);
        }
    }

    // build time stuff now
    // todo how to solve when both in same cluster?
    // todo flatten to env variables?
    private void resolveEnvVars() {
        Set<RootComponent> targets = TopologyGraphHelper.getTargetComponents(dependencyGraph, topComp, ConnectsTo.class);
        /*??
        for (RootComponent target : targets) {
            
            
            for (Map.Entry<String, String> envVar : target.getEnvVars().entrySet()) {
                stack.addEnvVar(envVar.getKey(), envVar.getValue());
            }
            
            stack.addEnvVar((target.getName() + "_HOSTNAME").toUpperCase(), target.getServiceName());
        }
        */


        PropertyBlocks reqs = topComp.getRequirements();
        //kubernetes ignores host for now
        for (var prop : reqs.flattenBlocks().entrySet()) {
            if (prop.getKey().startsWith("host")) {
                continue;
            }
            stack.addEnvVarRuntime(prop.getKey());
        }
    }


}
