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
 *     mkeith - Gemini JPA work 
 ******************************************************************************/
package org.eclipse.gemini.jpa.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;

import javax.persistence.EntityManagerFactory;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceFactory;
import org.osgi.framework.ServiceRegistration;

import org.eclipse.gemini.jpa.PUnitInfo;

import static org.eclipse.gemini.jpa.GeminiUtil.*;

/**
 * Dynamic proxy class to proxy the EMF service
 */
public class EMFServiceProxyHandler implements InvocationHandler, ServiceFactory {
    
    PUnitInfo pUnitInfo;
    EntityManagerFactory emf;
    
    public EMFServiceProxyHandler(PUnitInfo pUnitInfo) { this.pUnitInfo = pUnitInfo; }

    // May be called by the EMFBuilder service
    public EntityManagerFactory getEMF() { return emf; }
    public void setEMF(EntityManagerFactory factory) { emf = factory; }
    
    /*=========================*/
    /* InvocationProxy methods */
    /*=========================*/
    
    // Will only get calls for the methods on the EntityManagerFactory interface
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        debug("EMFProxy invocation on method ", method.getName());

        if (method.getName().equals("hashCode"))
            return this.hashCode();
        
        // ***NOTE: What if the provider supports multiple EMFs?
        // Should we ignore the cache and just call the provider each time?
        synchronized(this) {
            if (emf == null) {
                emf = pUnitInfo.getAssignedProvider().getProviderInstance()
                        .createEntityManagerFactory(pUnitInfo.getUnitName(), new HashMap<String,String>());
                if (emf == null) {
                    fatalError("EMFService could not create EMF " + 
                                            pUnitInfo.getUnitName() + " from provider", null);
                }
            } 
        }
        // Invoke the EMF method that was called
        Object result = method.invoke(emf,args);
        
        // If the operation was to close the EMF then remove our ref to it
        synchronized(this) {
            if (!emf.isOpen()) 
                emf = null;
        }
        return result;
    }

    /*========================*/
    /* ServiceFactory methods */
    /*========================*/

    public Object getService(Bundle b, ServiceRegistration serviceReg) {
        // TODO Track client bundles that use this service and clean up if they leave
        return this;
    }
    
    public void ungetService(Bundle b, ServiceRegistration serviceReg, Object obj) {
        // EMF is shared, leave as is until the p-unit or the provider goes away
        // and the service is unregistered
    }
}        

