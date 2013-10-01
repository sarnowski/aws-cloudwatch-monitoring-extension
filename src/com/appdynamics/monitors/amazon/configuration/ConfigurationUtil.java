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

                    if (startElement.getName().getLocalPart() == "Metric") {

                        // attribute
                        Iterator<Attribute> attributes = startElement.getAttributes();
                        String namespace="";
                        String metricName="";
                        while (attributes.hasNext()) {
                            Attribute attribute = attributes.next();

                            String attributeName = attribute.getName().toString();
                            if (attributeName.equals("Namespace")) {
                                namespace = attribute.getValue();
                            }
                            if (attributeName.equals("MetricName")) {
                                metricName = attribute.getValue();
                            }
                        }
                        if (!awsConfiguration.disabledMetrics.containsKey(namespace)) {
                            awsConfiguration.disabledMetrics.put(namespace, new HashSet<String>());
                        }
                        awsConfiguration.disabledMetrics.get(namespace).add(metricName);
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

/*private void initialize(Map<String, String> taskArguments) {
    long startTime = System.currentTimeMillis();
    if (!isInitialized) {
        try {
            //File configFile = new File(taskArguments.get("configurations"));
            FileInputStream configFile = new FileInputStream(taskArguments.get("configurations"));
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);

            // Initialize AmazonCloudWatch
            Element credentialsFromFile = (Element)doc.getElementsByTagName("AWSCredentials").item(0);
            String accessKey = credentialsFromFile.getElementsByTagName("AccessKey").item(0).getTextContent();
            String secretKey = credentialsFromFile.getElementsByTagName("SecretKey").item(0).getTextContent();
            awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
            awsCloudWatch = new AmazonCloudWatchClient(awsCredentials);

            // Initialize Namespaces
            Element namespacesElement = (Element)doc.getElementsByTagName("SupportedNamespaces").item(0);
            NodeList namespaces = namespacesElement.getElementsByTagName("Namespace");

            for (int i = 0; i < namespaces.getLength(); i++) {
                String namespace = namespaces.item(i).getTextContent();
                if (!availableNamespaces.contains(namespace)) {
                    availableNamespaces.add(namespaces.item(i).getTextContent());
                }
            }

            //Initialize Disabled Metrics
            Element disabledMetricsElement = (Element) doc.getElementsByTagName("DisabledMetrics").item(0);
            NodeList disabledMetricsList = disabledMetricsElement.getElementsByTagName("Metric");
            for (int i = 0; i < disabledMetricsList.getLength(); i++) {
                String namespaceKey = disabledMetricsList.item(i).getAttributes().getNamedItem("namespace").getNodeValue();
                String metricName = disabledMetricsList.item(i).getAttributes().getNamedItem("metricName").getNodeValue();
                if (!disabledMetrics.containsKey(namespaceKey)) {
                    disabledMetrics.put(namespaceKey, new HashSet<String>());
                }
                disabledMetrics.get(namespaceKey).add(metricName);
            }
            configFile.close();
            isInitialized = true;
        }
        catch (Exception e) {
            logger.error(e.getMessage());
        }
    }
    long endTime = System.currentTimeMillis();
    printExecutionTime(startTime, endTime);
}*/