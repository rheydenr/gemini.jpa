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
package org.eclipse.gemini.jpa;

/**
 * Gemini-specific persistence unit properties that can be configured 
 * in the persistence descriptor or passed into an EMF creation method.
 */
public class GeminiPersistenceUnitProperties {
    
    /*==================*/
    /* Static constants */
    /*==================*/

    // The presence of this property will imply that Gemini JPA will *not* look for the data source, but 
    // assume that the provider will connect to it directly. (Note that no property value is required or processed.)
    // This property should only be used if the provider can access/load the data source [driver] classes,
    // or the data source is natively supported by the provider and can be connected to without having to load 
    // additional classes. The connection properties needed by the provider must be supplied either in the
    // persistence descriptor or passed at runtime. (JDBC properties not required)
    public static String PROVIDER_CONNECTED_DATA_SOURCE = "gemini.jpa.providerConnectedDataSource";

}