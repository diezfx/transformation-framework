## Kubernetes Visitor



### transformation
`edmm-core/src/main/java/io/github/edmm/plugins/multi/KubernetesVisitor.java`

When the visitors' `visit()` is called, the first check is to see if this one is a "top" component. Which means that no further component is connected with `hosted_on`
If this is the case the whole stack is built.
All components are collected in the stack with hosted_on are collected. After that it's the same procedure as with the kubernetes plugin.
The only major difference is in properties that are not set at this moment are recognized and instead a reference to a configmap is made.
Furthermore chmod+x was added to the Dockerfile scripts to get rid of problems with permission denied.
     

    
2. later option(?):
  - create dockerfiles per component not only top?
  - secrets could be added to secretmap


### orchestrator
`edmm-core/src/main/java/io/github/edmm/plugins/multi/orchestration/KubernetesOrchestratorVisitor.java`


General info: All yamls are deployed with the kubernetes-client because fabric8 didn't work for me.

The idea is to only deploy when the top component of a stack is visited. Then the following steps are executed
>create configmap named \<componentname>-config.yaml, then deploy configmap

For the configmap all environment variables that are recognized to be from a connected component or only known during runtime
need to be known now. They are collected and added to this map.

>deploy deployment

Because we know that a `<componentname>-deployment.yaml` exists through the transformation step. We deploy this next.
All runtime references to the configmap are fulfilled now.

> deploy service

At last the service is deployed to expose ports.

> wait for it to come online

At the moment this is only implemented with waiting 20 seconds. Could be improved with polling the status.

> read runtime vars

After everything is deployed interesting variables can be read out. At the moment this is the clusterIP that is 
simulating some future external loadbalancer-hostname or something similar. This is injected in the model as `hostname

### special cases
  what if 2 components are deployed in same cluster -> check and use service name?
  
## future work
- add verification
At the moment something in the middle of a stack could be another deployment tech. Doesn't work with kubernetes.

