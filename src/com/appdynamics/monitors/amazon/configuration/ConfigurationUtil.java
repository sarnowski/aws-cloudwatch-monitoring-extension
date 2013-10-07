package com.appdynamics.monitors.amazon.configuration;

import com.amazonaws.auth.BasicAWSCredentials;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Iterator;

public class ConfigurationUtil {

    public static final String METRIC_TAG = "Metric";
    public static final String NAMESPACE_TAG = "namespace";
    public static final String METRIC_NAME_TAG = "metricName";
    public static final String ACCESS_KEY_TAG = "AccessKey";
    public static final String SECRET_KEY_TAG = "SecretKey";

    /**
     * Reads the config file in the conf/ directory and retrieves AWS credentials, disabled metrics, and available namespaces
     * @param filePath          Path to the configuration file
     * @return Configuration    Configuration object containing AWS credentials, disabled metrics, and available namespaces
     */
    public static Configuration getConfigurations(String filePath) {
        Configuration awsConfiguration = new Configuration();
        String accessKey= "";
        String secretKey= "";

        try {

            XMLInputFactory inputFactory = XMLInputFactory.newInstance();

            // Setup a new eventReader
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(filePath));
            XMLEventReader eventReader = inputFactory.createXMLEventReader(in);

            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                //reach the start of an item
                if (event.isStartElement()) {

                    StartElement startElement = event.asStartElement();

                    if (startElement.getName().getLocalPart().equals(METRIC_TAG)) {

                        // attribute
                        Iterator<Attribute> attributes = startElement.getAttributes();
                        String namespace="";
                        String metricName="";
                        while (attributes.hasNext()) {
                            Attribute attribute = attributes.next();

                            String attributeName = attribute.getName().toString();
                            if (attributeName.equals(NAMESPACE_TAG)) {
                                namespace = attribute.getValue();
                            }
                            if (attributeName.equals(METRIC_NAME_TAG)) {
                                metricName = attribute.getValue();
                            }
                        }
                        if (!awsConfiguration.disabledMetrics.containsKey(namespace)) {
                            awsConfiguration.disabledMetrics.put(namespace, new HashSet<String>());
                        }
                        ((HashSet)awsConfiguration.disabledMetrics.get(namespace)).add(metricName);
                    }

                    // data
                    if (event.isStartElement()) {
                        String tagName = event.asStartElement().getName().getLocalPart();
                        event = eventReader.nextEvent();
                        if(tagName.equals(ACCESS_KEY_TAG)) {
                            accessKey = event.toString();
                        }
                        if(tagName.equals(SECRET_KEY_TAG)) {
                            secretKey = event.toString();
                        }
                        if(tagName.equals(NAMESPACE_TAG)) {
                            String namespace = event.toString();
                            if (!awsConfiguration.availableNamespaces.contains(namespace)) {
                                awsConfiguration.availableNamespaces.add(namespace);
                            }
                        }
                    }
                }

                //reach the end of an item
                if (event.isEndElement()) {
                    EndElement endElement = event.asEndElement();
                }
            }
            awsConfiguration.awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
        }
        return awsConfiguration;
    }
}