package com.appdynamics.monitors.cloudwatch.configuration;

import com.amazonaws.auth.BasicAWSCredentials;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.HashSet;

public class ConfigurationUtil {

    /**
     * Reads the config file in the conf/ directory and retrieves AWS credentials, disabled metrics, and available namespaces
     * @param filePath          Path to the configuration file
     * @return Configuration    Configuration object containing AWS credentials, disabled metrics, and available namespaces
     */
    public static Configuration getConfigurations(String filePath) throws Exception{
        Configuration awsConfiguration = new Configuration();
        BufferedInputStream configFile = null;

        try {
            configFile = new BufferedInputStream(new FileInputStream(filePath));
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(configFile);

            // Initialize AmazonCloudWatch
            Element credentialsFromFile = (Element)doc.getElementsByTagName("AWSCredentials").item(0);
            String accessKey = credentialsFromFile.getElementsByTagName("AccessKey").item(0).getTextContent();
            String secretKey = credentialsFromFile.getElementsByTagName("SecretKey").item(0).getTextContent();
            awsConfiguration.awsCredentials = new BasicAWSCredentials(accessKey, secretKey);

            // Initialize Namespaces
            Element namespacesElement = (Element)doc.getElementsByTagName("SupportedNamespaces").item(0);
            NodeList namespaces = namespacesElement.getElementsByTagName("SupportedNamespace");

            for (int i = 0; i < namespaces.getLength(); i++) {
                String namespace = namespaces.item(i).getTextContent();
                if (!awsConfiguration.availableNamespaces.contains(namespace)) {
                  awsConfiguration.availableNamespaces.add(namespaces.item(i).getTextContent());
                }
            }

            //Initialize Disabled Metrics
            Element disabledMetricsElement = (Element) doc.getElementsByTagName("DisabledMetrics").item(0);
            NodeList disabledMetricsList = disabledMetricsElement.getElementsByTagName("Metric");
            for (int i = 0; i < disabledMetricsList.getLength(); i++) {
                String namespaceKey = disabledMetricsList.item(i).getAttributes().getNamedItem("namespace").getNodeValue();
                String metricName = disabledMetricsList.item(i).getAttributes().getNamedItem("metricName").getNodeValue();
                if (!awsConfiguration.disabledMetrics.containsKey(namespaceKey)) {
                  awsConfiguration.disabledMetrics.put(namespaceKey, new HashSet<String>());
                }
                ((HashSet)awsConfiguration.disabledMetrics.get(namespaceKey)).add(metricName);
            }
            return awsConfiguration;
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            configFile.close();
        }
    }
}