package com.tctest.perf.dashboard.common.cache;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.tctest.perf.dashboard.common.cache.ds.EventChronicle;
import com.tctest.perf.dashboard.common.cache.ds.Node;
import com.tctest.perf.dashboard.common.data.Statistics;
import com.tctest.perf.dashboard.common.metadata.MetaData;
import com.tctest.perf.dashboard.common.util.Tuple2;

/**
 * 
 * The cache that holds the statistics in three dimensions.
 * 1st dimension - A Tree that depicts the logical grouping of the sources in the following order
 * Application, DataCenter, Partition, Host, Instance.
 * <p>	This dimension can be of any depth ranging from 1 to 5
 * 	e.g. a depth of 3 means Application,DataCenter,Partition nodes only 
 * <p>	This may be useful in handling the DB statistics as there are no hosts of patitions involved
 *  <p>a depth of 4 can be useful for SNMP data
 *  <p>a depth of 5 would be useful for iTunes application level data 
 *  etc.
 *  <p><p>2d dimension - A Map that resides on every node of the first dimension that maps the event name 
 *  like 'buyProduct' etc. to a datastructure(3rd dimension) that saves it's statisticcs for past X number of mins/hours etc.
 *  <p><p>3rd Dimension - A TreeMap that save last 'n' statistics for the given event ... is also called an EventChronicle 
 * 
 */
public class Cache<E extends EventStatistics> {
	
	/**
	 * 
	 */
	private final String name;
	/**
	 * 
	 */
	private final int depth;
	/**
	 * 
	 */
	private final Node<E> root;
	/**
	 * 
	 */
	private MetaData<E> metaData;
	
	
	/**
	 * 
	 */
	private final Date timeOfCreation = new Date();
	
	/**
	 * @param depth 
	 * 
	 */
	public Cache(String name,Node<E> root, int depth) {
		this.name = name;
		this.root = root;
		this.depth = depth;
	}
	/**
	 * 
	 * @return name
	 */
	public String getName() {
		return name;
	}
	/**
	 * 
	 * @return root node
	 */
	public Node<E> getRoot() {
		return root;
	}
	

	/**
	 * 
	 * @return metaData
	 */
	public MetaData<E> getMetaData() {
		return metaData;
	}
	/**
	 * 
	 * @param metaData
	 */
	public void setMetaData(MetaData<E> metaData) {
		this.metaData = metaData;
	}

	/**
	 * Returns the statistics for the given time window
	 * for the node identified with the given fqn
	 * 
	 * @param eventName Name of the event e.g. buyProductAction
	 * @param statName Name of the statistic e.g. minTime
	 * @param from from time
	 * @param to to time
	 * @param fqn identifier for the given node e.g. mzstore.dc1.
	 * @return A List of Tuples of Dates and numbers
	 * @throws CacheException
	 */
//	public List<Tuple2<Date,Number>> getStatistics(String eventName, String statName,Date from, Date to,String ... fqn) throws CacheException{
	public Statistics getStatistics(String eventName, String[] statNames,Date from, Date to,String ... fqn) throws CacheException{

		if(eventName == null || eventName.equalsIgnoreCase("")){
			throw new CacheException("Event Name not specified");
		}
		
//		if(statNames == null || statNames.length == 0){
//			throw new CacheException("Statistic Names not specified");
//		}
		
		if(from == null ){
			throw new CacheException("From Date not specified");
		}

		
		if(to == null){
			//get data upto now.
			to = new Date();
		}
		

		if(fqn == null || fqn.length == 0){
			throw new CacheException("Node not specified");
		}
		
		if(fqn.length > depth){
			throw new CacheException("Cache is not so deep. fqn length is greater than cache depth");
		}
		
		
		/**
		 * 
		 */
		Node<E> target = root;
		for(String nodeName : fqn){
			target = target.getChild(nodeName);
			if(target == null) break;
		}

		if(target == null){
			//inefficient code.. but ideally this should never happen if the caller is any sane.
			String errorFQN = "";
			for(String nodeName : fqn){
				errorFQN.concat(nodeName).concat(" ");
			}
			throw new CacheException("Invalid fqn : " + errorFQN);
		}
		
		EventChronicle<E> chronicle = target.getEventChronicle(eventName);
		
		if(chronicle == null)
			throw new CacheException("Unkown event Name : "+eventName);
		
		Date epoch = chronicle.getEpoch();
		if(epoch != null && epoch.after(from)){
			return null;//null return means that the range falls outside the scope of this cache 
			// as in ... the from date is not on or after the epoch for the chronicle
		}
		
		List<Tuple2<Date,E>> list = chronicle.getStatistics(from, to);
		
		if(list == null)
			return null;
		
		Map<String,List<Tuple2<Date,Number>>> map = new HashMap<String,List<Tuple2<Date,Number>>>();
		
		if(statNames == null){
			
			//TODO figure out the statNames here
			if (!list.isEmpty()) {
				statNames = ((E)list.get(0).get_2()).getStatNames().toArray(new String[1]);
			} else {
				// Then the statistics is not there for the event so return null
				return null;
			}
		}
		
		Iterator<Tuple2<Date,E>> i =  list.iterator();
		
		while(i.hasNext()){
			
			Tuple2<Date,E> tuple = i.next();
			//create a new date object .. you don't want anyone outside to modify the real date object
			// or to even hold on to its reference
			Date eventTime = new Date(tuple._1.getTime());
			E e = tuple._2;
		
			for(String statName : statNames){
				
				
				Number value = e.getValue(statName);
				if(value == null) //TODO figure out if it should throw an exception or just return back 
					throw new CacheException("Invalid Stat Name "+statName);
				
				List<Tuple2<Date,Number>> data =map.get(statName);
				if(data == null){
					data = new ArrayList<Tuple2<Date,Number>>();
					map.put(statName, data);
				}
					
				data.add(new Tuple2<Date,Number>(eventTime,value));
			}
			
		}

		
		Statistics statistics = new Statistics(eventName,statNames,map);

		return statistics;
	}
	
	/**
	 * add the given Statistics bean to the cache
	 * 
	 * @param stat
	 * @param fqn
	 */
	public void addStatistics(E stat,String ... fqn) throws CacheException{
		if(fqn == null || fqn.length == 0){
			throw new CacheException("Node not specified");
		}

//		if(fqn.length > depth){
//			throw new CacheException("Cache is not so deep. fqn length is greater than cache depth");
//		}
		
		if(stat == null){
			throw new CacheException("Statistics object is null. ");
		}
		
		//TODO ... ideally one should not be made to go through the tree to get the data ... 
		// the node should be reachable directly ...  
		Node<E> target = root;
		for(String nodeName : fqn){
			target = target.getChild(nodeName);
			if(target == null) break;
		}

		// TODO ... we may need to add a new node in case we get an unknown target name ...  
		if(target == null){
			//inefficient code.. but ideally this sould never happen if the caller is any sane.
			String errorFQN = "";
			for(String nodeName : fqn){
				errorFQN.concat(nodeName).concat(" ");
			}
			throw new CacheException("Invalid fqn : " + errorFQN);
		}
		
		EventChronicle<E> chronicle = target.getEventChronicle(stat.getEventName());
		if(chronicle == null)
			throw new CacheException("Unkown event Name : "+stat.getEventName() + " for path " + target.toString());

		chronicle.addStatistics(stat.getCaptureTime(), stat);//TODO this may have to be changed to round up the time at minute level
		
	}
	
	
	
	/**
	 * 
	 * @return boolean
	 */
	public boolean equals(Object obj){
		if(obj == null || !(obj instanceof Cache)) return false;
		Cache that = (Cache)obj;
		if(that.name != this.name) return false;
		if(that.depth != this.depth) return false;
		if(that.metaData != this.metaData)  return false; //shallow check
		if(!that.root.equals(this.root)) return false;
		return true;
	}
	/**
	 * @return the timeOfCreation
	 */
	public Date getTimeOfCreation() {
		return timeOfCreation;
	}
	
}
