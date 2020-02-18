# ansible
Ansible is only used for software components

At the moment it always requires from the host :
- hostname
- priv_key_path

##transformation
`io/github/edmm/plugins/multi/AnsibleVisitor.java`
when the component is visited a play is created. This is the same as the ansible plugin
One difference is that runtime vars are recognized as well and added in a new runtimeEnv category.
And the host is added dynamically(could be changed to a hosts file)

## orchestration
`io/github/edmm/plugins/multi/orchestration/AnsibleOrchestratorVisitor.java`
The runtime vars are made available through a json. That is named `<component>.json`.
Then the playbook is executed.
At the moment no new variables can be computed by the examples, so a `<component>-output.json` is not necessary, but could be added without problems.

## future
- add output handling