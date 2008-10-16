/**
 * 
 */
package com.tctest.perf.dashboard.common.cache;

import java.util.Set;

import com.tctest.perf.dashboard.common.cache.ds.EventChronicle;
import com.tctest.perf.dashboard.common.cache.ds.Node;
import com.tctest.perf.dashboard.common.metadata.MetaData;
import com.tctest.perf.dashboard.common.metadata.MetaDataException;

/**
 * 
 * Factory class to create an instance off a Cache
 * 
 */
public class CacheFactory {
	

	
	/**
	 * Creates the cache using the given Meta Data. 
	 * 
	 * @param <E>
	 * @param metaData
	 * @return
	 * @throws CacheException
	 */
	public static <E extends EventStatistics> Cache<E> createCache(MetaData<E> metaData) throws CacheException{
		
		if(metaData == null)
			throw new CacheException("MetaData is null");
		
		//create the root node .... 
		Node<E> root = new Node<E>("root");
		Cache<E> cache = null;
		
		try {
			addNewNode(metaData,root,MetaData.ROOT_ID);
		} catch (MetaDataException e) {
			e.printStackTrace();
			throw new CacheException("Unable to create Cache : metadata exception -> "+e.getMessage(),e);
		}
		
		int depth = metaData.getTreeDepth();
		cache = new Cache<E>(metaData.getTypeName(),root,depth);
		
		cache.setMetaData(metaData);
		
		return cache;
	}

	/**
	 * This method 
	 * 
	 * @param <E>
	 * @param metaData
	 * @param node
	 * @param parents
	 * @throws CacheException
	 */
	private static <E extends EventStatistics> void addNewNode(MetaData<E> metaData,Node<E> node, String... pathToNode) throws MetaDataException{

		Set<String> names = metaData.getChildren(pathToNode);
		if(names == null || names.size() == 0) return;
		
		int pathLength = 0;
		//you don't want to include the rootID in the path  
		if(pathToNode!= null && pathToNode[0].equalsIgnoreCase(MetaData.ROOT_ID)){
			pathLength = pathToNode.length;
		}else{
			pathLength = pathToNode.length+1;
		}
		
		String[] pathToChildNode = new String[pathLength];
		System.arraycopy(pathToNode, 0, pathToChildNode, 0, pathToNode.length);
		
		for(String childName : names){
			Node<E> child = new Node<E>(childName);
			pathToChildNode[pathToChildNode.length-1] = childName;
			Set<String> events = metaData.getEventNamesForNode(pathToChildNode);
			child.setParent(node);
			if(events != null)
				for(String event : events){
					child.addEventChronicle(event, new EventChronicle<E>(event.toUpperCase(), metaData.getStatCount())); 
					//TODO ideally the count should not be set in the event chronicle individually.... 
					//the chronicle should refer to the global count in cache ... which can be centrally controlled 
				}
			node.addChild(child);
			addNewNode(metaData,child,pathToChildNode);
		}
		
		
	}
	
	
}
