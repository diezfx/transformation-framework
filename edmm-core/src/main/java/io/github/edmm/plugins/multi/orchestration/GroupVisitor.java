package io.github.edmm.plugins.multi.orchestration;

import java.util.List;


public interface GroupVisitor {

   void visit(List<DeploymentModelInfo> components);
}



