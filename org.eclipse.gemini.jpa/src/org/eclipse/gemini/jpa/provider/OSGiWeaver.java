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
 *     ssmith - EclipseLink integration
 ******************************************************************************/
package org.eclipse.gemini.jpa.provider;

import java.lang.instrument.IllegalClassFormatException;

import javax.persistence.spi.ClassTransformer;

import org.eclipse.gemini.jpa.weaving.IWeaver;
import org.eclipse.persistence.logging.AbstractSessionLog;
import org.eclipse.persistence.logging.SessionLog;
import org.osgi.framework.Version;


/**
 * Provides a weaving wrapper for JPA on OSGi
 * 
 * @author ssmith
 * @author tware
 *
 */
public class OSGiWeaver implements IWeaver {
    private final ClassTransformer transformer;
    private String bundleName;
    private Version bundleVersion;

    OSGiWeaver(final ClassTransformer transformer, final String bundleName, final Version bundleVersion) {
        this.transformer = transformer;
        this.bundleName = bundleName;
        this.bundleVersion = bundleVersion;
    }

    public byte[] transform(String className, String bundleName, Version bundleVersion, byte[] classfileBuffer ) {
        // Only attempt to weave if the class originates in the bundle and version
        // that this weaver was built for.
        if (!(this.bundleName.equals(bundleName) && this.bundleVersion.equals(bundleVersion))) {
            return null;
        }
        try {
            byte[] transformedBytes = transformer.transform(null, className, null, null, classfileBuffer);
            if (transformedBytes != null) {
                AbstractSessionLog.getLog().log(SessionLog.FINER, className + " woven successfully");  // TODO NON-NLS 
            }
            return transformedBytes;
        } catch (IllegalClassFormatException e) {
            // TODO log appropriate warning
            e.printStackTrace();
            return null;
        }
    }
}
