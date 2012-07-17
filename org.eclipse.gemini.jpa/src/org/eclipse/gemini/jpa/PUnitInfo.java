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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.eclipse.gemini.jpa.provider.OSGiJpaProvider;
import org.eclipse.gemini.jpa.proxy.EMFBuilderServiceProxyHandler;
import org.eclipse.gemini.jpa.proxy.EMFServiceProxyHandler;
import org.eclipse.gemini.jpa.xml.PersistenceDescriptorHandler;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

@SuppressWarnings({"rawtypes"})
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
     * @see GeminiServicesUtil
     */
    EMFServiceProxyHandler emfHandler;
    ServiceRegistration emfService;

    /** 
     * EMF Builder Service state - set by servicesUtil
     * @see GeminiServicesUtil
     */
    EMFBuilderServiceProxyHandler emfBuilderHandler;
    ServiceRegistration emfBuilderService;

    /** 
     * Shared EMF - set by EMF[Builder]ServiceProxyHandler
     *              unset by servicesUtil
     * @see EMFServiceProxyHandler
     * @see EMFBuilderServiceProxyHandler
     * @see GeminiServicesUtil
     */
    EntityManagerFactory emf;

    /** 
     * DataSourceFactory service used to indicate that a data source factory service was found and can be used
     *          - set by services util
     * @see GeminiServicesUtil
     */
    ServiceReference dsfService;

    /** 
     * Flag to indicate whether the EMF was set by the Builder or not
     *      - set by EMFServiceProxyHandler or EMFBuilderServiceProxyHandler
     * @see EMFServiceProxyHandler
     * @see EMFBuilderServiceProxyHandler
     */
    boolean emfSetByBuilderService;

    /** 
     * For tracking the data source factory - set by servicesUtil
     * @see GeminiServicesUtil 
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
    String driverVersion;

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

    public EntityManagerFactory getEmf() { return emf; }
    public void setEmf(EntityManagerFactory emf) { this.emf = emf; }
    
    public ServiceReference getDsfService() { return dsfService; }
    public void setDsfService(ServiceReference dsfService) { this.dsfService = dsfService; }

    public boolean isEmfSetByBuilderService() { return emfSetByBuilderService; }
    public void setEmfSetByBuilderService(boolean flag) { emfSetByBuilderService = flag; }
    
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
    
    public String getDriverVersion() { return driverVersion; }
    public void setDriverVersion(String driverVersion) { this.driverVersion = driverVersion; }

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
          .append((getProvider()!=null) ? "\n  provider: " + getProvider() : "")
          .append((getClasses()!=null) ? "\n  classes: " + getClasses() : "")
          .append((getDriverClassName()!=null) ? "\n  driverClassName: " + getDriverClassName() : "")
          .append((getDriverUrl()!=null) ? "\n  driverUrl: " + getDriverUrl() : "")
          .append((getDriverUser()!=null) ? "\n  driverUser: " + getDriverUser() : "")
          .append((getDriverPassword()!=null) ? "\n  driverPassword: " + getDriverPassword() : "")
          .append((getDriverVersion()!=null) ? "\n  driverVersion: " + getDriverVersion() : "")
          .append("\n  --- Runtime Data ---")
          .append((getBundle()!=null) ? "\n  bundle: " + getBundle().getSymbolicName() : "")
          .append((getAssignedProvider()!=null) ? "\n  assignedProvider: " + getAssignedProvider() : "")
          .append((getDescriptorInfo()!=null) ? "\n  descriptorInfo: " + getDescriptorInfo() : "")
          .append((getTracker()!=null) ? "\n  DSF tracker: " + getTracker() : "");
       return sb.toString();
    }
    
    public Map<String,Object> toMap() {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("unitName", unitName);
        map.put("provider", provider);
        map.put("classes", classes);
        map.put("driverClassName", driverClassName);
        map.put("driverUrl", driverUrl);
        map.put("driverUser", driverUser);
        map.put("driverUser", driverUser);
        map.put("driverPassword", driverPassword);
        map.put("driverVersion", driverVersion);
        map.put("bundle", bundle);
        map.put("assignedProvider", assignedProvider);
        map.put("emfSetByBuilderService", emfSetByBuilderService);
        map.put("descriptorInfo", descriptorInfo);
        return map;
    }
}