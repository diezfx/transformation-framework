package io.github.edmm.core.plugin.support;

import io.github.edmm.core.plugin.LifecyclePhase;

import java.util.List;

public interface LifecyclePhaseAccess {

    List<LifecyclePhase> getLifecyclePhases();
}
