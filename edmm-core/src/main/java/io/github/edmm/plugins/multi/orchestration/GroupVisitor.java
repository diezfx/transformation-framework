package io.github.edmm.plugins.multi.orchestration;

import io.github.edmm.model.component.RootComponent;

import java.util.List;
import java.util.Set;

public interface GroupVisitor {

   void visit(List<RootComponent> set);
}
