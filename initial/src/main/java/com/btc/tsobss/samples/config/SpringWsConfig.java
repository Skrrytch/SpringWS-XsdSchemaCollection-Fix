package com.btc.tsobss.samples.config;

import com.btc.tsobss.samples.endpoint.OrderEndpoint;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.ws.commons.schema.resolver.DefaultURIResolver;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.StringUtils;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.WsdlDefinition;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.sax.SaxUtils;
import org.springframework.xml.xsd.XsdSchemaCollection;
import org.springframework.xml.xsd.commons.CommonsXsdSchemaCollection;
import org.springframework.xml.xsd.commons.CommonsXsdSchemaException;
import org.xml.sax.InputSource;

/**
 * Spring WS uses a different servlet type for handling SOAP messages: MessageDispatcherServlet.
 * It is important to inject and set ApplicationContext to MessageDispatcherServlet. Without that,
 * Spring WS will not automatically detect Spring beans.
 * <p>
 * Naming this bean messageDispatcherServlet does not replace Spring Boot’s default DispatcherServlet bean.
 * <p>
 * DefaultMethodEndpointAdapter configures the annotation-driven Spring WS programming model.
 * This makes it possible to use the various annotations, such as @Endpoint (mentioned earlier).
 * <p>
 * DefaultWsdl11Definition exposes a standard WSDL 1.1 by using XsdSchema
 */
@EnableWs
@Configuration
public class SpringWsConfig extends WsConfigurerAdapter {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = OrderEndpoint.NAME)
    public WsdlDefinition serviceWsdl(XsdSchemaCollection orderWebserviceSchemaCollection) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("OrderPort");
        wsdl11Definition.setLocationUri("/ws/" + OrderEndpoint.NAME);
        wsdl11Definition.setTargetNamespace(OrderEndpoint.ROOT_ELEMENT_NAMESPACE);
        wsdl11Definition.setSchemaCollection(orderWebserviceSchemaCollection);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchemaCollection orderWebserviceSchemaCollection(ResourcePatternResolver resourceResolver) {
        return schemaCollectionOf(resourceResolver, "xsd/order/order-webservice.xsd");
    }

    private XsdSchemaCollection schemaCollectionOf(ResourcePatternResolver resourceResolver, String... schemas) {
        List<ClassPathResource> schemaList = Arrays.stream(schemas).map(ClassPathResource::new).toList();
        CommonsXsdSchemaCollection collection = new CommonsXsdSchemaCollection(schemaList.toArray(new ClassPathResource[]{}));

        // Fix issue with import of "../common.xsd" resulting in a java.lang.IllegalArgumentException: "The resource path [/../common.xsd] has been normalized to [null] which is not valid"
        // collection.setUriResolver(new FixedClasspathUriResolver(resourceResolver));

        collection.setInline(true);
        return collection;
    }

    /**
     * This implementation of DefaultURIResolver fixes an issue with the default resolver of CommonsXsdSchemaCollection:
     * <p>
     * When importing XSDs relatively (e.g. "../common.xsd") the call to resource.exists() throws the following exception:
     * <p>
     * java.lang.IllegalArgumentException: The resource path [/../common.xsd] has been normalized to [null] which is not valid
     * <p>
     * The default implementation supports such a use case but expects that resource.exists() returns false instead.
     * Therefor we implemented our own exists methode to fix this issue.
     */
    private static class FixedClasspathUriResolver extends DefaultURIResolver {

        private final ResourcePatternResolver resourceResolver;

        public FixedClasspathUriResolver(ResourcePatternResolver resourceResolver) {
            this.resourceResolver = resourceResolver;
        }

        @Override
        public InputSource resolveEntity(String namespace, String schemaLocation, String baseUri) {
            if (resourceResolver != null) {
                // schema location = "../common.xsd" (relative)
                Resource resource = resourceResolver.getResource(schemaLocation); // try absolute first
                // resource path = "/../common.xsd" => exists uses RequestUtil.normalize(path) which results to null!!!
                if (exists(resource)) { // in this case: false
                    return createInputSource(resource);
                } else if (StringUtils.hasLength(baseUri)) {
                    // let's try and find it relative to the baseUri, see SWS-413
                    try {
                        Resource baseUriResource = new UrlResource(baseUri);
                        resource = baseUriResource.createRelative(schemaLocation);
                        if (resource.exists()) {
                            return createInputSource(resource);
                        }
                    } catch (IOException e) {
                        // fall through
                    }
                }
                // let's try and find it on the classpath, see SWS-362
                String classpathLocation = ResourceLoader.CLASSPATH_URL_PREFIX + "/" + schemaLocation;
                resource = resourceResolver.getResource(classpathLocation);
                if (exists(resource)) {
                    return createInputSource(resource);
                }
            }
            return super.resolveEntity(namespace, schemaLocation, baseUri);
        }

        private boolean exists(Resource resource) {
            try {
                // in case of xsd-import of "../common.xsd" the resource points to "/../common.xsd"
                return resource.exists();
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        private InputSource createInputSource(Resource resource) {
            try {
                return SaxUtils.createInputSource(resource);
            } catch (IOException ex) {
                throw new CommonsXsdSchemaException("Could not resolve location", ex);
            }
        }
    }
}
