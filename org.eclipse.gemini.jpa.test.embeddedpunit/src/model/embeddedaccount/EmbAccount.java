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
package model.embeddedaccount;

import javax.persistence.*;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * Test JPA model class
 * 
 * @author mkeith
 */
@Entity
public class EmbAccount {

    @Id @GeneratedValue
    int id;
    
    double balance;
    
    @OneToOne(mappedBy="account")
    EmbCustomer customer;

    @OneToMany(mappedBy="account")
    @OrderBy("txTime")
    List<EmbTransaction> txns;
    
    @Temporal(TemporalType.DATE)
    public Date dateCreated;

    /* Constructors */
    public EmbAccount() { 
        dateCreated = new Date(System.currentTimeMillis()); 
    }
    public EmbAccount(EmbCustomer customer) {
        this();
        this.balance = 0;
        this.customer = customer;
        customer.setAccount(this);
        this.txns = new ArrayList<EmbTransaction>();
    }
    
    /* Getters and setters */
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public EmbCustomer getCustomer() { return customer; }
    public void setCustomer(EmbCustomer customer) { this.customer = customer; }
    
    public List<EmbTransaction> getTxns() { return txns; }
    public void setTxns(List<EmbTransaction> txns) { this.txns = txns; }
    
    public String toString() {
        return "EmbAccount(" + id + ", " + ((customer!=null)?customer.getLastName():"null") + ", Balance: $" + balance + ")";
    }
}
