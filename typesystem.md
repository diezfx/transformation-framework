# Type system

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

### Main problem: How to resolve properties
#### typed property
every property is "typed see (capabilities have types); only resolve in the own component with name; look through hosted_on with type
#### naming convention
strict naming convention; admin_address is admin_address in every component \
what if there are 2? e.g. comp1(database.port) and comp2(ssh_port) both are called port?
#### reexport properties
every component needs to reexport the capabilities


## implemented at the moment :properties have types
###  what if e.g. one component has more than. one port to offer (https/grpc)
solve with types one port is "port.https" the other "port.grpc"

at them moment implemented with .startswith() so getcapabilitiesofType("port") returns both.\

### What if component exports 2 http ports
first use type then name?


### related links:
https://docs.oasis-open.org/tosca/TOSCA-Simple-Profile-YAML/v1.3/cos01/TOSCA-Simple-Profile-YAML-v1.3-cos01.html#DEFN_TYPE_CAPABILITIES_ENDPOINT

