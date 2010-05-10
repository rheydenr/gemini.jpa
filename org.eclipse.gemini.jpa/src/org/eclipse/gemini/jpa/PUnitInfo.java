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

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;


import org.eclipse.gemini.jpa.provider.OSGiJpaProvider;
import org.eclipse.gemini.jpa.proxy.EMFBuilderServiceProxyHandler;
import org.eclipse.gemini.jpa.proxy.EMFServiceProxyHandler;

public class PUnitInfo {
    
    /*===============*/
    /* Runtime state */
    /*===============*/
    
    /** 
     * Persistence unit bundle - set by extender
     * @see PersistenceBundleExtender 
     */
    Bundle bundle;

    /** 
     * The provider servicing this p-unit - set by extender
     * @see PersistenceBundleExtender 
     */
    OSGiJpaProvider assignedProvider;
    
    /** 
     * Info about the persistence descriptor this data came from - set by bundleUtil
     * @see PersistenceUnitBundleUtil
     */
    PersistenceDescriptorInfo descriptorInfo;

    /** 
     * Package names of all managed classes in the p-unit - set by bundleUtil
     * @see PersistenceUnitBundleUtil
     */
    List<String> uniquePackageNames;
    
    /** 
     * EMF Service state - set by servicesUtil
     * @see PersistenceServicesUtil
     */
    EMFServiceProxyHandler emfHandler;
    ServiceRegistration emfService;

    /** 
     * EMF Builder Service state - set by servicesUtil
     * @see PersistenceServicesUtil
     */
    EMFBuilderServiceProxyHandler emfBuilderHandler;
    ServiceRegistration emfBuilderService;

    /** 
     * For tracking the data source factory - set by servicesUtil
     * @see PersistenceServicesUtil 
     */
    ServiceTracker tracker;

    /*==============================*/
    /* Persistence descriptor state */
    /*==============================*/

    /**
     * All of following state is set by the XML parser 
     * @see PersistenceDescriptorHandler 
     */
    String unitName;
    String provider;
    Set<String> classes = new HashSet<String>();
    String driverClassName;
    String driverUrl;
    String driverUser;
    String driverPassword;

    /*=============================*/
    /* Accessors for runtime state */
    /*=============================*/
    
    public Bundle getBundle() { return bundle; }
    public void setBundle(Bundle b) { this.bundle = b; }
    
    public OSGiJpaProvider getAssignedProvider() { return assignedProvider; }
    public void setAssignedProvider(OSGiJpaProvider p) { this.assignedProvider = p; }

    public PersistenceDescriptorInfo getDescriptorInfo() { return descriptorInfo; }
    public void setDescriptorInfo(PersistenceDescriptorInfo info) { this.descriptorInfo = info; }

    public List<String> getUniquePackageNames() { return uniquePackageNames; }
    public void setUniquePackageNames(List<String> names) { this.uniquePackageNames = names; }

    public EMFServiceProxyHandler getEmfHandler() { return emfHandler; }
    public void setEmfHandler(EMFServiceProxyHandler emfHandler) { this.emfHandler = emfHandler; }

    public ServiceRegistration getEmfService() { return emfService; }
    public void setEmfService(ServiceRegistration emfService) { this.emfService = emfService; }

    public EMFBuilderServiceProxyHandler getEmfBuilderHandler() { return emfBuilderHandler; }
    public void setEmfBuilderHandler(EMFBuilderServiceProxyHandler emfBuilderHandler) { this.emfBuilderHandler = emfBuilderHandler; }

    public ServiceRegistration getEmfBuilderService() { return emfBuilderService; }
    public void setEmfBuilderService(ServiceRegistration emfBuilderService) { this.emfBuilderService = emfBuilderService; }

    public ServiceTracker getTracker() { return tracker; }
    public void setTracker(ServiceTracker tracker) { this.tracker = tracker; }
    
    /*============================================*/
    /* Accessors for Persistence descriptor state */
    /*============================================*/

    public String getUnitName() { return unitName; }
    public void setUnitName(String s) { this.unitName = s ; }

    public String getProvider() { return provider; }
    public void setProvider(String s) { this.provider = s; }

    public Set<String> getClasses() { return classes; }
    public void addClass(String s) { this.classes.add(s); }

    public String getDriverClassName() { return driverClassName; }
    public void setDriverClassName(String s) { driverClassName = s; }

    public String getDriverUrl() { return driverUrl; }
    public void setDriverUrl(String driverUrl) { this.driverUrl = driverUrl; }

    public String getDriverUser() { return driverUser; }
    public void setDriverUser(String driverUser) { this.driverUser = driverUser; }

    public String getDriverPassword() { return driverPassword; }
    public void setDriverPassword(String driverPassword) { this.driverPassword = driverPassword; }
    
    /*=========*/
    /* Methods */
    /*=========*/

    @Override
    public int hashCode() { return getUnitName().hashCode(); }
        
    @Override
    public boolean equals(Object o) { 
        return (o instanceof PUnitInfo)
            && (this.getUnitName() != null)
            && (this.getUnitName().equals(((PUnitInfo)o).getUnitName())); 
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\nPUnit: ").append(getUnitName())
          .append("\n  --- XML Data ---")
          .append("\n  provider: ").append(getProvider())
          .append("\n  classes: ").append(getClasses())
          .append("\n  driverClassName: ").append(getDriverClassName())
          .append("\n  driverUrl: ").append(getDriverUrl())
          .append("\n  driverUser: ").append(getDriverUser())
          .append("\n  driverPassword: ").append(getDriverPassword())
          .append("\n  --- Runtime Data ---")
          .append("\n  bundle: ").append(getBundle() == null ? "null" : getBundle().getSymbolicName())
          .append("\n  assignedProvider: ").append(getAssignedProvider())
          .append("\n  descriptorInfo: ").append(getDescriptorInfo())
          .append("\n  uniquePackageNames: ").append(getUniquePackageNames())
          .append("\n  emfHandler: ").append(getEmfHandler())
          .append("\n  emfBuilderHandler: ").append(getEmfBuilderHandler())
          .append("\n  DSF tracker: ").append(getTracker());
       return sb.toString();
    }
}