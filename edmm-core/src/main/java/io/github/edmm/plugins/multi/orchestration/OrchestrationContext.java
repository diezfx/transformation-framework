package io.github.edmm.plugins.multi.orchestration;

import io.github.edmm.core.plugin.PluginFileAccess;

import java.io.File;
import java.util.List;

public class OrchestrationContext {

    private final File directory;
    private final List<DeploymentModelInfo> deployInfos;

    public OrchestrationContext(File directory,List<DeploymentModelInfo> deployInfos ){
        this.directory=directory;
        this.deployInfos=deployInfos;
    }

    public File getDirAccess() {
      return directory;
    }

    public List<DeploymentModelInfo> getDeployInfos() {
        return deployInfos;
    }
}
