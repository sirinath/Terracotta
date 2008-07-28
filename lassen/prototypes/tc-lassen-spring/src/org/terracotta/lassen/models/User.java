package org.terracotta.lassen.models;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.providers.encoding.ShaPasswordEncoder;

public class User {
  private Long        id;
  private boolean     confirmed = false;
  private String      userName;
  private String      password;
  private String      email;
  private String      firstName;
  private String      lastName;
  private Set<String> roles;

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

  public void setAndEncodePassword(final String password) {
    if (null == password) {
      this.password = null;
    }

    this.password = new ShaPasswordEncoder().encodePassword(password, userName);
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
  
  public synchronized void addRole(final String role) {
    Set<String> updatedRoles = roles;
    if (null == updatedRoles) {
      updatedRoles = new HashSet<String>();
    }
    updatedRoles.add(role);
    if (null == roles) {
      roles = updatedRoles;
    }
  }
  
  public synchronized boolean removeRole(final String role) {
    if (null == roles ||
        0 == roles.size()) {
      return false;
    }
    
    return roles.remove(role);
  }

  public synchronized Set<String> getRoles() {
    if (null == roles) {
      return Collections.emptySet();
    }
    return Collections.unmodifiableSet(roles);
  }

  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("[");
    result.append(id).append(";");
    result.append(confirmed).append(";");
    result.append(userName).append(";");
    // password left out for security concerns
    result.append(email).append(";");
    result.append(firstName).append(";");
    result.append(lastName).append(";");
    result.append("[");
    if (roles != null && roles.size() > 0) {
      for (String role : roles) {
        result.append(role);
        result.append(";");
      }
    }
    result.append("]");
    result.append("]");
    return result.toString();
  }
}