import com.gemstone.gemfire.cache.client.ClientCache
import com.gemstone.gemfire.cache.client.ClientCacheFactory
import com.gemstone.gemfire.cache.client.ClientCache
import com.gemstone.gemfire.cache.client.ClientCacheFactory
import com.gemstone.gemfire.cache.client.Pool
import com.gemstone.gemfire.cache.client.PoolFactory
import com.gemstone.gemfire.cache.client.PoolManager
import com.gemstone.gemfire.cache.execute.Execution
import com.gemstone.gemfire.cache.execute.FunctionService
import com.gemstone.gemfire.cache.execute.ResultCollector
import org.codehaus.groovy.runtime.typehandling.GroovyCastException

/*
 * Clears all entries from a region
 */

// parse the command line args for a locator and regions using Apache CliBuilder
def cl = new CliBuilder(usage: 'ClearRegions_1 -l "locatorHost" -p "nnnnn"  [regions]*')
cl.l(argName:'locator', longOpt:'locator', args:1, required:true, 'Name of a locator host in the pool, REQUIRED')
cl.p(argName:'port', longOpt:'port', args:1, required:true, 'the port number of the locator, REQUIRED')
def opt = cl.parse(args)

String locatorHost = opt.l
int locatorPort = Integer.parseInt(opt.p)
List<String> regionNameList = opt.arguments()

// Create a client cache 
ClientCache cache = new ClientCacheFactory()
		.set("name", "RegionCleaner")
		.set("log-level", "error")
		.create();

// create a connection pool to the locators		
PoolFactory factory = PoolManager.createFactory();
factory.addLocator(locatorHost, locatorPort);
Pool pool = factory.create("pool");

String regionNames = regionNameList.join(",");
String[] regionNameArray = regionNames.split(",")

System.out.println("-----------------------------")
System.out.println("Processing regions " + regionNames)

// pass the region name to the Clear Region function
Execution execution = FunctionService.onServer(pool).withArgs(regionNames);
ResultCollector rc = execution.execute("ClearRegionFunction");

// print the number of entries deleted. If an error was sent back, print it
List<String> results;
int resultIx = 0;
boolean hasMore=true;
while (hasMore) {
  try {
	results = rc.getResult();
	println("Number of results=" + (results.size() - 1))
	for (String result : results) {
	  if (result == null) {
		  hasMore = false;
	      break;
	  }
	  if (result.length() == 0) {
		// last entry
		  hasMore = false;
		  break;
	  }
	  println(result);	
	}
  }
  catch (MissingMethodException | GroovyCastException e) {
	String exception = results.get(resultIx);
	println ("Error getting results[ " + (resultIx + 1) + "]:" + exception);
  }
}
println()

System.out.println("Done");
System.out.println();

// Close cache
cache.close();

