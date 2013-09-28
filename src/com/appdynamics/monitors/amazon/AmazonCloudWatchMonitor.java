package com.appdynamics.monitors.amazon;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class AmazonCloudWatchMonitor extends AManagedMonitor {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private static boolean isInitialized = false;

    private MetricsManagerFactory metricsManagerFactory;

    // The AWS Cloud Watch client that retrieves instance metrics by executing requests
    private AmazonCloudWatch awsCloudWatch;
    // This HashSet of disabled metrics is populated by reading the DisabledMetrics.xml file
    private HashMap<String,HashSet<String>> disabledMetrics = new HashMap<String,HashSet<String>>();
    // This HashSet of available namespaces is populated by reading the AvailableNamespaces.xml file
    private HashSet<String> availableNamespaces = new HashSet<String>();

    private AWSCredentials awsCredentials;

    public AmazonCloudWatchMonitor() {
        logger.setLevel(Level.INFO);
        metricsManagerFactory = new MetricsManagerFactory(this);
    }

    /**
     * Initialize AWS credentials, disabled metrics, and supported namespaces
     * @param taskArguments
     * @return
     */
    private void initialize(Map<String, String> taskArguments) {
        long startTime = System.currentTimeMillis();
        if (!isInitialized) {
            try {
                File configFile = new File(taskArguments.get("configurations"));
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
                isInitialized = true;
            }
            catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
        long endTime = System.currentTimeMillis();
        printExecutionTime(startTime, endTime);
    }


    /**
     * Main execution method that uploads the metrics to the AppDynamics Controller
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    @Override
    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) {
        logger.info("Executing AmazonMonitor...");
        initialize(taskArguments);
        //logger.info("AmazonMonitor initialized");
        Iterator namespaceIterator = availableNamespaces.iterator();
        while (namespaceIterator.hasNext()) {
            long startTime = System.currentTimeMillis();
            String namespace = (String) namespaceIterator.next();
            //logger.info("Processing metrics for namespace: " + namespace);
            MetricsManager metricsManager = metricsManagerFactory.createMetricsManager(namespace);
            //logger.info("Created metrics manager for namespace: " + namespace);
            Object metrics = metricsManager.gatherMetrics();
            //logger.info("Gathered metrics for namespace: " + namespace + "  Size of metrics: " + ((HashMap)metrics).size());
            metricsManager.printMetrics(metrics);
            logger.info("Printed metrics for namespace: " + namespace + "       Size of metrics: " + ((HashMap) metrics).size());
            long endTime = System.currentTimeMillis();
            printExecutionTime(startTime, endTime);
        }
        logger.info("Finished Executing AmazonMonitor...");

        return new TaskOutput("AWS Cloud Watch Metric Upload Complete");
    }

    public AmazonCloudWatch getAmazonCloudWatch() {
        return this.awsCloudWatch;
    }
    public HashMap<String,HashSet<String>> getDisabledMetrics() {
        return this.disabledMetrics;
    }
    public AWSCredentials getAWSCredentials() {
        return awsCredentials;
    }

    public boolean isMetricDisabled(String namespace, String metricName) {
        boolean result = false;
        if (disabledMetrics.get(namespace) != null) {
            if (disabledMetrics.get(namespace).contains(metricName)) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Returns the metric to the AppDynamics Controller.
     * @param   namespacePrefix Name of the Prefix
     * @param 	metricName		Name of the Metric
     * @param 	metricValue		Value of the Metric
     * @param 	aggregation		Average OR Observation OR Sum
     * @param 	timeRollup		Average OR Current OR Sum
     * @param 	cluster			Collective OR Individual
     */
    public void printMetric(String namespacePrefix, String metricName, double metricValue, String aggregation, String timeRollup, String cluster)
    {
        try{
            MetricWriter metricWriter = getMetricWriter(getMetricPrefix()  + namespacePrefix + metricName,
                    aggregation,
                    timeRollup,
                    cluster
            );

            metricWriter.printMetric(String.valueOf((long) metricValue));
        } catch (NullPointerException e){
            logger.info("NullPointerException: " + e.getMessage());
        }
    }

    /**
     * Metric Prefix
     * @return	String
     */
    private String getMetricPrefix()
    {
        return "Custom Metrics|Amazon Cloud Watch|";
    }

    private void printExecutionTime(long startTime, long endTime) {
        long executionTime = endTime - startTime;
        String formattedTime = String.format("%d min, %d sec",
                TimeUnit.MILLISECONDS.toMinutes(executionTime),
                TimeUnit.MILLISECONDS.toSeconds(executionTime) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(executionTime))
        );
        logger.info("   EXECUTION TIME: " + formattedTime);
    }
}


