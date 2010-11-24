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
 *     ssmith - Based on - http://wiki.eclipse.org/index.php/BundleProxyClassLoader_recipe
 ******************************************************************************/  
package org.eclipse.gemini.jpa.classloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;

public class BundleProxyClassLoader extends ClassLoader {

    private Bundle bundle;
    private ClassLoader parent;
    private EclipseDotClasspathHelper classpathHelper = new EclipseDotClasspathHelper();
        
    public BundleProxyClassLoader(Bundle bundle) {
        this.bundle = bundle;
    }
    
    public BundleProxyClassLoader(Bundle bundle, ClassLoader parent) {
        super(parent);
        this.parent = parent;
        this.bundle = bundle;
    }

    // Note: Both ClassLoader.getResources(...) and bundle.getResources(...) consult 
    // the boot classloader. As a result, BundleProxyClassLoader.getResources(...) 
    // might return duplicate results from the boot classloader. Prior to Java 5 
    // Classloader.getResources was marked final. If your target environment requires
    // at least Java 5 you can prevent the occurrence of duplicate boot classloader 
    // resources by overriding ClassLoader.getResources(...) instead of 
    // ClassLoader.findResources(...).
    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        try {
            if ((bundle.getState() == Bundle.INSTALLED) ||
                (bundle.getState() == Bundle.UNINSTALLED)){
                List<URL> resourceURLs = new ArrayList<URL>(1);
                URL entry = getEntry(name);
                if (entry != null){
                    resourceURLs.add(entry);
                }
                return new ListEnumeration(resourceURLs);
            } else  {
                return super.findResources(name);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public URL findResource(String name) {
        return bundle.getResource(name);
    }

    public Class findClass(String name) throws ClassNotFoundException {
        return bundle.loadClass(name);
    }
    
    public URL getResource(String name) {
        try {
            if ((bundle.getState() == Bundle.INSTALLED) ||
                (bundle.getState() == Bundle.UNINSTALLED)){
                URL entry = getEntry(name);
                return entry;
            } else {
                return (parent == null) ? findResource(name) : super.getResource(name);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected URL getEntry(String name) {
        URL entry = bundle.getEntry(name);
        if (entry == null) {
            entry = getEclipseProjectEntry(name, entry);
        }
        return entry;
    }

    protected URL getEclipseProjectEntry(String name, URL entry) {
        String binPath = classpathHelper.getBinPath(bundle);
        if (binPath != null) {
            entry = bundle.getEntry(binPath + File.separator + name);
        }
        return entry;
    }

    protected Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class clazz = (parent == null) ? findClass(name) : super.loadClass(name, false);
        if (resolve)
            super.resolveClass(clazz);
        
        return clazz;
    }
    
    private final class ListEnumeration implements Enumeration {
        private Iterator iterator;

        public ListEnumeration(List<?> list) {
            this.iterator = list.iterator();
        }

        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        public Object nextElement() {
            return iterator.next();
        }
    }

    @Override
    public String toString() {
    	return super.toString() + "(" + this.bundle.getSymbolicName() + ")";
    }

}

