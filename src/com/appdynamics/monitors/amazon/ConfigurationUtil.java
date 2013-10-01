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

                    if (startElement.getName().getLocalPart() == "AccessKey") {
                        System.out.println("--start of an item");
                        // attribute
                        Iterator<Attribute> attributes = startElement.getAttributes();
                        while (attributes.hasNext()) {
                            Attribute attribute = attributes.next();
                            if (attribute.getName().toString().equals("id")) {
                                System.out.println("id = " + attribute.getValue());
                            }
                        }
                    }

                    // data
                    if (event.isStartElement()) {
                        String tagName = event.asStartElement().getName().getLocalPart();
                        if (tagName.equals("AccessKey")) {
                            event = eventReader.nextEvent();
                            accessKey = event.toString();
                        }
                        if (tagName.equals("SecretKey")) {

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
