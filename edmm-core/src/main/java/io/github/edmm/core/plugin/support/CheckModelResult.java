package io.github.edmm.core.plugin.support;

import io.github.edmm.model.component.RootComponent;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@NoArgsConstructor
public class CheckModelResult {

    private State state = State.PENDING;
    private List<RootComponent> unsupportedComponents = new ArrayList<>();

    public CheckModelResult(State state) {
        this.state = state;
    }

    public CheckModelResult(Collection<RootComponent> unsupportedComponents) {
        this.unsupportedComponents = new ArrayList<>(unsupportedComponents);
        if (unsupportedComponents.size() == 0) {
            this.state = State.OK;
        } else {
            this.state = State.UNSUPPORTED_COMPONENTS;
        }
    }

    public void addUnsupportedComponent(RootComponent component) {
        unsupportedComponents.add(component);
    }

    public enum State {
        PENDING,
        UNSUPPORTED_COMPONENTS,
        OK,
    }
}
