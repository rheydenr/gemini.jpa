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
 *     mkeith - Gemini JPA Sample 
 ******************************************************************************/
package model.configadmin;

import javax.persistence.*;

/**
 * Gemini JPA Sample class
 * 
 * @author mkeith
 */
@Entity
public class Book {
    @Id
    String isbn;
    
    String author;
    String title;

    @ManyToOne
    Library library;

    /* Constructors */
    protected Book() {}

    public Book(String isbn, String title, String author) { 
        this.isbn = isbn;
		this.title = title;
		this.author = author;
    }

    /* Getters and setters */
    public String getIsbn() { return isbn; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public Library getLibrary() { return library; }
    public void setLibrary(Library lib) { this.library = lib; }
    
    public String toString() {
        return "Book(" + title + ", by " + author + ")";
    }
}
