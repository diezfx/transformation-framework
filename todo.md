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
[x] compare with types not name
[] testing
[] define types; look at tosca

## kubernetes
- implement kubernetes -> not really modular anyway so only connects_to needs change
[x] add kubernetes transformer
[-] add kubernetes orchestrator
[] read vars during runtime 
[] replace ftl with real values
[] use configmap instead of ftl

[]implement cloudformation



## later
[]testing!!
[]properties vs. cap/reqs
[]remove todos

