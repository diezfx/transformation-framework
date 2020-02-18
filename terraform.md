# Terraform
Only compute instances are supported at the moment.
For software components every command needs to be executed as sudo to be semantically the same as ansible.
This is not possible with the Remote Exec argument scripts.

## transformation
During the transformation a openstack compute instance is started.
For this to work it needs provider infos. That are submitted through the provider artifact at the moment.


## orchestration
First the vars for openstack are made available. For this the provider.json is read and the content transformed to Terraform environment variables.
The commands `terraform init` and then `terraform apply` are executed. 
After execution a json should exist with the output infos. In this case the `hostname`
This is injected into the model.