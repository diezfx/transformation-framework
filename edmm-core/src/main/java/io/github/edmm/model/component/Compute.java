package io.github.edmm.model.component;

import io.github.edmm.core.parser.MappingEntity;
import io.github.edmm.model.Property;
import io.github.edmm.model.relation.RootRelation;
import io.github.edmm.model.support.Attribute;
import io.github.edmm.model.visitor.ComponentVisitor;
import lombok.ToString;
import org.jgrapht.Graph;

import java.util.Optional;

@ToString
public class Compute extends RootComponent {

    public static final Attribute<String> OS_FAMILY = new Attribute<>("os_family", String.class);
    public static final Attribute<String> MACHINE_IMAGE = new Attribute<>("machine_image", String.class);
    public static final Attribute<String> INSTANCE_TYPE = new Attribute<>("instance_type", String.class);
    public static final Attribute<String> KEY_NAME = new Attribute<>("key_name", String.class);
    public static final Attribute<String> PUBLIC_KEY = new Attribute<>("public_key", String.class);

    public static final Attribute<String> PRIVATE_KEY = new Attribute<>("private_key", String.class);

    // computed stuff not known at compile time
    public static final Attribute<String> HOST_ADDRESS = new Attribute<>("address", String.class);

    public Compute(MappingEntity mappingEntity) {

        super(mappingEntity);
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

    public Optional<String> getPrivateKey() {
        return getProperty(PRIVATE_KEY);
    }

    public Optional<String> getHostAddress(Graph<RootComponent, RootRelation> graph) {

        return getProvidedProperty(HOST_ADDRESS.getName(), graph).map(Property::getValue);
    }

    // only needed in orchestrator phase
    public void setHostAddress(String address, Graph<RootComponent,RootRelation> graph) {
        setProvidedValue(HOST_ADDRESS.getName(),address, graph);
    }

    @Override
    public void accept(ComponentVisitor v) {
        v.visit(this);
    }


    public void setInterfaceValue(Attribute<String> attribute, String newVal) {
        Optional<Property> prop = getProperty(attribute.getName());

        if (!prop.isPresent()) {
            return;
        }
        prop.get().setValue(newVal);
    }


}
