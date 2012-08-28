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
 *     mkeith - Gemini JPA sample 
 ******************************************************************************/
package client;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import model.configadmin.Library;
import model.configadmin.Book;

/**
 * Gemini JPA sample client class
 * 
 * @author mkeith
 */
public class LibraryClient {
    
    public void run(EntityManagerFactory emf) {
        System.out.println("Gemini JPA Sample Library client - creating library...");
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        
        Library lib = new Library("Springfield Memorial Library");
        Book b1 = new Book("978-1-59059-645-6","Pro EJB 3", "Mike Keith");
        lib.addBook(b1);
        Book b2 = new Book("978-1-4302-1956-9","Pro JPA 2", "Mike Keith");
        lib.addBook(b2);
        em.persist(lib);

        em.getTransaction().commit();

        System.out.println("\nLibrary: " + em.find(Library.class, lib.getId()));
        System.out.println("\nList of books: ");

        TypedQuery<Book> q = em.createQuery("SELECT b FROM Book b", Book.class);
        List<Book> results = q.getResultList();
        for (Book b : results)
            System.out.println(" " + b.toString());
        em.close();
    }
}