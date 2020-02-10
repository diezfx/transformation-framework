## idea:
keep topological order for consistency
visit every component and then:

### transformation
1. implemented:
  - am i top component?
      - no: do nothing
      - yes: crete dockerfile, yamls for whole stack
     
2. later option(?):
  - am i top component?
    - no: create partial dockerfile for this component
    - yes: import dockerfile through hosted_on, create yamls
    
    
Furthermore:
- required vars are collected and added to configmap/secretmap
- transformation only returns yaml that needs a configmap/secretmaop during runtime



### orchestrator:
if kubernetes-visitor; only deploy when top
- create configmap, deploy configmap
- deploy deployment
- deploy service


### read external ips and other stuff(?)

[-] add way to read external ip from service
- problem 1: properties vs. this vs. service
services' nodeport is another port than internally used -> Cast to Property with portvalue and nodeport value
- problem 2: ip adress; just take internal ip for now
- problem 3: 2 kubernetes stacks on same host? -> is not same host in kuberntes 
  solution: just set adress at this component 


### special cases:
  what if 2 components are deployed in same cluster -> check and use service name?
