# EDMM Transformation and Deployment Framework

> Transformation framework for the Essential Deployment Metamodel.

## Example

```shell
edmm transform multi icsoc-demo/deployment-full.yaml
```

The generated Kubernetes resource files are located relative to the `edmm_model.yml` file inside a `multi` directory.
This will create a stack for the petclinic demo application inclusive the underlying tomcat. 
Because kubernetes is chosen as the technology a configmap-,service- and deployment-file is added.
For the second "stack" first a openstack compuite instance is deployed. Then the software is installed with ansible.
For this to work the openstack-provider infos have to be provided.

### infos
- compute instance deployed with terraform needs a provider.json
- localhost:32000 is hardcoded as image repo
- default kubernetes context is used

## Usage
The transformation can be started by using the `transform` command of the `edmm` tool: `edmm transform multi <input>`.
For a deployment with multiple technologies the transform target is always "multi". Which technology is used exactly can be specified in the deployment model.
The generated technology-specific deployment models will be stored relative to the YAML input file. Every orchestration step has its own folder.
After the files are created the deployment can be started when the command line shows 
`Enter y to continue with orchestration`. After pressing y it begins.

## What happens?

The multi plugin is executed in different lifecycle phases.

The two special phases are `transform` and `orchestration`.
The phases are called from `edmm-core/src/main/java/io/github/edmm/plugins/multi/MultiLifecycle.java`.
Most of the implementation is in `edmm-core/src/main/java/io/github/edmm/plugins/multi/`.


### Transform

A graph with reversed edges is built. Then, components that don't create a cycle are grouped.
Related to https://link.springer.com/article/10.1007/s00607-019-00721-8 \

For each group a "context" is created that collects all the needed infos for the transforamtion.
Including a differentation which attributes are only known during runtime and which not.
Every component is "visited" in order and the result added to the context.

At the end the framework transforms this complete information to a concrete deployment model.
Additionally it creates a deployment plan `execution.plan.json`. Here the order of deployment is defined.


The Visitor implementation depends on the tech.

Valid for all techs:
- Environment variables from hosted_on are used transitively and keep their name
- Env vars from other connections are not used automatically and need to be called explicitly syntax see model
    - this can be changed with a few lines in 
This can be changed in `TopologyGraphHelper.findAllProperties`. When uncommenting the lines all vars that come through connects_to are imported
as <targetCompname>.<variablename>.




[Kubernetes](kubernetes.md)
[Ansible](ansible.md)
[Terraform](terraform.md)



### Orchestration(name is open for alternatives)

The orchestration step reads the plan `execution.plan.json`. Here the groups and their technology are defined.
These are read and then the corresponing model is executed. 

The execution works as follows:
In this step the runtime properties are provided. Then the deployment is executed.
After that the properties that become known during execution are added back to the model.
After every step the `state.yaml` is updated to reflect the new infos.

[Kubernetes](kubernetes.md)
[Ansible](ansible.md)
[Terraform](terraform.md)

### possible future work
- decpouple orchestration from transformation
- new instance model after transformation
- add verification

    


## Build the project

We use Maven as our build tool:

```shell
./mvnw clean package
```
