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
import io.github.edmm.model.PropertyBlocks;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.support.Attribute;
import io.github.edmm.model.support.ModelEntity;
import io.github.edmm.model.support.TypeWrapper;
import io.github.edmm.model.visitor.ComponentVisitor;
import io.github.edmm.model.visitor.VisitableComponent;
import lombok.ToString;
import lombok.var;
import org.jgrapht.Graph;

import java.util.*;

@ToString
public class RootComponent extends ModelEntity implements VisitableComponent {

    public static final Attribute<String> TYPE = new Attribute<>("type", String.class);
    public static final Attribute<RootRelation> RELATIONS = new Attribute<>("relations", RootRelation.class);


    //interface stuff
    public static final Attribute<Property> REQUIRES = new Attribute<>("requires", Property.class);
    public static final Attribute<Property> CAPABILITIES = new Attribute<>("capabilities", Property.class);
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

    public PropertyBlocks getRequirements() {

        Map<String, Map<String, Property>> result = new HashMap<>();
        // Resolve the chain of types
        Optional<Entity> propertiesEntity = entity.getChild(COMPONENT_INTERFACE).flatMap(i -> i.getChild(REQUIRES));

        propertiesEntity.ifPresent(value -> value.getChildren().forEach(block -> {
            Map<String, Property> requiredBlock = new HashMap<>();

            populateProperties(requiredBlock, block);
            result.put(block.getName(), requiredBlock);
        }));
        // Update current map by property definitions

        for (MappingEntity typeEntity : getTypeChain()) {
            propertiesEntity = typeEntity.getChild(COMPONENT_INTERFACE.getName())
                    .flatMap(i -> i.getChild(REQUIRES.getName()));
            propertiesEntity.ifPresent(value -> value.getChildren().forEach(block -> {
                Map<String, Property> requiredBlock = new HashMap<>();
                populateProperties(requiredBlock, block);
                result.put(block.getName(), requiredBlock);
            }));
        }
        return new PropertyBlocks(result);
    }



    /**
     * get blocks from the own component; after that fill if not already set with properties from parent components(inheritance)
     *
     * @return a list of all capabilties ordered by block; blocks are not relevant for the matching atm
     */
    public PropertyBlocks getCapabilities() {
        Map<String, Map<String, Property>> result = new HashMap<>();
        // Resolve the chain of types


        // Get initial properties by assignments
        Optional<Entity> propertiesEntity = entity.getChild(COMPONENT_INTERFACE)
                .flatMap(i -> i.getChild(CAPABILITIES));

        if (propertiesEntity.isPresent()) {
            for (Entity block : propertiesEntity.get().getChildren()) {
                Map<String, Property> blockList = new HashMap<>();

                populateProperties(blockList, block);
                result.put(block.getName(), blockList);
            }
        }

        for (MappingEntity typeEntity : getTypeChain()) {
            propertiesEntity = typeEntity.getChild(COMPONENT_INTERFACE.getName())
                    .flatMap(i -> i.getChild(CAPABILITIES));
            if (!propertiesEntity.isPresent()) {
                continue;
            }

            Map<String, Property> blockList = new HashMap<>();
            for (Entity blockEnt : propertiesEntity.get().getChildren()) {

                populateProperties(blockList, blockEnt);
                result.putIfAbsent(blockEnt.getName(), blockList);

                var newBlock = result.get(blockEnt.getName());
                blockList.forEach(newBlock::putIfAbsent);

            }
        }

        return new PropertyBlocks(result);

    }




    public List<MappingEntity> getTypeChain() {
        EntityGraph graph = entity.getGraph();
        MappingEntity typeRef = GraphHelper.findTypeEntity(graph, entity).
                orElseThrow(() -> new IllegalStateException("A component must be an instance of an existing type"));

        return GraphHelper.resolveInheritanceChain(graph, typeRef);

    }

    /**
     * finds the given capability but only on the own component; no search through dependency
     * @param name
     * @return
     */
    public Optional<Property> getCapabilityByName(String name) {


        PropertyBlocks blocks = getCapabilities();

        var prop = blocks.getPropertyByName(name);
        if (prop.isPresent()) {
            return prop;
        }

        return Optional.empty();
    }


     /**
     * finds the given capability but only on the own component; no search through dependency
     * @param name
     * @return
     */
    public Optional<Property> getCapabilityByType(String type) {


        PropertyBlocks blocks = getCapabilities();

        var prop = blocks.getPropertyByType(type);
        if (prop.isPresent()) {
            return prop;
        }

        return Optional.empty();
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
