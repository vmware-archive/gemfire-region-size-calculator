#!/bin/bash

#. gf.config

# Issue commands to gfsh to start locator and launch a server
echo "Starting locator and server..."
gfsh <<!
start locator --name=locator1 --port=10334 --properties-file=config/locator.properties --initial-heap=256m --max-heap=256m


deploy --jar=../target/clusterManager-1.0.0.jar

list members;
list regions;
exit;
!
