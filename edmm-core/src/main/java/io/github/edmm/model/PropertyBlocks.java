package io.github.edmm.model;

import com.google.gson.JsonObject;
import lombok.Data;
import lombok.NonNull;
import lombok.ToString;
import lombok.var;
import org.apache.commons.lang3.tuple.Pair;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Data
@ToString
public class PropertyBlocks {

    @NonNull Map<String, Map<String, Property>> blocks;


    public Optional<Map<String, Property>> getBlockByName(String name) {
        return Optional.of(blocks.get(name));
    }

    /**
     * @param name the name of the property
     * @return looks through all blocks and their property; the first one with the specified name is returned
     */
    public Optional<Property> getPropertyByName(String name) {

        for (var block : blocks.values()) {
            for (var prop : block.entrySet()) {
                if (prop.getKey().equals(name)) {
                    return Optional.of(prop.getValue());
                }
            }
        }

        return Optional.empty();
    }

    /**
     * @param name the name of the property
     * @return looks through all blocks and their property; the first one with the specified name is returned
     */
    public Optional<Property> getPropertyByType(String type) {

        for (var block : blocks.values()) {
            for (var prop : block.entrySet()) {
                if (prop.getValue().getType().startsWith(type)) {
                    return Optional.of(prop.getValue());
                }
            }
        }

        return Optional.empty();
    }

    /**
     * @param name the name of the property
     * @return looks through all blocks and their property; the first one with the specified name is returned
     */
    public Optional<Pair<String, Property>> getPropertyByTypeWithBlockName(String type) {

        for (var block : blocks.entrySet()) {
            for (var prop : block.getValue().entrySet()) {
                if (prop.getValue().getType().equals(type)) {
                    return Optional.of(Pair.of(block.getKey() + "_" + prop.getKey(), prop.getValue()));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * flattens the propertyblocks so that only one array exists
     * the block becomes the first part of the name
     * <block>-<property-name>
     */
    public Map<String, Property> flattenBlocks() {
        HashMap<String, Property> result = new HashMap<>();
        for (var block : blocks.entrySet()) {
            for (var prop : block.getValue().entrySet()) {
                String name = block.getKey() + "_" + prop.getKey();
                if (result.containsKey(name)) {
                    throw new IllegalArgumentException(String.format("the key {} is used twice", name));
                }
                result.put(name, prop.getValue());
            }

        }
        return result;

    }


    public Optional<Property> getProperty(String blockName, String propName) {

        if (!blocks.containsKey(blockName)) {
            return Optional.empty();
        }
        var block = blocks.get(blockName);

        for (var prop : block.entrySet()) {
            if (prop.getKey().equals(propName)) {
                return Optional.of(prop.getValue());
            }
        }

        return Optional.empty();

    }

    /**
     * merge blocks and prefer the own ones
     * @return
     */
    public PropertyBlocks mergeBlocks(PropertyBlocks blocks2){
        Map<String, Map<String, Property>> result=this.blocks;

        for(var blockEntry : blocks2.getBlocks().entrySet()){

            result.putIfAbsent(blockEntry.getKey(),new HashMap<>());

            var blockNew = result.get(blockEntry.getKey());
            for (var prob : blockEntry.getValue().entrySet()){
                    blockNew.putIfAbsent(prob.getKey(),prob.getValue());
            }

        }
        return new PropertyBlocks(result);

    }

    public void addBlock(String key,Map<String,Property> block ){
        blocks.putIfAbsent(key,block);
    }

    public JsonObject toJson(){
        JsonObject result= new JsonObject();

        for (var block : blocks.entrySet()){
            JsonObject blockJson= new JsonObject();
            for(var prop : block.getValue().entrySet()){

                blockJson.addProperty(prop.getKey(),prop.getValue().getValue());

            }
            result.add(block.getKey(),blockJson);
        }
        return result;
    }
}
