
Gemini JPA 1.0.0 RC2 - July 6, 2011

This release candidate provides you with access to the EclipseLink JPA 2.0 provider. The 
EclipseLink 2.3.0 or later bundles must be used.

For an example of how to access an EntityManager from an OSGi program run the sample bundle.
To do this, install and start the bundles described in "Getting Started.txt" (included in this 
distribution) as well as the org.eclipse.gemini.jpa.samples bundle.

Bugs Addressed in RC2

The following bugs were addressed in this milestone:

344328	Changes to ASM code in EclipseLink 2.3 will cause compile failure in Gemini
338043	BundleException when persistence bundle refreshed
342942	Bug in PlainDriverDataSource isWrapperFor
330126	EMF service is not visible when punit has mapping files
342996	Persistence units from lazy bundles are not re-registered after bundle restart
337157	Add an option to not refresh bundles that are already deployed
