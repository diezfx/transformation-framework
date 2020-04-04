package io.github.edmm.plugins.multi.orchestration;

import java.util.List;


public interface GroupVisitor {

   // todo components in context?
   void execute(List<DeploymentModelInfo> components);
}



