package org.terracotta.lassen.models;

public class User {
  private Long    id;
  private boolean confirmed = false;
  private String  userName;
  private String  password;
  private String  email;
  private String  firstName;
  private String  lastName;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public boolean isConfirmed() {
    return confirmed;
  }

  public void setConfirmed(boolean confirmed) {
    this.confirmed = confirmed;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }
  
  public String toString() {
    return "[" 
      + id + ";" 
      + confirmed + ";" 
      + userName + ";" 
      // password left out for security concerns
      + email + ";" 
      + firstName + ";" 
      + lastName 
      + "]";
  }
}