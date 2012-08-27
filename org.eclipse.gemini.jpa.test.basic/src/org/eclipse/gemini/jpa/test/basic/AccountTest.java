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
package org.eclipse.gemini.jpa.test.basic;

import javax.persistence.*;

import model.account.Account;

import org.eclipse.gemini.jpa.test.common.JpaTest;

/**
 * Abstract Test class to for Account entity
 * 
 * @author mkeith
 */
public abstract class AccountTest extends JpaTest {

    /* === Methods that have been subclassed === */

    public Object newObject() {
        Account a = new Account();
        a.setBalance(100.0);
        return a;
    }

    public Object findObject() {
        EntityManager em = getEmf().createEntityManager();
        Object obj = em.find(Account.class, 1);
        em.close();
        return obj;
    }

    public String queryString() {
        return "SELECT a FROM Account a";
    }
}
