package io.github.edmm.plugins.multi;

import io.github.edmm.core.plugin.Plugin;
import io.github.edmm.core.transformation.Platform;
import io.github.edmm.core.transformation.TransformationContext;

public class MultiPlugin extends Plugin<MultiLifecycle> {

    public static final Platform MULTI = Platform.builder().id("multi").name("Mixed Deployment").build();

    public MultiPlugin() {
        super(MULTI);
    }

    @Override
    public MultiLifecycle getLifecycle(TransformationContext context) {
        return new MultiLifecycle(context);
    }
}
