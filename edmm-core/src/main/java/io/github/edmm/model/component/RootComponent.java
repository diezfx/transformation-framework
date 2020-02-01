package io.github.edmm.model.component;

import com.google.common.collect.Lists;
import io.github.edmm.core.parser.Entity;
import io.github.edmm.core.parser.EntityGraph;
import io.github.edmm.core.parser.MappingEntity;
import io.github.edmm.core.parser.support.GraphHelper;
import io.github.edmm.core.plugin.TopologyGraphHelper;
import io.github.edmm.model.ComponentInterface;
import io.github.edmm.model.Operation;
import io.github.edmm.model.Property;
import io.github.edmm.model.RequiredProperties;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.support.Attribute;
import io.github.edmm.model.support.ModelEntity;
import io.github.edmm.model.support.TypeWrapper;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.model.visitor.VisitableComponent;
import lombok.ToString;
import org.jgrapht.Graph;

import java.util.*;

@ToString
public class RootComponent extends ModelEntity implements VisitableComponent {

    public static final Attribute<String> TYPE = new Attribute<>("type", String.class);
    public static final Attribute<RootRelation> RELATIONS = new Attribute<>("relations", RootRelation.class);


    //interface stuff
    public static final Attribute<Property> REQUIRES = new Attribute<>("requires", Property.class);
    public static final Attribute<Property> PROVIDES = new Attribute<>("provides", Property.class);
    public static final Attribute<ComponentInterface> COMPONENT_INTERFACE = new Attribute<>("interface", ComponentInterface.class);

    private final List<RootRelation> relationCache = new ArrayList<>();

    public RootComponent(MappingEntity mappingEntity) {
        super(mappingEntity);
        // Resolve the chain of types
        EntityGraph graph = entity.getGraph();
        MappingEntity typeRef = GraphHelper.findTypeEntity(graph, entity).
                orElseThrow(() -> new IllegalStateException("A component must be an instance of an existing type"));
        List<MappingEntity> typeChain = GraphHelper.resolveInheritanceChain(graph, typeRef);
        typeChain.forEach(this::updateEntityChain);
    }


    public String getType() {
        return get(TYPE);
    }

    public List<RootRelation> getRelations() {
        if (relationCache.isEmpty()) {
            List<RootRelation> result = new ArrayList<>();
            Optional<Entity> artifactsEntity = getEntity().getChild(RELATIONS);
            artifactsEntity.ifPresent(value -> populateRelations(result, value));
            relationCache.addAll(Lists.reverse(result));
        }
        return relationCache;
    }


    public boolean hasRelations() {
        return getRelations().size() > 0;
    }

    public boolean hasOperations() {
        return getOperations().size() > 0;
    }

    public StandardLifecycle getStandardLifecycle() {
        return new StandardLifecycle(getOperations());
    }

    private void populateRelations(List<RootRelation> result, Entity entity) {
        Set<Entity> children = entity.getChildren();
        for (Entity child : children) {
            MappingEntity relationEntity = (MappingEntity) child;
            RootRelation relation = TypeWrapper.wrapRelation(relationEntity, this.entity);
            result.add(relation);
        }
    }

    private RequiredProperties getRequired() {

        Map<String, Map<String, Property>> result = new HashMap<>();
        // Resolve the chain of types
        Optional<Entity> propertiesEntity = entity.getChild(COMPONENT_INTERFACE).flatMap(i -> i.getChild(REQUIRES));

        propertiesEntity.ifPresent(value -> {

            value.getChildren().forEach(block -> {
                Map<String, Property> requiredBlock = new HashMap<>();

                populateProperties(requiredBlock, block);
                result.put(block.getName(), requiredBlock);
            });

        });
        // Update current map by property definitions

        for (MappingEntity typeEntity : getTypeChain()) {
            propertiesEntity = typeEntity.getChild(COMPONENT_INTERFACE.getName())
                    .flatMap(i -> i.getChild(REQUIRES.getName()));
            propertiesEntity.ifPresent(value -> {

                value.getChildren().forEach(block -> {
                    Map<String, Property> requiredBlock = new HashMap<>();
                    populateProperties(requiredBlock, block);
                    result.put(block.getName(), requiredBlock);
                });

            });
        }
        return new RequiredProperties(result);
    }

    public void setProvidedValue(String attribute, String newVal, Graph<RootComponent, RootRelation> graph) {
        Optional<Property> prop = getProvidedProperty(attribute, graph);

        prop.ifPresent(p -> p.setValue(newVal));
    }

    public Optional<ComponentInterface> getInterface(Graph<RootComponent, RootRelation> graph) {

        return Optional.of(new ComponentInterface(getProvided(), getRequired(), getProvidedHostingProps(graph)));
    }


    private Map<String, Property> getProvidedHostingProps(Graph<RootComponent, RootRelation> graph) {
        Map<String, Property> properties = new HashMap<>();

        Optional<RootComponent> host = TopologyGraphHelper.resolveHostingComponent(graph, this);
        while (host.isPresent()) {
            host.get().getProvided().forEach(properties::putIfAbsent);
            host = TopologyGraphHelper.resolveHostingComponent(graph, host.get());
        }

        return properties;

    }

    private Map<String, Property> getProvided() {
        Map<String, Property> result = new HashMap<>();
        // Resolve the chain of types

        // Get initial properties by assignments
        Optional<Entity> propertiesEntity = entity.getChild(COMPONENT_INTERFACE).flatMap(i -> i.getChild(PROVIDES));
        propertiesEntity.ifPresent(value -> populateProperties(result, value));
        // Update current map by property definitions

        for (MappingEntity typeEntity : getTypeChain()) {
            propertiesEntity = typeEntity.getChild(COMPONENT_INTERFACE.getName())
                    .flatMap(i -> i.getChild(PROVIDES.getName()));
            propertiesEntity.ifPresent(value -> populateProperties(result, value));
        }
        return result;

    }

    public List<MappingEntity> getTypeChain() {
        EntityGraph graph = entity.getGraph();
        MappingEntity typeRef = GraphHelper.findTypeEntity(graph, entity).
                orElseThrow(() -> new IllegalStateException("A component must be an instance of an existing type"));

        return GraphHelper.resolveInheritanceChain(graph, typeRef);

    }

    public Optional<Property> getProvidedProperty(String name, Graph<RootComponent, RootRelation> graph) {
        return getInterface(graph).map(ComponentInterface::getAllProvided).map(reqs -> reqs.get(name));
    }

    public Optional<Property> getRequiredProperty(String block, String name, Graph<RootComponent, RootRelation> graph) {
        return getInterface(graph).map(i -> i.getRequires().getRequires().get(block)).map(reqs -> reqs.get(name));
    }

    @Override
    public void accept(ComponentVisitor v) {
        v.visit(this);
    }

    @ToString
    public static class StandardLifecycle {

        private final Map<String, Operation> operations;

        public StandardLifecycle(Map<String, Operation> operations) {
            this.operations = operations;
        }

        public Optional<Operation> getCreate() {
            return Optional.ofNullable(operations.get("create"));
        }

        public Optional<Operation> getConfigure() {
            return Optional.ofNullable(operations.get("configure"));
        }

        public Optional<Operation> getStart() {
            return Optional.ofNullable(operations.get("start"));
        }

        public Optional<Operation> getStop() {
            return Optional.ofNullable(operations.get("stop"));
        }

        public Optional<Operation> getDelete() {
            return Optional.ofNullable(operations.get("delete"));
        }
    }
}
