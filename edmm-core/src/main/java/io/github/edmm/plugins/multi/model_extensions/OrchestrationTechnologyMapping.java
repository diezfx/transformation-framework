package io.github.edmm.plugins.multi.model_extensions;

import io.github.edmm.core.parser.Entity;
import io.github.edmm.core.parser.EntityId;
import io.github.edmm.core.parser.MappingEntity;
import io.github.edmm.core.parser.ScalarEntity;
import io.github.edmm.core.parser.support.GraphHelper;
import io.github.edmm.model.component.RootComponent;
import io.github.edmm.model.support.Attribute;
import io.github.edmm.model.support.BaseElement;
import io.github.edmm.plugins.multi.Technology;
import lombok.var;
import org.springframework.scheduling.concurrent.ScheduledExecutorTask;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

public class OrchestrationTechnologyMapping extends BaseElement {

    private Set<RootComponent> components;

    // todo find solution without this
    public OrchestrationTechnologyMapping(MappingEntity entity) {
        super(entity);
    }

    public OrchestrationTechnologyMapping(MappingEntity entity, Set<RootComponent> components) {
        super(entity);
        this.components = components;

    }

    public Optional<Set<RootComponent>> getListForTechnology(Technology tech) {
        Optional<Entity> otechList = entity.getChildren().stream()
                .filter(child -> child.getName().equals(tech.name().toLowerCase())).findFirst();

        if (!otechList.isPresent())
            return Optional.empty();
        Set<ScalarEntity> compTechList = otechList.get().getChildren().stream().map(c -> (ScalarEntity) c)
                .collect(Collectors.toSet());

        Set<RootComponent> referencedComps = new HashSet<>();

        // check which component is referenced
        for (ScalarEntity compEntity : compTechList) {
            Optional<RootComponent> referencedComp = components.stream()
                    .filter(c -> c.getName().equals(compEntity.getValue())).findFirst();

            referencedComp.ifPresentOrElse(referencedComps::add, () -> {
                throw new IllegalArgumentException(
                        String.format("the given component(%s) is not in the model", (compEntity.getValue())));
            });
        }
        return Optional.of(referencedComps);
    }

    public Map<RootComponent, Technology> getTechForComponents() {

        Map<RootComponent, Technology> result = new HashMap<>();

        for (var tech : EnumSet.allOf(Technology.class)) {
            getListForTechnology(tech).ifPresent(t -> t.forEach(c -> result.put(c, tech)));

        }

        return result;
    }

}
