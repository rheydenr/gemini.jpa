/*
 * Copyright (C) 2010 Oracle Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package model.account;

import javax.persistence.*;

/**
 * Test JPA model class
 * 
 * @author mkeith
 */
@Entity
public class Customer {
    @Id @GeneratedValue
    int id;
    
    @Column(name="LNAME")
    String lastName;
    
    @Column(name="FNAME")
    String firstName;

    @Column(name="ADDR")
    String address;

    @OneToOne
    Account account;

    /* Constructors */
    public Customer() { super(); }
    public Customer(String lastName, String firstName, String address) {
        super();
        this.lastName = lastName;
        this.firstName = firstName;
        this.address = address;
    }

    /* Getters and setters */
    public int getId() { return id; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    
    public String toString() {
        return "Customer(" + firstName + " " + lastName + ", " + address + ")";
    }
}
