- special cases; what if 2 components are deployed in same cluster -> check and use service name?
- how to change lifecycle service to support kbernetes
- limit model? no connects to between middle component from one stack to other stack


idea:
keep topological order for consistency
when visit look at hosted on and extend this image

1. option implement this first maybe later use 2. option
lazy evaluation??
if component without other comp at the top, then create new container
-> always ask am i top? else do nothing; as top traverse hosted on down and build image

2. option
deploy when top but always build docker "images"



orchestrator:
if kubernetes only deploy when top

else
always deploy


[] add way to read external ip from service