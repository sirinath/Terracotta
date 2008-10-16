/**
 * 
 */
package com.tctest.perf.dashboard.common.metadata;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.tctest.perf.dashboard.common.cache.EventStatistics;

public class MetaData<E extends EventStatistics> {
	
	public static final String ROOT_ID = ".";

	/**
	 * 
	 */
	private final Node root;

	/**
	 * 
	 */
	private int statCount = 0;
	
	/**
	 * 
	 */
	private int treeDepth = 0;
	/**
	 * 
	 */
	private String typeName;
	
	/**
	 * 
	 */
	public MetaData(){
		this.root = new Node("ROOT");
	}
	

	/**
	 * This method adds a node under the given path.
	 * <p> Where it finds that
	 * 
	 * @param pathToNode
	 */
	public void addNode(String... pathToNode){
		
		Node parent = root;
		for(String nodeName : pathToNode){
			Node node = parent.getChild(nodeName.toUpperCase());
			if(node == null){
				node = new Node(nodeName.toUpperCase());
				node.setParent(parent);
				parent.addChild(node);
			}
			parent = node;
		}
	}
	
	/**
	 * 
	 * @param eventNames
	 * @param pathToNode
	 * @return original set if any
	 */
	public Set<String> setEventNamesForNode(Set<String> eventNames,String... pathToNode) throws MetaDataException{
		Node node = root;
		for(String nodeName : pathToNode){
			node = node.getChild(nodeName.toUpperCase());
			if(node == null){
				StringBuffer sb = new StringBuffer();
				for(String str : pathToNode) sb = sb.append(str).append(".");
				throw new MetaDataException("Invalid path : "+sb.toString());
			}
		}
		
		Set<String> retVal = node.getEventNames();
		node.setEventNames(eventNames);
		return retVal;
	}
	
	
	/**
	 * 
	 * @param pathToNode
	 * @return set event names
	 * @throws MetaDataException
	 */
	public Set<String> getEventNamesForNode(String... pathToNode) throws MetaDataException{
		Node node = root;
		Set<String> eventNames = null; 
		//loop through all the nodes in the path ... pick up the eventNames for the destination node
		// or the closest ancestor of the same
		for(String nodeName : pathToNode){
			node = node.getChild(nodeName.toUpperCase());
			if(node == null) {
				StringBuffer sb = new StringBuffer();
				for(String str : pathToNode) sb = sb.append(str).append(ROOT_ID);
				throw new MetaDataException("Invalid path : "+sb.toString());
			}
			if(node.getEventNames() != null)
				eventNames = node.getEventNames();
		}
		return eventNames;
	}
	
	
	/**
	 * Returns the name of the children for the node identified by the given path 
	 * <p> For root access -pass "."
	 * <p> For non-root access - pass the series of ids from the first non-root node
	 * <p> e.g. "app1","datacenter1","partition1"
	 * 
	 * @param pathToNode
	 * @return set children
	 * @throws MetaDataException
	 */
	public Set<String> getChildren(String... pathToNode) throws MetaDataException{
		Node node = root;
		if(pathToNode == null) throw new MetaDataException(" Null path. for root access pass \""+ROOT_ID+"\"");
		if(!(pathToNode.length == 1 && pathToNode[0].equals(ROOT_ID))){
			for(String nodeName : pathToNode){
				node = node.getChild(nodeName.toUpperCase());
				if(node == null) {
					StringBuffer sb = new StringBuffer();
					for(String str : pathToNode) sb = sb.append(str).append(".");
					throw new MetaDataException("Invalid path : "+sb.toString());
				}
			}
		}
		
		if(node.getChildren() == null) return null;
		
		return node.getChildren().keySet();
	}
		
	/**
	 * 
	 * @return int
	 */
	public int getTreeDepth() {
		return treeDepth;
	}
	/**
	 * 
	 * @param treeDepth
	 */
	public void setTreeDepth(int treeDepth) {
		this.treeDepth = treeDepth;
	}
	/**
	 * 
	 * @return typeName
	 */
	public String getTypeName() {
		return typeName;
	}
	/**
	 * 
	 * @param typeName
	 */
	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}
	/**
	 * 
	 * @return int
	 */
	public int getStatCount() {
		return statCount;
	}
	/**
	 * 
	 * @param statCount
	 */
	public void setStatCount(int statCount) {
		this.statCount = statCount;
	}
	
	/**
	 * An instance of this class represents a node in the MetaData tree
	 * @author vipul
	 *
	 */
	private class Node{
		
		/**
		 * 
		 */
		private final String name;
		
		/**
		 * 
		 */
		private Node parent = null;
		
		/**
		 * name of all the events 
		 */
		private Set<String> eventNames = null;
		
		/**
		 * 
		 */
		private int count;

		/**
		 * 
		 */
		Map<String,Node> children = null; //don't create an empty map here ... leaf nodes may not even need one .. no point creating extra objects
		
		/**
		 * 
		 * @param name
		 */
		private Node(String name){
			this.name = name;
		}
		
		/**
		 * 
		 * @param child
		 */
		public void addChild(Node child){
			if(children == null) children = new HashMap<String,Node>();// don't want to create an empty hashmap at the top unnecessarily .. better do a null check here and create if not existing already
			children.put(child.name,child);
		}
		/**
		 * 
		 * @param name
		 * @return child node
		 */
		public Node getChild(String name){
			if(children == null) return null;// don't want to create an empty hashmap at the top unnecessarily .. better do a null check here
			return children.get(name);
		}
		
		/**
		 * 
		 * @return eventNames
		 */
		public Node getParent() {
			return parent;
		}
		/**
		 * 
		 * @param parent
		 */
		public void setParent(Node parent) {
			this.parent = parent;
		}
		/**
		 * 
		 * @return eventNames
		 */
		public Set<String> getEventNames() {
			return eventNames;
		}
		/**
		 * 
		 * @param eventNames
		 */
		public void setEventNames(Set<String> eventNames) {
			this.eventNames = eventNames;
		}
		/**
		 * 
		 * @return count
		 */
		public int getCount() {
			return count;
		}
		/**
		 * 
		 * @param count
		 */
		public void setCount(int count) {
			this.count = count;
		}
		
		/**
		 * 
		 * @return children
		 */
		public Map<String,Node> getChildren(){
			return children;
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
		 * @return string
		 */
		public String toString(){
			return name+" "+children.size()+" child(ren)";
		}
		
	}



}
