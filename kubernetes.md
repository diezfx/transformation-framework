## idea:
keep topological order for consistency

1. option implement this first maybe later use 2. option
lazy evaluation??
if component without other comp at the top, then create new container
-> always ask am i top? else do nothing; as top traverse hosted on down and build image

2. option
deploy when top but always build docker "images"
required vars are collected and added to configmap/secretmaop
transformation only returns yaml that needs a configmap/secretmaop during runtime



### orchestrator:
if kubernetes only deploy when top
create configmap, deploy configmap
deploy deployment
deploy service


### read external ips and other stuff(?)

[-] add way to read external ip from service
problem 1: properties vs. this
nodeport is another port than internally -> Cast to Property with portvalue and nodeport value
problem 2: ip adress; just take internal ip for now
problem 3: 2 kubernetes stacks on same host? -> is not same host in kuberntes 
  solution: just set adress at this component 


### special cases:
  what if 2 components are deployed in same cluster -> check and use service name?
