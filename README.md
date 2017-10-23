# Region Size Calculator
Wes Williams
October 20, 2017

The cost of calculating object sizes in a high-speed data grid is too expensive to perform on each entry.  So how does one calculate the size of a region?

The Region Size Calculator calculates the sizes. Here are a few considerations to keep in mind.

## Understanding GemFire Serialization
GemFire stores data in serialized form. It will store the object in deserialized form in some circumstances temporarily. This deserialized object will later be garbage collected. Therefore, the actual region size will flux depending on your operations.

If you store your objects using PDX and do queries with “Select *”, GemFire will store the object in deserialized form until the next GC. If your queries use fieldnames, such as “Select lastName, firstName”, GemFire will maintain the object in serialized form.

Function execution will also affect PDX deserialization. If your function casts a PDX object to it’s domain object, the object will be stored in deserialized form on that node and that node only temporarily. 

The Region Size Calculator will return both the size of the deserialized storage and serialized storage. You can estimate the real size of the region based on your use. If you do not use “Select *” and do not cast PDX objects to the Domain object in functions, your region size will be the sum of the keys and the deserialized values.

## Installation

The code is packaged as a function. Just deploy the function once the cluster is up.
I include a sample launch script for the cluster. Execute startall.sh from the grid directory.

Here is an example on a two-node cluster:

Undeploy the old function first.
```gfsh>undeploy --jar=functions-1.0.0.RELEASE.jar
Member  |       Un-Deployed JAR       | Un-Deployed From JAR Location
------- | --------------------------- | ------------------------------------------------------------------------------------------------------
server1 | functions-1.0.0.RELEASE.jar | server1/vf.gf#functions-1.0.0.RELEASE.jar#1
server2 | functions-1.0.0.RELEASE.jar | server2/vf.gf#functions-1.0.0.RELEASE.jar#1
```

```gfsh>deploy --jar=functions/target/functions-1.0.0.RELEASE.jar
Member  |        Deployed JAR         | Deployed JAR Location
------- | --------------------------- | ------------------------------------------------------------------------------------------------------
server1 | functions-1.0.0.RELEASE.jar | server1/vf.gf#functions-1.0.0.RELEASE.jar#1
server2 | functions-1.0.0.RELEASE.jar | server2/vf.gf#functions-1.0.0.RELEASE.jar#1
```

```gfsh>list functions
Member  | Function
------- | -------------------------
server1 | region-size-calculator
server2 | region-size-calculator
```

## Execution

You can execute a function from a client program or from gfsh or the Swagger UI. I recommend gfsh since it is so easy. 

The function takes one required argument and one optional argument.
Required argument: the name of the region
Optional argument: the number of samples to take. If you have a region with 1 billion entries, you may deem it unnecessary to go through each entry and calculate its size. For this reason, this argument will limit the number of entries to sample and the total size will be projected from the results * the number of entries in the region.

Function execution arguments in gfsh are comma-delimited strings.
 
Example: To calculate the size of the Customer partitioned region on server2 with a sample size of 5:
```gfsh>execute function --id=region-size-calculator --arguments="Customer,5" --member=server2
Execution summary

           Member ID/Name            | Function Execution Result
------------------------------------ | -----------------------------------------------------------------------------------------
192.168.0.10(server2:55895)<v8>:1071 | {Serialized values size=750,148, Keys size=24,528, Region type=Partitioned, Entries=511}
```


Example: To calculate the size of the Customer partitioned region on server1 with a sample size of 5:
```gfsh>execute function --id=region-size-calculator --arguments="Customer,5" --member=server1
Execution summary

           Member ID/Name             | Function Execution Result
------------------------------------- | -----------------------------------------------------------------------------------------
192.168.0.10(server1:50892)<v1>:56765 | {Serialized values size=807,828, Keys size=23,472, Region type=Partitioned, Entries=489}

Important note: a partitioned region returns the answer node by node
```

Example: To calculate the size of the Phone replicated region with a sample size of 5:
```gfsh>execute function --id=region-size-calculator --arguments="Phone,5" --member=server1
Execution summary

           Member ID/Name             | Function Execution Result
------------------------------------- | --------------------------------------------------------------------------------------------
192.168.0.10(server1:50892)<v1>:56765 | {Serialized values size=1,563,000, Keys size=48,000, Region type=Replicated, Entries=1,000}
```

Example: To calculate the size of the Phone replicated region with a sample size of 25:
```gfsh>execute function --id=region-size-calculator --arguments="Phone,25" --member=server1
Execution summary

           Member ID/Name             | Function Execution Result
------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------
192.168.0.10(server1:50892)<v1>:56765 | {Deserialized values size=1,548,000, Serialized values size=497,000, Keys size=48,000, Region type=Replicated, Entries=1,000}
```


Example: To calculate the size of the Phone replicated region using all entries:
```gfsh>execute function --id=region-size-calculator --arguments="Phone" --member=server1
Execution summary

           Member ID/Name             | Function Execution Result
------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------
192.168.0.10(server1:50892)<v1>:56765 | {Deserialized values size=1,561,000, Serialized values size=311,000, Keys size=48,000, Region type=Replicated, Entries=1,000}
```
