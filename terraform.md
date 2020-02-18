# Terraform
Only compute instances are supported at the moment.
For software components every command needs to be executed as sudo to be semantically the same as ansible.
This is not possible with the Remote Exec argument scripts.

The supported openstack instance requires:
- openstack-provider infos
- priv_key_path
- key_name

it produces:
- hostname
## transformation
`io/github/edmm/plugins/multi/TerraformVisitor.java`
Creates a terraform file to deploy a openstack compute instance.
This is similar to the terraform plugin. Only difference Openstack instead of AWS.

## orchestration
`io/github/edmm/plugins/multi/orchestration/TerraformOrchestratorVisitor.java`
For this to work it needs provider infos. That are submitted through the `provider` artifact at the compute instance at the moment.

First the vars for openstack are made available. For this the provider.json is read and the content transformed to Terraform environment variables.
The commands `terraform init` and then `terraform apply` are executed. 
After execution a json should exist with the output infos. In this case the `hostname`
This is injected into the model.


## future
- too much hardcoded
- dont use aws-classes for openstack instances
- better provider info handling
