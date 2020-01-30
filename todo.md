- deployment technology in groupings instead of property
```yaml
orchestration_technology:
    ansible:
        - <component_name>
        ...
```

- implement requires provides model with explicit stuff
```yaml
capabilities: #change to tosca naming
    

requires:
    db:
        adress:
            type: ip # with type


```
match with tyoes not name
define types; look at tosca

- implement kubernetes -> not really modular anyway so only connects_to needs change

- implement cloudformation -> stuff todo

- improve orchestration; kubernetes doesnt need a visit for every component; just one stack

