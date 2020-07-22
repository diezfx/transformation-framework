package io.github.edmm.plugins;

import io.github.edmm.core.plugin.support.CheckModelResult;
import io.github.edmm.core.transformation.TransformationContext;
import io.github.edmm.model.component.*;
import io.github.edmm.model.visitor.ComponentVisitor;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ComputeAreaSupportVisitor implements ComponentVisitor {

    protected final TransformationContext context;

    @Getter
    protected Set<RootComponent> supportedComponents = new HashSet<>();

    public ComputeAreaSupportVisitor(TransformationContext context) {
        this.context = context;
    }

    public CheckModelResult getResult() {
        List<RootComponent> unsupportedComponents = context.getGroup().getGroupComponents().stream()
            .filter(c -> !supportedComponents.contains(c))
            .collect(Collectors.toList());
        return new CheckModelResult(unsupportedComponents);
    }

    @Override
    public void visit(Compute component) {
        supportedComponents.add(component);
    }

    @Override
    public void visit(MysqlDatabase component) {
        supportedComponents.add(component);
    }

    @Override
    public void visit(MysqlDbms component) {
        supportedComponents.add(component);
    }

    @Override
    public void visit(Tomcat component) {
        supportedComponents.add(component);
    }

    @Override
    public void visit(WebApplication component) {
        supportedComponents.add(component);
    }

    @Override
    public void visit(Database component) {
        supportedComponents.add(component);
    }

    @Override
    public void visit(Dbms component) {
        supportedComponents.add(component);
    }

    @Override
    public void visit(SoftwareComponent component) {
        supportedComponents.add(component);
    }

    @Override
    public void visit(WebServer component) {
        supportedComponents.add(component);
    }
}
