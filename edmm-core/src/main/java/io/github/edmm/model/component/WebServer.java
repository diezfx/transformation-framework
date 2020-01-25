package io.github.edmm.model.component;

import io.github.edmm.core.parser.MappingEntity;
import io.github.edmm.model.support.Attribute;
import io.github.edmm.model.visitor.ComponentVisitor;
import lombok.ToString;

import java.util.Optional;

@ToString
public class WebServer extends SoftwareComponent {

    public static final Attribute<Integer> PORT = new Attribute<>("port", Integer.class);

    public WebServer(MappingEntity mappingEntity) {
        super(mappingEntity);
    }

    public Optional<Integer> getPort() {
        return getProperty(PORT);
    }

    @Override
    public void accept(ComponentVisitor v) {
        v.visit(this);
    }
}
