# todos
## in general

- [ ] testing
## properties
- [x] just copy all with connects_to
- [x] implement syntax propname: $(db.bla)


## kubernetes
- implement kubernetes (see kubernetes.md)
- [x] add kubernetes transformer
- [x] add kubernetes executor
- [x] use configmap for runtime vars
- [x] read vars during runtime 



## considerations
- use real plugin structure not mock it
- executor could be made completely independent


## done 

[x] deployment technology in groupings instead of property
```yaml
orchestration_technology:
    ansible:
        - <component_name>
        ...
```

