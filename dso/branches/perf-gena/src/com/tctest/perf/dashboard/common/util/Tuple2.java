/**
 * 
 */
package com.tctest.perf.dashboard.common.util;

/**
 * 
 * A 2-Tuple - a Pair
 * 
 */
public final class Tuple2<V1,V2> {
	
	public final V1 _1;
	public final V2 _2;

	/**
	 * 
	 * @param v1
	 * @param v2
	 */
	public Tuple2(V1 v1,V2 v2){
		this._1 = v1;
		this._2 = v2;
	}

	/**
	 * 
	 * @return V1
	 */
	public V1 get_1() {
		return _1;
	}
	/**
	 * 
	 * @return V2
	 */
	public V2 get_2() {
		return _2;
	}

	/**
	 * 
	 */
	public String toString(){
		return "("+_1.toString()+","+_2.toString()+")";
	}
	
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o){
		if(!(o instanceof Tuple2)) return false;
		Tuple2<V1,V2> that = (Tuple2<V1,V2>)o;
		
		if(that._1 == null && this._1 != null) return false;
		if(that._2 == null && this._2 != null) return false;
		
		if(that._1 != null && this._1 == null) return false;
		if(that._2 != null && this._2 == null) return false;
		
		if(!that._1.equals(this._1) || !that._2.equals(this._2)) return false;
		return true;
	}
	
	/**
	 * 
	 */
	public int hashCode(){
		return _1.hashCode() + _2.hashCode();
	}
}
