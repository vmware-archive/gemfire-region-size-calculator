#!/bin/bash

#DIR=$(PWD)
#APP_JARS=$DIR/../cluster-management-domain/target/cluster-management-domain-1.0.0.jar


gfsh <<!
start locator --name=locator1 --port=10334 --properties-file=config/locator.properties --initial-heap=256m --max-heap=256m
# start locator --name=locator1 --port=10334 --properties-file=config/locator.properties --initial-heap=256m --max-heap=256m  --load-cluster-configuration-from-dir=true

configure pdx --portable-auto-serializable-classes=".*";

start server --name=server1 --classpath=../../domain/target/domain-1.0.0.jar --server-port=0 --properties-file=config/gemfire.properties --initial-heap=1g --max-heap=1g
#start server --name=server2 --server-port=0 --properties-file=config/gemfire.properties --initial-heap=1g --max-heap=1g

# deploy the functions
undeploy --jar=functions-1.0.0.jar
deploy --jar=../functions/target/functions-1.0.0.jar

create region --name=Customer --type=PARTITION
create region --name=Phone --type=PARTITION

list members;
list regions;
exit;
!
