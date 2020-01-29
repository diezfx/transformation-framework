# new feature Interfaces

## Idea
Every component can expose or require specific properties.

- provides:  These properties are exposed by this component
- requires: These properties have to be provided from another component for it to work

components without custom java code possible?


## Possible Problems
### components may not have a direct relation to their compute instance

- e.g. compute -> tomcat -> java_service. The java_service still need some kind of "connection" property from compute.

- if another service connects to this java_service a "connection" is needed as well that needs info  from the underlying compute instance. Something like the ip_address.

option 1:
- make it transitive(?) hosted on forwards provided by variables

option2:
- make it explicit; just write them as provided as well
### interface properties could have name-collisions
At the moment the name implies the type -> name them differently

#### one service two ports
e.g. one service exposes 2 "ports". Solution: the properties are typed. So one port could be a mysql-port and another one for ssh.

#### name collisions
 e.g. ip address from compute that this service connects to and own compute; which one to take?
Computes are generic so can't name their ip differently either
idea: only when both appear together somewhere it is taken
```yaml
interface:
    requires:
        db: # one relation block one component -> one block
            address
            mysql-port
        host: # special case for hosted_on?
            ip_address :
              type : ip
            ssh_port :
              type : port
            private_key :
              type : privkey
            fingerprint? :
              type : sha
    provided:
        ip    #let it stay as it is?
        stuff #change to same grouping; but some are inherited from hosted_on
```

idea 2: like tosca; set a specific type to one block, 

```yaml
interface:
    requires:
        db: database.mysql # interface somewhere else defined
        host: tomcat # depending on implementation needs tomcat and compuite?
    provided:
        ip    #let it stay as it is?
        stuff #change to same grouping; but some are inherited from hosted_on
```
This should be more or less the same
### more properties belong together
e.g. ssh-conection consists of ssh-port, ssh-password, ssh-endpoint

solution for now: 
- provides: just name them accordingly
- requires: bundle them in one object

### the values are only known at runtime
solution for now: read the concrete value only after this component was instantiated.

### two services on same compute
ip address should change to localhost


### interface vs properties 
provided interface attributes may be properties as well.
E.g. a port may be a property that is set but needs to be exported to other components at the same time 

solution for now: provided/required interface stuff is exported as env variable as well, so if it is both don't write to property

###later: only one of several options is required


Example:
```yaml
  pet_clinic:
    type: web_application
    artifacts:
      - war: ./files/petclinic/petclinic.war
    operations:
      configure: ./files/petclinic/configure.sh
      start: ./files/petclinic/start.sh
    relations:
      - hosted_on: pet_clinic_tomcat
      - connects_to: db
    properties:
      deployment_tool: ansible
      interface:
        provides:
          http-port:
            type: pet_clinic_http_port
            value: 4222
        requires: # the values are calculated during runtime
          mysql-db:
              mysql-port:
                type: integer
              mysql-adress: 
                type: string 
```

### disadvantages

#### technology leak
if ip is required e.g. for petclinic can't use same model entity in paas environment
but: can't use anyway because of bash operations?
so legit to require some things from host?

one possible solution -> allow more alternatives; e.g. pet_clinic with host host; with tomcat plattform -> different implementations tho
should this be hidden from model?


### advantages
#### escape hatches
in theory it's possible to write a one component in terraform and another in ansible; no java code required￼

### decoupling
components are in theory completely decoupled and no custom java templating is necessary anymore?






