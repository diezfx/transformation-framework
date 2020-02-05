idea:
keep topological order for consistency

1. option implement this first maybe later use 2. option
lazy evaluation??
if component without other comp at the top, then create new container
-> always ask am i top? else do nothing; as top traverse hosted on down and build image

2. option
deploy when top but always build docker "images"

required vars are collected and written in ftl-style
transformation only returns yaml.ftl that need will be replaced during runtime



orchestrator:
if kubernetes only deploy when top
replace deployment and service and substitute with avaialable variables
better use configmap!

read external ips and other stuff(?)


[] add way to read external ip from service


- special cases; what if 2 components are deployed in same cluster -> check and use service name?