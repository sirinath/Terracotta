package com.tctest.domain;

public class Account implements AccountIntf {

	private int id;

	private String number;
  
  private Customer customer;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getNumber() {
		return number;
	}

	public void setNumber(String number) {
		this.number = number;
	}

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
  }

  public String toString() {
		return "account.id: " + id + ", account.number: " + number;
	}
}
