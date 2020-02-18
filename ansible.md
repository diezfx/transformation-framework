# ansible
Ansible is only used for software components

##transformation
when the component is visited a play is created. This should be more or less the same as the plugin.
One difference is that runtime vars are recognized as well and added in a new runtimeEnv category.

## orchestration
The runtime vars are made available through the a json. That is named `<component>.json`.
Then the playbook is executed.
At the moment no new variables can be computed by the examples, so a `<component>-output.json` is not necessary, but could be added without problems.