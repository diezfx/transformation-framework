# todos
## in general
[x] deployment technology in groupings instead of property
```yaml
orchestration_technology:
    ansible:
        - <component_name>
        ...
```

[-|] implement requires provides model with explicit stuff
```yaml
capabilities: #change to tosca naming
    

requires:
    db:
        adress:
            type: ip # with type
```
[x] compare properties with types not name
[] testing
[] define capability-types; look at tosca (see types.md)

## kubernetes
- implement kubernetes -> not really modular anyway so only connects_to needs change
[x] add kubernetes transformer
[-] add kubernetes orchestrator
[x]use configmap for runtime vars
[] read vars during runtime 

[]implement cloudformation



## later
[]testing!!
[]properties vs. cap/reqs
[]remove todos




