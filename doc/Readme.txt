
Gemini JPA Milestone M3 - Nov 18, 2010

This milestone provides you with access to the EclipseLink JPA 2.0 provider. The EclipseLink 2.2
or later bundles (or nightly builds/milestones after Nov 12, 2010) must be used.

For an example of how to access an EntityManager from an OSGi program run the sample program.
To do this, install and start the bundles described in "Getting Started.txt" included in this 
distribution. Additionally, install the org.eclipse.gemini.jpa.samples bundle.

Bug fixes in M3:

328072 	GeminiOSGiInitializer constructor is missing call to super - can cause NPE 
328133 	BundleProxyClassLoader.getResources() returns an Enumeration with a null element when called for non-existant resource 
329383 	Update Gemini to allow EclipseLink provider to discover classes
328568 	Classloading problems for javax.persistence classes
