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
 *     mkeith - Gemini JPA tests 
 ******************************************************************************/
package punit;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Embedded Test JPA persistence unit activator class
 * 
 * @author mkeith
 */
public class Activator implements BundleActivator {

    public void start(BundleContext context) throws Exception {
        System.out.println("Embedded test persistence unit active");
    }

    public void stop(BundleContext context) throws Exception {}
}
