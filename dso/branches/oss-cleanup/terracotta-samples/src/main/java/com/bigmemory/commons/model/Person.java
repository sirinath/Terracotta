/*
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.bigmemory.commons.model;

import java.io.Serializable;

public class Person implements Serializable {

  private static final long serialVersionUID = 1L;
  private final int age;
  private final String name;
  private final Gender gender;
  private final Address address;

  public Person(String name, int age, Gender gender, String street, String state, String zip) {
    this.name = name;
    this.age = age;
    this.gender = gender;
    this.address = new Address(street, state, zip);
  }

  public int getAge() {
    return age;
  }

  public String getName() {
    return name;
  }

  public Gender getGender() {
    return gender;
  }

  public enum Gender {
    MALE, FEMALE;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "(name:" + name + ", age:" + age + ", sex:" + gender.name().toLowerCase() + ")";
  }

  public Address getAddress() {
    return address;
  }

}