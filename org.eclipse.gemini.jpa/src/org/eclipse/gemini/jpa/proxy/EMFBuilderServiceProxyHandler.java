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
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;


import org.eclipse.gemini.jpa.PUnitInfo;
import org.eclipse.gemini.jpa.proxy.EMFServiceProxyHandler;

import static org.eclipse.gemini.jpa.GeminiUtil.*;

/**
 * Dynamic proxy class to proxy the EMFBuilder service
 */
public class EMFBuilderServiceProxyHandler extends EMFServiceProxyHandler
                                           implements InvocationHandler {
            
    // We inherit an emf field and use that in the case when there is no EMF Service.
    // When there is an EMF Service then we use the emf field in the EMF service proxy stored below.
    
    // Hold onto the EMF service if it exists
    EMFServiceProxyHandler emfService;
    
    // Keep around a copy of the props used to create an EMF through the EMF builder
    Map<String,Object> emfProps = new HashMap<String,Object>();
            
    public EMFBuilderServiceProxyHandler(PUnitInfo pUnitInfo,
                                         EMFServiceProxyHandler emfService) {
        super(pUnitInfo);
        this.emfService = emfService;
    }

    /*=========================*/
    /* InvocationProxy methods */
    /*=========================*/

    // Will only get calls for the method on the EntityManagerFactoryBuilder interface
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        debug("EMFBuilderProxy invocation on method ", method.getName());

        if (method.getName().equals("hashCode"))
            return this.hashCode();

        // Must be a createEntityManagerFactory(String, Map) call

        // If we have an EMF and it has already been closed, discard it
        synchronized (this) {
            if ((emf != null) && (!emf.isOpen()))
                emf = null;
    
            // If we have a local factory, return it
            if (emf != null) 
                return emf;
        }
        // The first arg must be the properties Map
        Map<String,Object> props = (Map<String,Object>)args[0];

        // If an EMF service is registered the EMF must be stored there
        if (emfService != null) {

            // Verify the JDBC properties match the ones in the descriptor.
            verifyJDBCProperties(pUnitInfo.getDriverClassName(), 
                                 pUnitInfo.getDriverUrl(), 
                                 props);
            
            // Verify the JDBC properties match the ones previously passed in.
            verifyJDBCProperties((String) emfProps.get(JPA_JDBC_DRIVER_PROPERTY), 
                                 (String) emfProps.get(JPA_JDBC_URL_PROPERTY), 
                                 props);

            // Synchronize to ensure we share the same factory
            synchronized(emfService) {
                
                // If EMF service has one that is closed then discard it
                if ((emfService.getEMF() != null) && (!emfService.getEMF().isOpen())) {
                    emfService.setEMF(null);
                    emfProps.clear();
                }
                // If it doesn't have one, then assign it one
                if (emfService.getEMF() == null) {
                    emfService.setEMF(createEMF(props));
                }      
                // Create a proxy to the EMF in the EMFService
                return Proxy.newProxyInstance(this.getClass().getClassLoader(),
                                              new Class[] { EntityManagerFactory.class },
                                              emfService);
            }
        } else {
            // No EMF service (data source was not active). Create our own EMF since we don't have one
            synchronized (this) {
                if (emf == null)
                    emf = createEMF(props);
            }
            return emf;
        }
    }

    /*================*/
    /* Helper methods */
    /*================*/
    
    protected EntityManagerFactory createEMF(Map<String,Object> props) {
        emfProps = props;
        return super.createEMF(props);
    }

    // Local method to compare properties passed in Map to ones in persistence descriptor or in previously set props
    protected void verifyJDBCProperties(String driver, String driverUrl, Map<String,Object> props) {

        if (driver != null) {
            String propDriver = (String) props.get(JPA_JDBC_DRIVER_PROPERTY);
            if ((propDriver != null) && !driver.equals(propDriver)) {
                throw new IllegalArgumentException();
            }
        }
        if (driverUrl != null) {
            String propUrl = (String) props.get(JPA_JDBC_URL_PROPERTY);
            if ((propUrl != null) && !driverUrl.equals(propUrl)) {
                throw new IllegalArgumentException();
            }
        }
    }
}        
