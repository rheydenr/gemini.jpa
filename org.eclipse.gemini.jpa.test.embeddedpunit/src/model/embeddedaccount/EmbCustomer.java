/*******************************************************************************
 * Copyright (c) 2010 Oracle.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution. 
 * The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 * and the Apache License v2.0 is available at 
 *     http://www.opensource.org/licenses/apache2.0.php.
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *     mkeith - Gemini JPA tests 
 ******************************************************************************/
package model.embeddedaccount;

import javax.persistence.*;

/**
 * Test JPA model class
 * 
 * @author mkeith
 */
@Entity
public class EmbCustomer {
    @Id @GeneratedValue
    int id;
    
    @Column(name="LNAME")
    String lastName;
    
    @Column(name="FNAME")
    String firstName;

    @Column(name="ADDR")
    String address;

    @OneToOne
    EmbAccount account;

    /* Constructors */
    public EmbCustomer() { super(); }
    public EmbCustomer(String lastName, String firstName, String address) {
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

    public EmbAccount getAccount() { return account; }
    public void setAccount(EmbAccount account) { this.account = account; }
    
    public String toString() {
        return "EmbCustomer(" + firstName + " " + lastName + ", " + address + ")";
    }
}
