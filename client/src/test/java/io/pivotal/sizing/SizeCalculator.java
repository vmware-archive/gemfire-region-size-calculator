package io.pivotal.sizing;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.springframework.util.StopWatch;

import com.gemstone.gemfire.LogWriter;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.client.ClientCache;
import com.gemstone.gemfire.cache.client.ClientCacheFactory;
import com.gemstone.gemfire.cache.execute.FunctionService;
import com.gemstone.gemfire.cache.execute.ResultCollector;

public class SizeCalculator {

	private static LogWriter logger = null;

	static Region<String, ?> customerRegion;
	static Region<String, ?> phoneRegion;
	static ClientCache clientCache = null;
	static String sizeCalculationFunction = "size-calculation-function";

	static {
		ClientCacheFactory ccf = new ClientCacheFactory();
		ccf.set("cache-xml-file", "META-INF/spring/gemfire/client-cache.xml");
		System.out.println(new File(".").getAbsolutePath());
		clientCache = ccf.create();
		logger = clientCache.getLogger();
	}

	/**
	 * $ SizeCalculator <regionName> <sample records to size>
	 * 
	 * For instance: $ SizeCalculator Customer 10 will size the Customer region by multiplying the number
	 * of entries in the region by the average size of 10 objects in the region
	 * @param regionName  the region name to size
	 * @param sampleSize  the number of objects to sample to compute an object average size
	 */
	public static void main(String[] args) {

		String regionName = args[0];
		customerRegion = clientCache.getRegion(regionName);
		if (customerRegion == null) {
			logger.info("Supply a defined region name to the client");
			logger.info("");
			return;
		}

		List<Object> arguments = new ArrayList<Object>();

		// sample the top 5 sizes from the region
		arguments.add(regionName);
		arguments.add(300);
		StopWatch sw = new StopWatch();
		sw.start();
		SizingResultCollector sizingResultCollector = new SizingResultCollector();
		ResultCollector<?, ?> rc = FunctionService.onRegion(customerRegion).withCollector(sizingResultCollector)
				.withArgs(arguments).execute(sizeCalculationFunction);
		sw.stop();
		logger.info("Query took " + sw.getTotalTimeMillis() + "ms: ");
		Object result = rc.getResult();
		if (!(result instanceof List<?>)) {
			logger.info("Something other than a List was returned");
		}
		List<?> results = (List<?>) result;

		for (int i = 0; i < results.size(); i++) {
			@SuppressWarnings("unchecked")
			Map<String, Long> sizings = (Map<String, Long>) results.get(i);

			SortedMap<String, Long> sortedSizings = Collections.synchronizedSortedMap(new TreeMap<>(sizings));
			Set<Map.Entry<String, Long>> sortedEntries = sortedSizings.entrySet();

			Iterator<Map.Entry<String, Long>> it = sortedEntries.iterator();
			Map.Entry<String, Long> serverEntry = it.next();
			String serverName = serverEntry.getKey();
			logger.info("\n-------------------- Results for " + serverName + " --------------------");
			for (Entry<String, Long> sizing : sortedEntries) {
				logger.info(sizing.getKey() + ": " + sizing.getValue());
			}
			logger.info("-------------------------------------------------------------");
		}
	}

}