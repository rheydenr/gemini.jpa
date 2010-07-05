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
import java.util.Calendar;


/**
 * Test JPA model class
 * 
 * @author mkeith
 */
@Entity
@Table(name="ACCT_TXN")
@NamedQuery(name="EmbTransaction.findAllSince", 
    query="SELECT t FROM EmbTransaction t WHERE t.account = :account AND t.txTime >= :dateArg")
public class EmbTransaction {

    @Id @GeneratedValue
    int id;

    @ManyToOne
    EmbAccount account;

    @Column(name="OP")
    EmbTxOperation operation;

    double amount;
    
    @Temporal(TemporalType.TIME)
    Date txTime;

    /* Constructors */
    public EmbTransaction() { super(); }
    public EmbTransaction(EmbAccount account, EmbTxOperation operation, double amount) {
        super();
        this.account = account;
        account.getTxns().add(this);
        this.operation = operation;
        this.amount = amount;
        this.txTime = Calendar.getInstance().getTime();
    }

    /* Getters and setters */
    public int getId() { return id; }
    
    public EmbAccount getAccount() { return account; }
    public void setAccount(EmbAccount account) { this.account = account; }
    
    public EmbTxOperation getOperation() { return operation; }
    public void setOperation(EmbTxOperation operation) { this.operation = operation; }
    
    public Date getTxTime() { return txTime; }
    public void setTxTime(Date txTime) { this.txTime = txTime; }
    
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    
    public String toString() {
        return "("+ txTime + " - " + "Acct#: " + account.getId() + " " + operation.toString() + ": " + amount + ")"; 
    }
}
