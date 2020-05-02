package io.github.edmm.model.support;

import io.github.edmm.core.parser.Entity;
import io.github.edmm.core.parser.EntityGraph;
import io.github.edmm.core.parser.MappingEntity;
import io.github.edmm.core.parser.ScalarEntity;
import io.github.edmm.core.parser.support.DefaultKeys;
import io.github.edmm.core.parser.support.GraphHelper;
import io.github.edmm.model.Operation;
import io.github.edmm.model.Property;

import lombok.ToString;
import lombok.var;

import java.util.*;

@ToString
public abstract class ModelEntity extends DescribableElement {

    public static final Attribute<String> EXTENDS = new Attribute<>("extends", String.class);
    public static final Attribute<Property> PROPERTIES = new Attribute<>("properties", Property.class);
    public static final Attribute<Operation> OPERATIONS = new Attribute<>("operations", Operation.class);


    private boolean transformed = false;

    public ModelEntity(MappingEntity entity) {
        super(entity);
    }

    public Optional<String> getExtends() {
        return Optional.ofNullable(get(EXTENDS));
    }

    public Map<String, Property> getProperties() {
        EntityGraph graph = entity.getGraph();
        Map<String, Property> result = new HashMap<>();
        // Resolve the chain of types
        MappingEntity typeRef = GraphHelper.findTypeEntity(graph, entity).
            orElseThrow(() -> new IllegalStateException("A component must be an instance of an existing type"));
        List<MappingEntity> typeChain = GraphHelper.resolveInheritanceChain(graph, typeRef);
        // Get initial properties by assignments
        Optional<Entity> propertiesEntity = entity.getChild(PROPERTIES);
        propertiesEntity.ifPresent(value -> populateProperties(result, value));
        // Update current map by property definitions
        for (MappingEntity typeEntity : typeChain) {
            propertiesEntity = typeEntity.getChild(PROPERTIES.getName());
            propertiesEntity.ifPresent(value -> populateProperties(result, value));
        }
        return result;
    }


    public void addProperty(String name, String prop_value) {
        EntityGraph graph = entity.getGraph();
        var props = entity.getChild(PROPERTIES);
        MappingEntity propertyEnt;
        if (!props.isPresent()) {
            propertyEnt = new MappingEntity(entity.getId().extend(PROPERTIES.getName()), graph);
            graph.addEntity(propertyEnt);
        } else {
            propertyEnt = (MappingEntity) props.get();
        }
        var id = propertyEnt.getId().extend(name);


        MappingEntity propertyEntity = new MappingEntity(id, graph);
        ScalarEntity type = new ScalarEntity(DefaultKeys.STRING, propertyEntity.getId().extend(DefaultKeys.TYPE), graph);
        ScalarEntity value = new ScalarEntity(prop_value, propertyEntity.getId().extend(DefaultKeys.VALUE), graph);
        ScalarEntity computed = new ScalarEntity("true", propertyEntity.getId().extend(DefaultKeys.COMPUTED), graph);

        //check if entity already exists
        if (propertyEnt.getChild(name).isPresent()) {
            var old_value = propertyEnt.getChild(name).get().getChild(DefaultKeys.VALUE).get();
            var old_type = propertyEnt.getChild(name).get().getChild(DefaultKeys.TYPE).get();
            graph.replaceEntity(old_value, value);
            graph.replaceEntity(old_type, type);

        } else {
            graph.addEntity(propertyEntity);
            graph.addEntity(type);
            graph.addEntity(value);
        }
        graph.addEntity(computed);
    }

    public Optional<Property> getProperty(String name) {
        return Optional.ofNullable(getProperties().get(name));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getProperty(Attribute<T> attribute) {
        Class<T> targetType = attribute.getType();
        Optional<Property> property = getProperty(attribute.getName());
        if (!property.isPresent()) {
            return Optional.empty();
        }
        if (String.class.isAssignableFrom(targetType)) {
            return (Optional<T>) property.map(Property::getValue);
        } else if (Integer.class.isAssignableFrom(targetType)) {
            return (Optional<T>) Optional.of(Integer.valueOf(property.get().getValue()));
        } else if (Boolean.class.isAssignableFrom(targetType)) {
            return (Optional<T>) Optional.of(Boolean.valueOf(property.get().getValue()));
        } else {
            throw new IllegalStateException(String.format("Cannot get value of type '%s' from attribute '%s'", targetType, attribute));
        }
    }

    public void setPropertyValue(Attribute<String> attribute, String newVal) {
        Optional<Property> prop = getProperty(attribute.getName());

        if (!prop.isPresent()) {
            return;
        }
        prop.get().setValue(newVal);
    }



    public Map<String, Operation> getOperations() {
        EntityGraph graph = entity.getGraph();
        Map<String, Operation> result = new HashMap<>();
        // Resolve the chain of types
        MappingEntity typeRef = GraphHelper.findTypeEntity(graph, entity).
            orElseThrow(() -> new IllegalStateException("A component must be an instance of an existing type"));
        List<MappingEntity> typeChain = GraphHelper.resolveInheritanceChain(graph, typeRef);
        // Get initial operations by component assignment
        Optional<Entity> operationsEntity = entity.getChild(OPERATIONS);
        operationsEntity.ifPresent(value -> populateOperations(result, value));
        // Update current map by property definitions
        for (MappingEntity typeEntity : typeChain) {
            operationsEntity = typeEntity.getChild(OPERATIONS.getName());
            operationsEntity.ifPresent(value -> populateOperations(result, value));
        }
        return result;
    }

    public Optional<Operation> getOperation(String name) {
        return Optional.ofNullable(getOperations().get(name));
    }

    public boolean isTransformed() {
        return transformed;
    }

    public void setTransformed(boolean transformed) {
        this.transformed = transformed;
    }


    protected void populateProperties(Map<String, Property> result, Entity entity) {
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


    private void populateOperations(Map<String, Operation> result, Entity entity) {
        Set<Entity> children = entity.getChildren();
        for (Entity child : children) {
            MappingEntity operationEntity = (MappingEntity) child;
            if (result.get(operationEntity.getName()) == null) {
                Operation operation = new Operation(operationEntity, this.entity);
                result.put(operation.getName(), operation);
            } else {
                result.get(operationEntity.getName())
                    .updateEntityChain(operationEntity);
            }
        }
    }
}
