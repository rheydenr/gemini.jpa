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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

/**
 * Sample JPA model class
 * (NOTE: Not a suitable design for a *real* library with lots of books!)
 * 
 * @author mkeith
 */
@Entity
public class Library {

    @Id @GeneratedValue
    int id;

    String name;
	
    @OneToMany(mappedBy="library", cascade=CascadeType.PERSIST)
    Map<String,Book> books;

    /* Constructors */
    protected Library() {} 
    public Library(String libName) { 
        this.name = libName;
        this.books = new HashMap<String,Book>();
    }
	    
    /* Getters and setters */
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getName() { return name; }
    public void setId(String name) { this.name = name; }

    public Map<String,Book> getBooks() { return books; }

    /* Methods to add/remove books */
    public void addBook(Book book) {
        this.getBooks().put(book.getIsbn(),book);
        book.setLibrary(this);
    }
	
    public boolean removeBook(Book book) {
        Book b = this.getBooks().remove(book.getIsbn());
        if (b != null) b.setLibrary(null);
        return b != null;
    }

    /* Queries over the books */
    public Book findBookByIsbn(String isbn) { 
        return this.getBooks().get(isbn);
    }
	
    public Collection<Book> findBooksByTitle(String title) { 
        Collection<Book> result = new ArrayList<Book>();
        for (Book b : this.getBooks().values()) {
            if (b.getTitle().equalsIgnoreCase(title))
                result.add(b);
        }
        return result;
    }

    public Collection<Book> findBooksByAuthor(String author) {
        Collection<Book> result = new ArrayList<Book>();
        for (Book b : this.getBooks().values()) {
            if (b.getAuthor().equalsIgnoreCase(author))
                result.add(b);
        }
        return result;
    }

    public String toString() {
        return "Library(" + name + ", " + books.size() + " books)";
    }
}
