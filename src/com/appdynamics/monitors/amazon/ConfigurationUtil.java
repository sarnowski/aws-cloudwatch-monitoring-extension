package com.appdynamics.monitors.amazon;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Iterator;

public class ConfigurationUtil {

    public static Configuration getConfigurations(String filePath) {
        Configuration awsConfiguration = new Configuration();
        String accessKey;
        String secretKey;

        //TODO: logic to handle reading config file using STAX parser
        // First create a new XMLInputFactory
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

                    if (startElement.getName().getLocalPart() == "Metric") {
                        System.out.println("--start of an item");
                        // attribute
                        Iterator<Attribute> attributes = startElement.getAttributes();
                        while (attributes.hasNext()) {
                            Attribute attribute = attributes.next();
                            String namespace;
                            String metricName;
                            if (attribute.getName().toString().equals("Namespace")) {
                                namespace = attribute.getValue();
                            }
                            if (attribute.getName().toString().equals("MetricName")) {

                            }

                        }
                    }

                    // data
                    if (event.isStartElement()) {
                        String tagName = event.asStartElement().getName().getLocalPart();
                        event = eventReader.nextEvent();
                        if(tagName.equals("AccessKey")) {
                            accessKey = event.toString();
                        }
                        if(tagName.equals("SecretKey")) {
                            secretKey = event.toString();
                        }
                        if(tagName.equals("Namespace")) {
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
                    if (endElement.getName().getLocalPart() == "item") {
                        System.out.println("--end of an item\n");
                    }
                }

            }
        }
        catch(Exception e) {
            System.out.println(e.getMessage());
        }

        return awsConfiguration;
    }
}
