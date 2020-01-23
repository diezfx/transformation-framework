package io.github.edmm.model;

import io.github.edmm.core.parser.Entity;
import io.github.edmm.core.parser.EntityGraph;
import io.github.edmm.core.parser.MappingEntity;
import io.github.edmm.core.parser.support.GraphHelper;
import io.github.edmm.model.support.Attribute;
import io.github.edmm.model.support.DescribableElement;

import java.util.*;

public class ComponentInterface extends DescribableElement {

    public static final Attribute<Property> REQUIRES= new Attribute<>("requires", Property.class);
    public static final Attribute<Property> PROVIDES= new Attribute<>("provides", Property.class);


    public ComponentInterface(MappingEntity entity) {
        super(entity);
    }


    public Map<String, Property> getRequired() {

        EntityGraph graph = entity.getGraph();
        Map<String, Property> result = new HashMap<>();
        // Resolve the chain of types
        Optional<Entity> propertiesEntity = entity.getChild(REQUIRES);
        propertiesEntity.ifPresent(value -> populateProperties(result, value));
        // Update current map by property definitions
        for (MappingEntity typeEntity : getParentTypeChain()) {
            propertiesEntity = typeEntity.getChild(REQUIRES.getName());
            propertiesEntity.ifPresent(value -> populateProperties(result, value));
        }
        return result;
    }

    public Map<String, Property> getProvided() {
        EntityGraph graph = entity.getGraph();
        Map<String, Property> result = new HashMap<>();
        // Resolve the chain of types

        // Get initial properties by assignments
        Optional<Entity> propertiesEntity = entity.getChild(PROVIDES);
        propertiesEntity.ifPresent(value -> populateProperties(result, value));
        // Update current map by property definitions

        for (MappingEntity typeEntity : getParentTypeChain()) {
            propertiesEntity = typeEntity.getChild(PROVIDES.getName());
            propertiesEntity.ifPresent(value -> populateProperties(result, value));
        }

        return result;

    }

    protected List<MappingEntity> getParentTypeChain() {
        EntityGraph graph = entity.getGraph();
        MappingEntity typeRef = GraphHelper.findTypeEntity(graph,(MappingEntity) entity.getParent().get()).
                orElseThrow(() -> new IllegalStateException("A component must be an instance of an existing type"));

        return GraphHelper.resolveInheritanceChain(graph, typeRef);

    }


    private void populateProperties(Map<String, Property> result, Entity entity) {
        Set<Entity> children = entity.getChildren();
        for (Entity child : children) {
            MappingEntity propertyEntity = (MappingEntity) child;
            if (result.get(propertyEntity.getName()) == null) {
                Property property = new Property(propertyEntity, this.entity);
                result.put(property.getName(), property);
            } else {
                result.get(propertyEntity.getName())
                        .updateEntityChain(propertyEntity);
            }
        }
    }




}
