# Type system

## future 
## capabilities have types
type capabilities e.g. database.mysql or  tosca.capabilities.Compute
these capability blocks define a set of capabilities that have to be fulfilled

```yaml
requirements:
        db: capabilities.endpoint.database.mysql # interface somewhere else defined; is fulfilled with connects_to
        host: capabilities.tomcat # only describe capability; not the type of component 
capabilities:
        host: capabilities.Compute
```

e.g. database.mysql implies could be defined as:
```yaml
capabiltiy-types:
        endpoint.database..mysql:
                port:
                    type: string(?)
                    default_value: 3306
                 address:
                    type: string(?) ## only exists transtively?
                 user:
                    type: string # separate user/password and use it with connects_to??
                 password:
                    type: string
        endpoint.dbms.mysql:
                port:
                    type: string(?)
                    default_value: 3306
                address:
                    type: string(?) ## only exists transtively?
                root_password:
                    type: string   
            
```
address is only a transitive capability, so naming not in control
For comparison generic tosca endpoint

```yaml
tosca.capabilities.Endpoint:
  derived_from: tosca.capabilities.Root
  properties:
    protocol:
      type: string
      required: true
      default: tcp
    port:
      type: PortDef
      required: false
    secure:
      type: boolean
      required: false
      default: false
    url_path:
      type: string
      required: false
    port_name:
      type: string
      required: false
    network_name:
      type: string
      required: false
      default: PRIVATE
    initiator:
      type: string
      required: false
      default: source
      constraints:
        - valid_values: [ source, target, peer ]
    ports:
      type: map
      required: false
      constraints:
        - min_length: 1
      entry_schema:
        type: PortSpec
  attributes: # what is this?
    ip_address:
      type: string
```

And a db_component

tosca_db_component:
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
some properties are exported through an underlying component. 
#### typed property
every property is "typed see (properties have types); look through hosted_on with type
#### naming convention
strict naming convention; admin_address is admin_address in every component \
what if there are 2? e.g. comp1(database.port) and comp2(ssh_port) both are called port?
#### reexport properties
every component needs to reexport the capabilities. No transitivity. 
Best solution?


### Different technologies need different definitions
in a kubernetes cluster some of capabilities.Compute address isn't needed\
ansible needs compute address to install sth; for kubernetes needs to use kubectl exec or sth.


## implemented at the moment :properties have types
###  what if e.g. one component has more than. one port to offer (https/grpc)
solve with types one port is "port.https" the other "port.grpc"

at them moment implemented with .startswith() so getcapabilitiesofType("port") returns both.\

### What if component exports 2 http ports
first use type then name?


### related links:
https://docs.oasis-open.org/tosca/TOSCA-Simple-Profile-YAML/v1.3/cos01/TOSCA-Simple-Profile-YAML-v1.3-cos01.html#DEFN_TYPE_CAPABILITIES_ENDPOINT

