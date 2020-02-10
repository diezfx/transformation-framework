# todos
## in general

[] testing
[] define capability-types; look at tosca (see types.md)

## kubernetes
- implement kubernetes (see kubernetes.md)
- [x] add kubernetes transformer
- [-] add kubernetes orchestrator
- [x] use configmap for runtime vars
- [] read vars during runtime 
## cloudformation
- []implement cloudformation



## later
[]testing!!
[]properties vs. cap/reqs
[]remove todos


## done 

[x] deployment technology in groupings instead of property
```yaml
orchestration_technology:
    ansible:
        - <component_name>
        ...
```
```yaml
capabilities: #change to tosca naming
requirements:
    db:
        adress:
            type: ip # with type
```
[x] compare properties with types not name
