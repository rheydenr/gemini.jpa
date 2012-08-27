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
 *     tware - initial implementation
 ******************************************************************************/
package org.eclipse.gemini.jpa.eclipselink;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;

import org.eclipse.persistence.internal.jpa.deployment.ArchiveFactoryImpl;
import org.eclipse.persistence.internal.jpa.deployment.JarInputStreamURLArchive;
import org.eclipse.persistence.jpa.Archive;

/**
 * Subclass of EclipseLink's ArchiveFactoryImpl
 * This subclass allows construction of a BundleArchive which can use the Bundle API
 * to extract information out of a persistence unit
 * @author tware
 *
 */
@SuppressWarnings({"rawtypes"})
public class OSGiArchiveFactoryImpl extends ArchiveFactoryImpl{

    @Override
    public Archive createArchive(URL rootUrl, String descriptorLocation, Map properties) throws URISyntaxException, IOException {
        logger.entering("ArchiveFactoryImpl", "createArchive", new Object[]{rootUrl, descriptorLocation});
        String protocol = rootUrl.getProtocol();
        logger.logp(Level.FINER, "ArchiveFactoryImpl", "createArchive", "protocol = {0}", protocol);
        
        if (properties != null && properties.get("org.eclipse.gemini.jpa.bundle") != null){
            if (isJarInputStream(rootUrl)){
                return new JarInputStreamURLArchive(rootUrl, descriptorLocation);
            } else {
                return new BundleArchive(rootUrl,properties, descriptorLocation, logger);
            }
        }
        return super.createArchive(rootUrl, descriptorLocation, properties);
    }
}
