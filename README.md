# EDMM Transformation Framework

> Transformation framework for the Essential Deployment Metamodel.

## Example

```shell
edmm transform multi icsoc-demo/deployment-full.yaml
```

The generated Kubernetes resource files are located relative to the `edmm_model.yml` file inside a `multi` directory.
This will create a stack for the petclinic demo application inclusive the underlying tomcat. 
Because kubernetes is chosen as the technology a configmap-,service- and deployment-file is added.

## Usage
The transformation can be started by using the `transform` command of the `edmm` tool: `edmm transform multi <input>`.
For a deployment with multiple technologies the transform target is always "multi". Which technology is used exactly can be specified in the deployment model.
The generated technology-specific deployment models will be stored relative to the YAML input file.

## What happens?

The multi plugin is executed in different lifecycle phases.



Each plugin implements its own transformation logic by providing a respective `Plugin` implementation.
The two special phases are `transform` and `orchestration`.
The phases are called from `edmm-core/src/main/java/io/github/edmm/plugins/multi/MultiLifecycle.java`.

### Transform
The general idea of the transformation is to visit every component once. 
At the moment all components are visited in the order they will be deployed. 
To visit a component, first the chosen deployment technology is looked up in the model. Then a new technology dependent "Visitor" is created and
called. This is done to reset the context, because something else that has to be deployed in between could be skipped. E.g. ansible -> terraform -> ansible
A Future optimization could be to keep the context for components that are guaranteed deployed without other technologies in between.

The Visitor implementation depends on the tech.

Valid for all techs:
- Environment variables from hosted_on are used transitively and keep their name
- Env vars from other connections are not used automatically and need to be called explicitly
This can be changed in `TopologyGraphHelper.findAllProperties`. When uncommenting the lines all vars that come through connects_to are imported
as <targetCompname>.<variablename>.


[Kubernetes](kubernetes.md)
[Kubernetes](ansible.md)
[Kubernetes](terraform.md)




### Orchestration(name is open for alternatives)
The principal idea is the same as for transformation
The major difference is the implementation of the visitors.
In this step every property that becomes known during execution is added to the model
After every step the `state.yaml` is updated to reflect the new infos.

Link kubernetes
link ansible
link terraform

## Built-in Types

The types used are all in the model. 

## Build the project

We use Maven as our build tool:

```shell
./mvnw clean package
```
