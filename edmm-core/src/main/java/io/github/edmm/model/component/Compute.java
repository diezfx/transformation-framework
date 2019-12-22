package io.github.edmm.model.component;

import java.util.Optional;


import io.github.edmm.core.parser.Entity;
import io.github.edmm.core.parser.EntityId;
import io.github.edmm.core.parser.MappingEntity;
import io.github.edmm.model.support.Attribute;
import io.github.edmm.model.visitor.ComponentVisitor;
import lombok.ToString;

@ToString
public class Compute extends RootComponent {

    public static final Attribute<String> OS_FAMILY = new Attribute<>("os_family", String.class);
    public static final Attribute<String> MACHINE_IMAGE = new Attribute<>("machine_image", String.class);
    public static final Attribute<String> INSTANCE_TYPE = new Attribute<>("instance_type", String.class);
    public static final Attribute<String> KEY_NAME = new Attribute<>("key_name", String.class);
    public static final Attribute<String> PUBLIC_KEY = new Attribute<>("public_key", String.class);

    // computed stuff not known at compile time
    public static final Attribute<String> HOST_ADRESS = new Attribute<>("ip_adress", String.class);

    public Compute(MappingEntity mappingEntity) {

        super(mappingEntity);
        setPropertyValue(HOST_ADRESS, "newVal");
    }

    public Optional<String> getOsFamily() {
        return getProperty(OS_FAMILY);
    }

    public Optional<String> getMachineImage() {
        return getProperty(MACHINE_IMAGE);
    }

    public Optional<String> getInstanceType() {
        return getProperty(INSTANCE_TYPE);
    }

    public Optional<String> getKeyName() {
        return getProperty(KEY_NAME);
    }

    public Optional<String> getPublicKey() {
        return getProperty(PUBLIC_KEY);
    }

    public Optional<String> getHostAdress() {
        return getProperty(HOST_ADRESS);
    }

    // only needed in orchestrator phase
    public void setHostAdress(String adress) {
        this.set(HOST_ADRESS, adress);
    }

    @Override
    public void accept(ComponentVisitor v) {
        v.visit(this);
    }
}
