/**
 * 
 */
package com.tctest.perf.dashboard.stats.app.metadata;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.naming.ConfigurationException;

import org.apache.commons.lang.StringUtils;

import com.tctest.perf.dashboard.common.InitializationException;
import com.tctest.perf.dashboard.common.config.Configurable;
import com.tctest.perf.dashboard.common.config.Configurator;
import com.tctest.perf.dashboard.common.metadata.MetaData;
import com.tctest.perf.dashboard.common.metadata.MetaDataException;
import com.tctest.perf.dashboard.stats.app.AppEventStatistics;

/**
 * The metadata class for App Cache.
 * 
 * <p>
 * Properties that need to exist in the configuration object are (sample below)
 * <ul>
 * <li>app.stats.metadata.tree.depth
 * <li>app.stats.metadata.tree.root.mzfinance.cup.pod1.mzhost1.mzinst1
 * <li>app.stats.metadata.tree.root.mzfinance.cup.pod2.mzhost21.mzinst21
 * <li>app.stats.metadata.tree.root.mzfinance.nwk.pod1.mzhost1.mzinst1
 * <li>app.stats.metadata.tree.root.mzstore.nwk.pod1.mzhost1.mzinst1
 * <li>app.stats.metadata.tree.root.mzstore.cup.pod1.mzhost1.mzinst1
 * <li>app.stats.metadata.stats.count
 * <li>app.stats.metadata.events.eventname.buyProduct.tree.root.mzstore
 * <li>app.stats.metadata.events.eventname.songDownload.tree.root.mzstore
 * </ul>
 * <p>
 * 
 */
public class AppMetaData implements Configurable {

	private static final String YES = "Yes";
	private static final String Y = "Y";
	private static final String APP_STATS_METADATA_TREE_ROOT = "app.stats.metadata.tree.root.";
	private static final String APP_STATS_METADATA_TREE_DEPTH = "app.stats.metadata.tree.depth";
	private static final String APP_STATS_METADATA_EVENTS_EVENTNAME = "app.stats.metadata.events.eventname.";
	private static final String APP_STATS_METADATA_STATS_COUNT = "app.stats.metadata.stats.count";
	private static final String APP_STATS_METADATA = "app.stats.metadata";

	// SAMPLE CONFIGURATION .
	// app.stats.metadata.tree.depth
	// app.stats.metadata.tree.root.mzfinance.cup.pod1.mzhost1.mzinst1
	// app.stats.metadata.tree.root.mzfinance.cup.pod2.mzhost21.mzinst21
	// app.stats.metadata.tree.root.mzfinance.nwk.pod1.mzhost1.mzinst1
	// app.stats.metadata.tree.root.mzstore.nwk.pod1.mzhost1.mzinst1
	// app.stats.metadata.tree.root.mzstore.cup.pod1.mzhost1.mzinst1
	// app.stats.metadata.stats.count
	// app.stats.metadata.events.eventname.buyProduct.tree.root.mzstore
	// app.stats.metadata.events.eventname.songDownload.tree.root.mzstore

	/**
	 * 
	 */
	private MetaData<AppEventStatistics> metaData;

	/**
	 * 
	 */
	Properties properties;

	/**
	 * This method creates the metaData object for Application Statistics from
	 * the Configuration.
	 * 
	 * @throws InitializationException
	 */
	public void init(Configurator configurator) throws InitializationException {

		info("Init called for AppMetaData");

		if (metaData != null)
			throw new InitializationException("already initialized");

		try {
			properties = configurator.loadProperties();
		} catch (ConfigurationException e1) {
			e1.printStackTrace();
			throw new InitializationException(e1);
		}
		Set keys = properties.keySet();

		int depthSoFar = -1;
		int depth = -1;
		int statCount = -1;

		metaData = new MetaData<AppEventStatistics>();

		Map<String, Set<String>> eventNames = new HashMap<String, Set<String>>();

		for (Object keyObj : keys) {
			// read one property at a time and set it onto the metadata object

			String key = (String) keyObj;// can safely do that ... i guess
			// set the count
			if (key.startsWith(APP_STATS_METADATA_STATS_COUNT)) {
				String countStr = (String) properties.get(key);
				try {
					statCount = Integer.parseInt(countStr);
					metaData.setStatCount(statCount);
				} catch (NumberFormatException e) {
					error("Invalid count value in configuration " + key + ","
							+ countStr, e);
					throw new InitializationException(
							"Invalid count value in configuration " + key + ","
									+ countStr, e);
				}

			} else if (key.startsWith(APP_STATS_METADATA_EVENTS_EVENTNAME)) {

				String eventNameWithPath = StringUtils.remove(key,
						APP_STATS_METADATA_EVENTS_EVENTNAME);
				String activeFlag = (String) properties.get(key);// set only if
				// the event
				// is marked
				// active
				if (activeFlag.equalsIgnoreCase(Y)
						|| activeFlag.equalsIgnoreCase(YES)) {
					String[] arr = StringUtils.split(eventNameWithPath, '.');
					String eventName = arr[0];
					String[] pathArr = new String[arr.length - 3];// new array
					// for
					// eventName.tree.root.x.y.z
					// withouth
					// eventName.tree.root
					// part
					System.arraycopy(arr, 3, pathArr, 0, pathArr.length);
					String path = StringUtils.join(pathArr, '.').toUpperCase();
					if (eventNames.containsKey(path)) {
						eventNames.get(path).add(eventName);
					} else {
						Set<String> names = new HashSet<String>();
						names.add(eventName);
						eventNames.put(path, names);
					}

				}
			} else if (key.startsWith(APP_STATS_METADATA_TREE_DEPTH)) {
				String depthStr = (String) properties.get(key);
				try {
					depth = Integer.parseInt(depthStr);
					if (depthSoFar != -1 && depth != depthSoFar) {
						throw new InitializationException(
								"Depth is set to "
										+ depth
										+ " though one path declared already has a different depth of "
										+ depthSoFar);
					}
					metaData.setTreeDepth(depth);
				} catch (NumberFormatException e) {
					error("Invalid 'depth' value in configuration " + key + ","
							+ depthStr, e);
					throw new InitializationException(
							"Invalid 'depth' value in configuration " + key
									+ "," + depthStr, e);
				}

			} else if (key.startsWith(APP_STATS_METADATA_TREE_ROOT)) {
				String path = StringUtils.remove(key,
						APP_STATS_METADATA_TREE_ROOT);
				String activeFlag = (String) properties.get(key);
				if (activeFlag.equalsIgnoreCase(Y)
						|| activeFlag.equalsIgnoreCase(YES)) {
					String[] pathToNode = StringUtils.split(path, '.');
					if (depthSoFar == -1) { // this is the first root being read
						// ...
						depthSoFar = pathToNode.length;
					} else {// if this is not the first one .. the the depth for
						// this one should match the 'depth' OR the last
						// value for depthSoFar
						if (depth != -1 && depth != pathToNode.length) {
							error("Depth is set to "
									+ depth
									+ " though this path has a different depth "
									+ path + "  (" + pathToNode.length + ")");
							throw new InitializationException(
									"Depth is set to "
											+ depth
											+ " though this path has a different depth "
											+ path + "  (" + pathToNode.length
											+ ")");
						}
						if (depthSoFar != pathToNode.length) {
							error("Depth so far has been consistent at "
									+ depthSoFar
									+ " for all the previous nodes though this path has a different depth "
									+ path + "  (" + pathToNode.length + ")");
							throw new InitializationException(
									"Depth so far has been consistent at "
											+ depthSoFar
											+ " for all the previous nodes though this path has a different depth "
											+ path + "  (" + pathToNode.length
											+ ")");
						}

					}
					metaData.addNode(pathToNode);
				}
			}
		}

		// set all the event names

		Set<String> pathStrs = eventNames.keySet();
		for (String pathStr : pathStrs) {
			String[] path = StringUtils.split(pathStr, '.');
			Set<String> names = eventNames.get(pathStr);
			try {
				metaData.setEventNamesForNode(names, path);
			} catch (MetaDataException e) {
				error("Unable to add eventNames to the path " + pathStr, e);
				throw new InitializationException(
						"Unable to add eventNames to the path " + pathStr, e);
			}
		}

		if (depth == -1)
			throw new InitializationException(" Depth not specified ");
		if (statCount == -1)
			throw new InitializationException(" StatCount not specified ");

		info("App MetaData created : ");
		info("\tdepth = " + depth);
		info("\tstatCount = " + statCount);
		pathStrs = eventNames.keySet();
		for (String pathStr : pathStrs) {
			Set<String> names = eventNames.get(pathStr);
			info("\t" + names.size() + " event names for : " + pathStr);
		}

	}

	private void error(String msg, Throwable e) {
		e.printStackTrace();
		System.err.println("ERROR : " + msg + e.getMessage());
	}

	private void error(String msg) {
		System.err.println("ERROR : " + msg);
	}

	private void info(String msg) {
		System.out.println("INFO : " + msg);
	}

	/**
	 * 
	 * @return metaData
	 */
	public MetaData<AppEventStatistics> getMetaData() {
		return metaData;
	}

	/**
	 * 
	 * @param metaData
	 */
	public void setMetaData(MetaData<AppEventStatistics> metaData) {
		this.metaData = metaData;
	}

	public String getNamespace() {
		return APP_STATS_METADATA;
	}

}
