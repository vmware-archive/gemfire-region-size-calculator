package io.pivotal.utils;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.gemstone.gemfire.LogWriter;
import com.gemstone.gemfire.cache.Region;
import com.gemstone.gemfire.cache.partition.PartitionRegionHelper;
import com.gemstone.gemfire.internal.cache.CachedDeserializable;
import com.gemstone.gemfire.internal.cache.EntrySnapshot;
import com.gemstone.gemfire.internal.cache.LocalRegion;
import com.gemstone.gemfire.internal.cache.PartitionedRegion;
import com.gemstone.gemfire.internal.cache.RegionEntry;
import com.gemstone.gemfire.internal.size.ObjectGraphSizer;
import com.gemstone.gemfire.internal.size.ReflectionObjectSizer;
import com.gemstone.gemfire.pdx.PdxInstance;

public class SizeCalculator {

	private LogWriter logger = null;

	private long avgDeserializedRegionEntrySizeBefore = 0;
	private long avgDeserializedRegionEntrySizeAfter = 0;
	private long avgDeserializedKeySize = 0;
	private long avgSerializedValueSize = 0;
	private long avgDeserializedPdxValueSize = 0;
	private long avgDeserializedValueSize = 0;

	private long totalDeserializedRegionEntrySizeBefore;
	private long totalDeserializedKeySize;
	private long totalDeserializedValueSize;
	private long totalDeserializedPdxValueSize;
	private long totalDeserializedRegionEntrySizeAfter;
	private long totalSerializedValueSize;
	
	private long regionTypeInd;
	private long regionSize;

	static String totalSerializedRegionEntrySizeBeforeLabel = "Total RegionEntry size (serialized)";
	static String totalKeySizeLabel = "Total Key size";
	static String totalDeserializedValueSizeLabel = "Total Value size (deserialized)";
	static String totalDeserializedPdxValueSizeLabel = "Total PDX Value size (deserialized)";
	static String totalDeserializedRegionEntrySizeAfterLabel = "Total RegionEntry size (deserialized)";
	static String totalSerializedValueSizeLabel = "Total Value size (serialized)";
	static String totalEntriesLabel = "_Total Entries";
	static String totalSamplesLabel = "_Total Sampled Entries";

	public static String grandTotalKeySizeLabel = "Keys size";
	public static String grandTotalDeserializedValueSizeLabel = "Deserialized values size";
	public static String grandTotalDeserializedPdxValueSizeLabel = "Deserialized PDX size";
	public static String grandTotalSerializedValueSizeLabel = "Serialized values size";
	public static String grandTotalEntriesLabel = "Entries";
	
	NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.US);

	/**
	 * Creates an instance that logs all the output to <code>System.out</code>.
	 */
	public SizeCalculator() {
	}

	/**
	 * Creates an instance that logs to the provided <code>LogWriter</code>.
	 * 
	 * @param logger
	 *            <code>LogWriter</code> to use for all the output.
	 */
	public SizeCalculator(LogWriter logger) {
		this.logger = logger;
	}

	/**
	 * Calculates the size of an object.
	 * 
	 * @param The
	 *            object to size
	 * @return The size of object o
	 */
	public static int sizeObject(Object o) {
		return ReflectionObjectSizer.getInstance().sizeof(o);
	}

	public static String histObject(Object o) throws IllegalArgumentException, IllegalAccessException {
		return ObjectGraphSizer.histogram(o, false);
	}

	/**
	 * Calculates and logs the size of all entries in the region.
	 * 
	 * @param string
	 */
	public Map<String, String> sizeRegion(Region<?,?> region) {
		return sizeRegion(region, 0);
	}

	/**
	 * Calculates and logs the size of first numEntries in the region.
	 * 
	 * @param region
	 * @param numberOfSamples
	 *            The number of entries to calculate the size for. If 0 all the
	 *            entries in the region are included.
	 */
	public Map<String, String> sizeRegion(Region<?,?> region, int numberOfSamples) {
		if (region == null) {
			throw new IllegalArgumentException("Region is null.");
		}

		if (region instanceof PartitionedRegion) {
			return sizePartitionedRegion(region, numberOfSamples);
		} else {
			return sizeReplicatedOrLocalRegion(region, numberOfSamples);
		}
	}

	/**
	 * Sizes numEntries of a partitioned region, or all the entries if numEntries is
	 * 0.
	 * 
	 * @param numberOfSamples
	 *            Number of entries to size. If the value is 0, all the entries are
	 *            sized.
	 */
	private Map<String, String> sizePartitionedRegion(Region<?,?> region, long numberOfSamples) {
		regionTypeInd = 0L;
		Region<?,?> primaryDataSet = PartitionRegionHelper.getLocalData(region);
		regionSize = primaryDataSet.size();
		log("region size=" + regionSize + ", numEntries=" + numberOfSamples);
		if (numberOfSamples == 0) {
			numberOfSamples = primaryDataSet.size();
		} else if (numberOfSamples > regionSize) {
			numberOfSamples = regionSize;
		}

		int count = 0;
		for (Iterator<?> i = primaryDataSet.entrySet().iterator(); i.hasNext();) {
			if (count == numberOfSamples) {
				break;
			}
			EntrySnapshot entry = (EntrySnapshot) i.next();
			RegionEntry re = entry.getRegionEntry();
			dumpSizes(entry, re);
			count++;
		}

		dumpTotalAndAverageSizes(numberOfSamples);
		Map<String, String> results = packageResults(numberOfSamples);
		clearTotals();
		return results;
	}

	/**
	 * Sizes numEntries of a replicated or local region, or all the entries if
	 * numEntries is 0.
	 * 
	 * @param numberOfSamples
	 *            Number of entries to size. If the value is 0, all the entries are
	 *            sized.
	 */
	private Map<String, String> sizeReplicatedOrLocalRegion(Region<?,?> region, long numberOfSamples) {
		regionTypeInd = 1L;
		Set<?> entries = region.entrySet();
		regionSize = entries.size();
		if (numberOfSamples == 0) {
			numberOfSamples = entries.size();
		} else if (numberOfSamples > regionSize) {
			numberOfSamples = regionSize;
		}

		int count = 0;
		for (Iterator<?> i = entries.iterator(); i.hasNext();) {
			if (count == numberOfSamples) {
				break;
			}
			LocalRegion.NonTXEntry entry = (LocalRegion.NonTXEntry) i.next();
			RegionEntry re = entry.getRegionEntry();
			dumpSizes(entry, re);
			count++;
		}

		dumpTotalAndAverageSizes(numberOfSamples);
		Map<String, String> results = packageResults(numberOfSamples);
		clearTotals();
		return results;
	}

	private void dumpSizes(Region.Entry<?,?> entry, RegionEntry re) {
		int deserializedRegionEntrySizeBefore = ReflectionObjectSizer.getInstance().sizeof(re);
		int serializedValueSize = calculateSerializedValueSize(entry, re);
		int deserializedKeySize = ReflectionObjectSizer.getInstance().sizeof(entry.getKey());
		Object value = entry.getValue();
		int deserializedValueSize;
		int deserializedPdxValueSize = 0;
		if (value instanceof PdxInstance) {
			Object actualObj = ((PdxInstance) value).getObject();
			deserializedPdxValueSize = sizeObject(actualObj);
		}
		deserializedValueSize = sizeObject(value);
		int deserializedRegionEntrySizeAfter = ReflectionObjectSizer.getInstance().sizeof(re);
		this.totalDeserializedRegionEntrySizeBefore += deserializedRegionEntrySizeBefore;
		this.totalDeserializedKeySize += deserializedKeySize;
		this.totalDeserializedValueSize += deserializedValueSize;
		this.totalDeserializedPdxValueSize += deserializedPdxValueSize;
		this.totalSerializedValueSize += serializedValueSize;
		this.totalDeserializedRegionEntrySizeAfter += deserializedRegionEntrySizeAfter;
		log("RegionEntry (key = " + re.getKey() + ") size: " + deserializedRegionEntrySizeBefore + " (serialized), "
				+ deserializedRegionEntrySizeAfter + " (deserialized). Key size: " + deserializedKeySize
				+ ". Value size: " + serializedValueSize + " (serialized), " + deserializedValueSize
				+ "(deserialized)." + ". Value type: " + value.getClass().getSimpleName());
		
		String histStats = "";
		try {
			histStats = histObject(re);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		log("Hist Stats=" + histStats);
	}

	private int calculateSerializedValueSize(Region.Entry<?,?> entry, RegionEntry re) {
		Object valueInVm = re.getValue(null);
		int serializedValueSize = 0;
		if (valueInVm instanceof CachedDeserializable) {
			// Value is a wrapper
			Object cdValue = ((CachedDeserializable) valueInVm).getValue();
			if (cdValue instanceof byte[]) {
				// The wrapper wraps a serialized domain object
				serializedValueSize = ((byte[]) cdValue).length;
			} else {
				// The wrapper wraps a deserialized domain object
				serializedValueSize = ReflectionObjectSizer.getInstance().sizeof(cdValue);
			}
		} else {
			// Value is a domain object
			serializedValueSize = ReflectionObjectSizer.getInstance().sizeof(valueInVm);
		}

		return serializedValueSize;
	}

	private void dumpTotalAndAverageSizes(long totalSamples) {
		log("Total RegionEntry size (serialized): " + this.totalDeserializedRegionEntrySizeBefore);
		log("Total RegionEntry size (deserialized): " + this.totalDeserializedRegionEntrySizeAfter);
		log("Total Key size: " + this.totalDeserializedKeySize);
		log("Total Value size (serialized): " + this.totalSerializedValueSize);
		if (this.totalDeserializedPdxValueSize > 0) {
			log("Total Pdx Value size (deserialized): " + this.totalDeserializedPdxValueSize);
		}
		log("Total Value size (deserialized): " + this.totalDeserializedValueSize);
		if (totalSamples > 0) {
			avgDeserializedRegionEntrySizeBefore = (int) (this.totalDeserializedRegionEntrySizeBefore / totalSamples);
			avgDeserializedRegionEntrySizeAfter = (int) (this.totalDeserializedRegionEntrySizeAfter / totalSamples);
			avgDeserializedKeySize = (int) (this.totalDeserializedKeySize / totalSamples);
			avgSerializedValueSize = (int) (this.totalSerializedValueSize / totalSamples);
			avgDeserializedPdxValueSize = (int) (this.totalDeserializedPdxValueSize / totalSamples);
			avgDeserializedValueSize = (int) (this.totalDeserializedValueSize / totalSamples);
		}

		log("Average RegionEntry size (serialized): " + avgDeserializedRegionEntrySizeBefore);
		log("Average RegionEntry size (deserialized): " + avgDeserializedRegionEntrySizeAfter);
		log("Average Key size: " + avgDeserializedKeySize);
		log("Average Value size (serialized): " + avgSerializedValueSize);
		log("Average Value size (deserialized): " + avgDeserializedValueSize);
		log("Average PDX Value size (deserialized): " + avgDeserializedPdxValueSize);
		log("--------------");
	}

	private Map<String, String> packageResults(long totalSamples) {
		Map<String, String> results = new HashMap<>();
		results.put(grandTotalKeySizeLabel, numberFormatter.format(avgDeserializedKeySize * regionSize));
		if (avgDeserializedValueSize != avgSerializedValueSize) {
			results.put(grandTotalDeserializedValueSizeLabel, numberFormatter.format(avgDeserializedValueSize * regionSize));
		}
		if (avgDeserializedPdxValueSize > 0) {
			results.put(grandTotalDeserializedPdxValueSizeLabel, numberFormatter.format(avgDeserializedPdxValueSize * regionSize));
		}
		results.put(grandTotalSerializedValueSizeLabel, numberFormatter.format(avgSerializedValueSize * regionSize));
		results.put(grandTotalEntriesLabel, numberFormatter.format((long) regionSize));
		results.put("Region type", (regionTypeInd == 0) ? "Partitioned" : "Replicated");

		return results;
	}

	private void clearTotals() {
		this.totalDeserializedRegionEntrySizeBefore = 0;
		this.totalDeserializedKeySize = 0;
		this.totalDeserializedValueSize = 0;
		this.totalSerializedValueSize = 0;
		this.totalDeserializedRegionEntrySizeAfter = 0;
	}

	protected void log(String message) {
		if (logger != null) {
			logger.info(message);
		} else {
			System.out.println(message);
		}
	}
}