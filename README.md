# Context: 

spring-boot-starter-parent 3.4.1

# The problem:

The sample project provides a SOAP web service via SpringWS. It uses some XSDs organized as follows:

- /xsd/common.xsd
- /xsd/order/order.xsd (imports "../common.xsd")
- /xsd/order/order-webservice.xsd (imports "order.xsd")

Since multiple XSDs are responsible for the final web service, we configure the ```WsdlDefinition``` with a ```CommonXsdSchemaCollection``` which should resolve all used XSDs.
This works very well as long as no relative import of “../common.xsd” is used in our “order.xsd”.

In that case the ```ClasspathUriResolver``` of ```CommonXsdSchemaCollection``` fails with:

```java.lang.IllegalArgumentException: The resource path [/../common.xsd] has been normalized to [null] which is not valid```

# The cause

The problem lies in the following code snippet of ```CommonXsdSchemaCollection::ClasspathUriResolver```:

```
Resource resources = resourcesLoader.getResource(schemaLocation);
if (resource.exists()) {
   return createInputSource(resource);
}
```

If schemaLocation is "../common.xsd" then resource.exists() fails with the above exception thrown by internally RequestUtils.normalize("/../common.xsd").

# About this sample project

The current state of this project will throw the exception when starting the application:

```
mvn clean install
mvn spring-boot:run
```

We have implemented a workaround in ```SpringWsConfig:90``` (```FixedClasspathUriResolver``` class) which can be activated in ```SpringWsConfig:74```. This demonstrates that the fallback to resolving the path relatively works like a charm!
It solves this problem by catching the exception and executing the else section of the if statement which then then manages to resolve the storage location relatively.

There is also a ```TomcatRequestUtilTest``` that demonstrates the behavior of the RequestUtil class which is used deep inside the call of ```resource.exists()```

