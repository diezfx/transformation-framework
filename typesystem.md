# Type system

## implemented at the moment :properties have types
problem: what if one component has more than one port to offer e.g. https/grpc

sol1: solve with name -> bad becase every component would need to use the same names \
sol2: solve with types one port is "port.https" the other "port.grpc"


## future 
## capabilities have types
type capabilities e.g. database.mysql or  tosca.capabilities.Compute
these capability blocks define a set of capabilities that have to be fulfilled

```yaml
requirements:
        db: database.mysql # interface somewhere else defined
        host: tomcat # depending on implementation needs tomcat and compuite?
capabilities:
        host: capabilities.Compute    #let it stay as it is?
```

e.g. database.mysql implies:
```yaml
database.mysql:
        port:
            type: string(?)
            default_value: 3306
         address:
            type: string(?) ## only exists transtively??
          user:
            type: string
           password:
            type: string
            
```
address is only a transitive capability, so naming not in control


tosca:
```yaml
tosca.nodes.Database:
  derived_from: tosca.nodes.Root # needed?
  properties:
    name:
      type: string
      description: the logical name of the database
    port:
      type: integer
      description: the port the underlying database service will listen to for data
    user:
      type: string
      description: the optional user account name for DB administration
      required: false
    password:
      type: string
      description: the optional password for the DB user account
      required: false
  requirements:
    - host:
        capability: tosca.capabilities.Compute
        node: tosca.nodes.DBMS
        relationship: tosca.relationships.HostedOn
  capabilities:
    database_endpoint:
      type: tosca.capabilities.Endpoint.Database
```

What to do with 
