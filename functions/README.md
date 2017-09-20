# Dynamic Server Management via gfsh

Creates server regions dynamically by invoking the same code that is used by GFSH.
This permits the regions to be stored into the cluster configuration service.

Requirements: 
1) Create the jar file from the project directory. Run mvn package with "skip tests"
$ mvn clean compile test-compile package -DskipTests

2) You will need to bring up a locator on localhost[10334] before running. For convenience I have provided the grid subdirectory. Launch startall.sh in the grid subdirectory.
$ cd grid
$ . startall.sh

The startall.sh script loads the server jar into the locator's
Cluster Configuration Service. This automatically registers the function.
If you need to create the jar for it to be loaded, run maven build package and skip the tests to create the jar.

Warning: This uses internal gemfire calls. If the implementation changes in the future, consult Geode RegionCommands & CommandResult for the upgraded code to use.
