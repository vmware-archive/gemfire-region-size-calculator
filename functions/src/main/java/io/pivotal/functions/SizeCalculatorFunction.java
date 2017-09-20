package io.pivotal.functions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.util.StopWatch;

import com.gemstone.gemfire.LogWriter;
import com.gemstone.gemfire.cache.CacheFactory;
import com.gemstone.gemfire.cache.Declarable;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.execute.FunctionAdapter;
import com.gemstone.gemfire.cache.execute.FunctionContext;

import io.pivotal.utils.SizeCalculator;

@SuppressWarnings("serial")
public class SizeCalculatorFunction extends FunctionAdapter implements Declarable {

	public static final String ID = "size-calculation-function";

	private static LogWriter log;

	static {
		log = CacheFactory.getAnyInstance().getDistributedSystem().getLogWriter();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void execute(FunctionContext fc) {

		log.info("I am in the " + this.getClass().getName() + " function");
		StopWatch sw = new StopWatch();
		
		List<Object> args = (List<Object>) fc.getArguments();
		if (args.isEmpty()) {
			Map<String, Long> results = new HashMap<String, Long>();
			fc.getResultSender().lastResult(results);
		}
		String regionName = (String) args.get(0);
		if (regionName == null) {
			regionName = "Unsupplied region name";
		}
		int sampleSize = 0;
		if (args.size() > 1) {
			sampleSize = (Integer) args.get(1);
		}
		Region<?, ?> region = CacheFactory.getAnyInstance().getRegion(regionName);
		log.info("Sizing region " + regionName + " with a sample size of " + sampleSize);
		if (region == null) {
			log.error("Supplied region does not exist: " + regionName);
			Map<String, Long> results = new HashMap<String, Long>();
			regionName = "Supplied region does not exist: " + regionName;
			results.put(regionName, 0L);
			fc.getResultSender().lastResult(results);
		}
		
		sw.start();
		SizeCalculator sizeCalculator = new SizeCalculator(log);
		Map<String, Long> results = sizeCalculator.sizeRegion(region, sampleSize);
		sw.stop();
		log.info("Sizing Calculation took: " + sw.getTotalTimeMillis() + " millis");
		fc.getResultSender().lastResult(results);
	}


	  @Override
	  public void init(Properties props) {
	  }

	  public String getId() {
	    return ID;
	  }

	  public boolean optimizeForWrite() {
	    return true;
	  }

	  public boolean hasResult() {
	    return true;
	  }

	  public boolean isHA() {
	    return true;
	  }
	}
