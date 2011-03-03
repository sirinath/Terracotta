/**
 * 
 */
package com.tctest.perf.dashboard.common.cache.ds;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tctest.perf.dashboard.common.cache.EventStatistics;

/**
 * Node class that represents 
 * - individual application instances 
 * - Logical groups of individual application instances
 * - Logical Groups of logical groups (above)
 * 
 */
public class Node<E extends EventStatistics> {
	
	/**
	 * Name of the node
	 */
	private final String name;
	
	/**
	 * Parent Node
	 */
	private Node<E> parent = null;
	
	/**
	 * Children
	 */
	
	//TODO we may need to look ad this one .. whether we can do with NOT having the reference of children 
	// that may make it easier for terracotta
	private Map<String,Node<E>> children = new HashMap<String,Node<E>>();

	/**
	 * map of the EventChronicles against the event name
	 * 
	 */
	private Map<String,EventChronicle<E>> eventStats = new HashMap<String,EventChronicle<E>>();
	
	/**
	 * 
	 */
	public Node(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @return parent node
	 */
	public Node<E> getParent() {
		return parent;
	}
	/**
	 * 
	 * @param parent
	 */
	public void setParent(Node<E> parent) {
		this.parent = parent;
		parent.addChild(this);
	}
	/**
	 * 
	 * @return children
	 */
	public Map<String, Node<E>> getChildren() {
		return children;
	}
	/**
	 * 
	 * @return child node
	 */
	public Node<E> getChild(String name) {
		if(name == null) return null;
		return children.get(name.toUpperCase());
	}
	/**
	 * 
	 * @param child
	 */
	public void addChild(Node<E> child) {
		children.put(child.getName().toUpperCase(), child);
	}
	
	/**
	 * 
	 * @param eventName
	 * @param chronicle
	 */
	public void addEventChronicle(String eventName,EventChronicle<E> chronicle){
		eventStats.put(eventName.toUpperCase(), chronicle);
	}
	
	/**
	 * get the chronicle for this node for the given event 
	 * @param eventName
	 * @return event chronicle
	 */
	public EventChronicle<E> getEventChronicle(String eventName){
		if(eventName == null) return null;
		return eventStats.get(eventName.toUpperCase());
	}
	/**
	 * 
	 * @return
	 */
	public Set<String> getEventNames(){
		return new HashSet(eventStats.keySet());
	}
	
	/**
	 * 
	 * @return name
	 */
	private String getName() {
		return name;
	}
	
	
	/**
	 * 
	 * @param obj
	 * @return
	 */
	public boolean equals(Object obj){
		if(obj == null || !(obj instanceof Node)) return false;
		Node that = (Node)obj;
		if(that.name != this.name) return false;
		if(children != null && that.children == null) return false;
		if(children ==null && that.children != null) return false;
		if(children.size() != that.children.size()) return false;
		if(that.eventStats == null && this.eventStats != null) return false;
		if(that.eventStats != null && this.eventStats == null) return false;
		
		//just check if the events are the same for both the nodes .. no need to go the the chronicle level
		for(String key : eventStats.keySet()){
			if(that.eventStats.get(key) == null) return false;
		}
		//check if all the children are there .. and check them for equality ... 
		for(String childName : this.children.keySet()){
			if(that.children.get(childName) == null || !children.get(childName).equals(that.children.get(childName))) return false; 
		}
		
		
		 
		
		return true;
	}
	
	/**
	 * 
	 * @return to string
	 */
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(name).append(" -> ").append(children.size()).append(" children ").append(eventStats.size()).append(" statistics");
		return sb.toString();
	}

	// TEMPORARY UTILITY METHODS ... to be deleted
	/**
	 * 
	 * @return subtree
	 */
	public List<String> getSubtreeString(){
		List<String> list = new ArrayList<String>();
		list.add(name);
		for(Node child : children.values()){
			for(Object line : child.getSubtreeString()){
				list.add(name+"."+line.toString());
			}
		}
		return list;
	}
	/**
	 * 
	 */
	public void printSubtree(){
		for(Object line : getSubtreeString()){System.out.println(line);}
	}
	

}
