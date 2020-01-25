# new feature Interfaces

## Idea
Every component can expose or require specific properties.

- provides:  These properties are exposed by this component
- requires: These properties have to be provided from another component for it to work


possible problems:
### components may not have a direct relation to their compute instance

1. e.g. compute -> tomcat -> java_service. The java_service still need some kind of "connection" property from compute.

2. if another service connects to this java_service a "connection" is needed as well that needs info  from the underlying compute instance. Something like the ip_adress.

### interface properties could have name-collisions
e.g. one service exposes 2 "ports". Solution: the properties are typed. So one port could be a mysql-port and another one for ssh.
At the moment the name implies the type. 

### more properties are bundled
e.g. ssh-conection consists of ssh-port, ssh-password, ssh-endpoint

solution for now: just name them accordingly

### the values are only known at runtime
solution for now: read the concrete value only after this component was instantiated.

### 2 services on same compute
ip adress should change to localhost


### interface vs properties 
provided interface attributes may be properties as well.
E.g. a port may be a property so that it can be set and needs to be exported to other components at the same time 

solution for now: provided/required interface stuff is exported as env variable as well, so if it fits only write it there


### later: name collisions
a provided from hostingcomp could have same name e.g. port port

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
          service-port:
            type: pet_clinic_http_port
            value: 4222
        requires: # the values are calculated during runtime
          mysql-port:
            type: integer
          mysql-adress: 
            type: string 
```


