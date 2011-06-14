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
 *     ssmith, tware - EclipseLink integration
 ******************************************************************************/
package org.eclipse.gemini.jpa.weaving;

import org.osgi.framework.Version;

/** 
 * An interface used to register a byte code weaving service that
 * would typically wrap a ClassTransformer.
 * 
 * @see javax.persistence.spi.ClassTransormer
 * 
 * @author shsmith
 */
public interface IWeaver {
	byte[] transform(String className, String bundleId, Version bundleVersion, byte[] classfileBuffer);
}

