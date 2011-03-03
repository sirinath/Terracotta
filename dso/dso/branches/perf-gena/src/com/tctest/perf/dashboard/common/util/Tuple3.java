/**
 * 
 */
package com.tctest.perf.dashboard.common.util;

/**
 * 
 * A 3-Tuple - a triplet
 */
public final class Tuple3<V1,V2,V3> {
	
	public final V1 _1;
	public final V2 _2;
	public final V3 _3;

	/**
	 * 
	 * @param v1
	 * @param v2
	 */
	public Tuple3(V1 v1,V2 v2,V3 v3){
		this._1 = v1;
		this._2 = v2;
		this._3 = v3;
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
	 * @return V2
	 */
	public V3 get_3() {
		return _3;
	}

	/**
	 * 
	 */
	public String toString(){
		return "("+_1.toString()+","+_2.toString()+","+_3.toString()+")";
	}
	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object o){
		
		if(!(o instanceof Tuple3)) return false;
		Tuple3<V1,V2,V3> that = (Tuple3<V1,V2,V3>)o;
		
		if((this._1 == null && that._1 != null)|| (this._1 != null && that._1 == null) ) return false;
		if((this._2 == null && that._2 != null)|| (this._2 != null && that._2 == null) ) return false;
		if((this._3 == null && that._3 != null)|| (this._3 != null && that._3 == null) ) return false;
		
		if(!that._1.equals(this._1) || !that._2.equals(this._2) || !that._3.equals(this._3)) return false;
		return true;
	}
}
