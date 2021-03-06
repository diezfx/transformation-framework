package io.github.edmm.core.transformation;

import io.github.edmm.core.plugin.Plugin;
import io.github.edmm.core.plugin.PluginService;
import io.github.edmm.core.transformation.support.ExecutionTask;
import io.github.edmm.model.DeploymentModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TransformationService {

    private static final Logger logger = LoggerFactory.getLogger(TransformationService.class);

    private final PluginService pluginService;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Autowired
    public TransformationService(PluginService pluginService) {
        this.pluginService = pluginService;
    }

    public void startTransformation(TransformationContext context) {
        TargetTechnology targetTechnology = context.getTargetTechnology();
        Optional<Plugin<?>> plugin = pluginService.findByTargetTechnology(targetTechnology);
        if (!plugin.isPresent()) {
            logger.error("Plugin for given technology '{}' could not be found", targetTechnology.getId());
            return;
        }
        if (context.getState() == TransformationContext.State.READY) {
            try {
                executor.submit(new ExecutionTask(plugin.get(), context)).get();
            } catch (Exception e) {
                logger.error("Error executing transformation task", e);
            }
        }
    }

    public TransformationContext createContext(DeploymentModel model, String target, File sourceDirectory, File targetDirectory) {
        TargetTechnology targetTechnology = pluginService.getSupportedTargetTechnologies().stream()
            .filter(p -> p.getId().equals(target))
            .findFirst()
            .orElseThrow(IllegalStateException::new);
        return new TransformationContext(model, targetTechnology, sourceDirectory, targetDirectory);
    }
}
