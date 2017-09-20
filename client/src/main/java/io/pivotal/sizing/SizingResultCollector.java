package io.pivotal.sizing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import com.gemstone.gemfire.cache.execute.FunctionException;
import com.gemstone.gemfire.cache.execute.ResultCollector;
import com.gemstone.gemfire.distributed.DistributedMember;

public class SizingResultCollector implements ResultCollector<Map<String, Long>, List<Map<String, Long>>> {

	private List<Map<String, Long>> resultList = new ArrayList<>();

	private long totalEntries = 0;
	private long totalSamples = 0;
	private long totalSerializedRegionEntrySizeBefore = 0;
	private long totalKeySize = 0;
	private long totalDeserializedValueSize = 0;
	private long totalDeserializedRegionEntrySizeAfter = 0;
	private long totalSerializedValueSize = 0;

	private static String totalSerializedRegionEntrySizeBeforeLabel = "Total RegionEntry size (serialized)";
	private static String totalKeySizeLabel = "Total Key size";
	private static String totalDeserializedValueSizeLabel = "Total Value size (deserialized)";
	private static String totalDeserializedRegionEntrySizeAfterLabel = "Total RegionEntry size (deserialized)";
	private static String totalSerializedValueSizeLabel = "Total Value size (serialized)";
	private static String totalEntriesLabel = "_Total Entries";
	private static String totalSamplesLabel = "_Total Sampled Entries";

	public void addResult(DistributedMember memberID, Map<String, Long> results) {

		Set<String> keys = results.keySet();
		Iterator<String> keyIter = keys.iterator();
		while (keyIter.hasNext()) {
			String key = keyIter.next();
			if (key.equalsIgnoreCase(totalSerializedRegionEntrySizeBeforeLabel)) {
				Long size = results.get(key);
				totalSerializedRegionEntrySizeBefore += size;
			} else if (key.equalsIgnoreCase(totalKeySizeLabel)) {
				Long size = results.get(key);
				totalKeySize = size;
			} else if (key.equalsIgnoreCase(totalDeserializedValueSizeLabel)) {
				Long size = results.get(key);
				totalDeserializedValueSize += size;
			} else if (key.equalsIgnoreCase(totalSerializedValueSizeLabel)) {
				Long size = results.get(key);
				totalSerializedValueSize += size;
			} else if (key.equalsIgnoreCase(totalDeserializedRegionEntrySizeAfterLabel)) {
				Long size = results.get(key);
				totalDeserializedRegionEntrySizeAfter += size;
			} else if (key.equalsIgnoreCase(totalEntriesLabel)) {
				Long size = results.get(key);
				totalEntries += size;
			} else if (key.equalsIgnoreCase(totalSamplesLabel)) {
				Long size = results.get(key);
				totalSamples = size;
			}
		}

		results.put(" " + memberID.getName(), 0L);

		this.resultList.add(results);
	}

	private Map<String, Long> generateTotals() {

		Map<String, Long> clusterTotals = new HashMap<>();
		clusterTotals.put(" " + "Cluster Total", 0L);
		clusterTotals.put("Cluster" + totalSerializedRegionEntrySizeBeforeLabel,
				this.totalSerializedRegionEntrySizeBefore);
		clusterTotals.put("Cluster" + totalDeserializedRegionEntrySizeAfterLabel,
				this.totalDeserializedRegionEntrySizeAfter);
		clusterTotals.put("Cluster" + totalKeySizeLabel, this.totalKeySize);
		clusterTotals.put("Cluster" + totalSerializedValueSizeLabel, this.totalSerializedValueSize);
		clusterTotals.put("Cluster" + totalDeserializedValueSizeLabel, this.totalDeserializedValueSize);
		clusterTotals.put("Cluster" + totalSamplesLabel, this.totalSamples);
		clusterTotals.put("Cluster" + totalEntriesLabel, this.totalEntries);

		return clusterTotals;
	}

	public void endResults() {
	}

	public List<Map<String, Long>> getResult() throws FunctionException {
		return getResult(0, TimeUnit.SECONDS);
	}

	public List<Map<String, Long>> getResult(long timeout, TimeUnit unit) throws FunctionException {
		Map<String, Long> clusterTotals = generateTotals();
		resultList.add(clusterTotals);
		return resultList;
	}

	public void clearResults() {
		resultList.clear();
	}
}
