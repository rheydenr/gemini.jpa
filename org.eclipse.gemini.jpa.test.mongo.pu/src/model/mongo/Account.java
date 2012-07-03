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
package model.mongo;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

import javax.persistence.*;

import org.eclipse.persistence.nosql.annotations.*;

/**
 * Test JPA model class
 * 
 * @author mkeith
 */
@Entity
@NoSql(dataFormat=DataFormatType.MAPPED)
public class Account {

    @Id @GeneratedValue 
	@Field(name="_id")
	String id;
    
    double balance;
    
/*    @OneToOne(mappedBy="account")
    Customer customer;

    @OneToMany(mappedBy="account")
    @OrderBy("txTime")
    List<Transaction> txns;
    
    @Temporal(TemporalType.DATE)
    public Date dateCreated;
*/

    /* Constructors */
    public Account() { 
//        dateCreated = new Date(System.currentTimeMillis()); 
        balance = 5;
    }
/*    public Account(Customer customer) {
        this();
        this.balance = 0;
        this.customer = customer;
        customer.setAccount(this);
        this.txns = new ArrayList<Transaction>();
    }
*/    
    /* Getters and setters */
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
/*
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    
    public List<Transaction> getTxns() { return txns; }
    public void setTxns(List<Transaction> txns) { this.txns = txns; }
*/   
    public String toString() {
//        return "Account(" + id + ", " + ((customer!=null)?customer.getLastName():"null") + ", Balance: $" + balance + ")";
        return "Account(" + id + ", Balance: $" + balance + ")";
    }
}
