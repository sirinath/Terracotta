package org.terracotta.reference.exam.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "USERS")
public class User {
  
  @Id @GeneratedValue
  @Column(name = "USER_ID")
  private Long id;
  
  @Column(name = "FIRST_NAME")
  private String firstName;
  
  @Column(name = "LAST_NAME")
  private String lastName;
  
  @Column(name = "EMAIL")
  private String email;
  
  @Column(name = "PASSWORD")
  private String password;
  
  public User() {
  }

  public User(Long id, String firstName, String lastName, String email, String password) {
    super();
    this.id = id;
    this.firstName = firstName;
    this.lastName = lastName;
    this.email = email;
    this.password = password;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String toString() {
    return firstName + " " + lastName;
  }
  
  @Override
  public int hashCode() {
    return this.id.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if(obj == this) {
      return true;
    } else if(!(obj instanceof User)) {
      return false;
    } else {
      return ((User)obj).getId().equals(id);
    }
  }
}
