DEPENDENCY_JARS=/Users/wwilliams/.m2/repository/commons-cli/commons-cli/1.4/commons-cli-1.4.jar
GEODE_DEPENDENCIES=/usr/local/Cellar/gemfire/9.1.0/libexec/lib/geode-dependencies.jar


#java -cp client/target/client-1.5.4.RELEASE.jar:$DEPENDENCY_JARS io.pivotal.index.generator.IndexGenerator -xrefClass=Phone -xrefKeyClass=String  -indexClass=Phone -indexKeyClass=String -indexValueGetterMethod=getNumber -foreignClass=Customer -foreignKeyClass=String -foreignKeyGetterMethod=getCustomerId -targetDirectory=./server/src/main/java/io/pivotal/server/listeners

java -cp client/target/client-1.5.4.RELEASE.jar:$DEPENDENCY_JARS io.pivotal.index.generator.IndexGenerator -xrefClass=Phone -xrefKeyClass=String  -indexClass=Phone -indexKeyClass=String -indexValueGetterMethod=getSequence -foreignClass=Customer -foreignKeyClass=String -foreignKeyGetterMethod=getCustomerId -targetDirectory=./server/src/main/java/io/pivotal/server/listeners

mvn package


gfsh <<!
connect

# deploy the functions
undeploy --jar=gemfire-server-1.0.0.jar
deploy --jar=server/target/gemfire-server-1.0.0.jar


alter region --name=Phones --cache-listener=io.pivotal.server.listeners.PhoneCustomerXrefCacheListener
create region --name=PhoneCustomerXref --type=PARTITION

list members;
list regions;
exit;
!


java -cp client/target/client-1.5.4.RELEASE.jar:$GEODE_DEPENDENCIES:domain/target/domain-1.0.0.jar io.pivotal.data.generator.FastDataGenerator


gfsh <<!
connect

describe region --name=Phones;
describe region --name=PhoneCustomerXref;

exit;
!

