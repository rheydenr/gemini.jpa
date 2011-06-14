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
 package org.eclipse.gemini.jpa.weaving.equinox;

import java.io.IOException;
import java.net.URLConnection;
import java.util.Properties;

import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

public class WeavingAdaptor implements AdaptorHook {

	public void frameworkStart(BundleContext context) throws BundleException {
		WeaverRegistry.getInstance().start(context);
	}

	public void frameworkStopping(BundleContext context) {
		WeaverRegistry.getInstance().stop(context);

	}

	public void addProperties(Properties properties) {
	}

	public FrameworkLog createFrameworkLog() {
		return null;
	}

	public void frameworkStop(BundleContext context) throws BundleException {
	}

	public void handleRuntimeError(Throwable error) {
	}

	public void initialize(BaseAdaptor adaptor) {
	}

	public URLConnection mapLocationToURLConnection(String location)
			throws IOException {
		return null;
	}

	public boolean matchDNChain(String pattern, String[] dnChain) {
		return false;
	}
}
