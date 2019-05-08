package io.github.miwurster.edm.model.relation;

import io.github.miwurster.edm.core.parser.MappingEntity;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode(callSuper = true)
public class HostedOn extends RootRelationship {

    public HostedOn(MappingEntity entity) {
        super(entity);
    }
}
