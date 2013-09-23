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
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.*;

public class AmazonCloudWatchMonitor extends AManagedMonitor {

    private static final String AWS_ACCESS_KEY = "accessKey";
    private static final String AWS_SECRET_KEY = "secretKey";
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private static boolean isInitialized = false;

    private MetricsManagerFactory metricsManagerFactory;

    // The AWS Cloud Watch client that retrieves instance metrics by executing requests
    private AmazonCloudWatch awsCloudWatch;
    // This HashSet of disabled metrics is populated by reading the DisabledMetrics.xml file
    private HashMap<String,HashSet<String>> disabledMetrics = new HashMap<String,HashSet<String>>();

    private HashSet<String> availableNamespaces = new HashSet<String>();

    private AWSCredentials awsCredentials;

    public AmazonCloudWatchMonitor() {

        metricsManagerFactory = new MetricsManagerFactory(this);
    }

    public void init(Map<String,String> taskArguments) {
        if (!isInitialized) {
            setDisabledMetrics(taskArguments.get("disabledMetricsFile"));
            initCloudWatchClient(taskArguments.get("awsCredentials"));
            setAvailableNamespaces(taskArguments.get("namespaces"));
            isInitialized = true;
        }
    }

    /**
     * Set list of disabled metrics from xml file
     * @return
     */
    private void setDisabledMetrics(String filePath) {
        try {
            FileInputStream disabledMetricsFile = new FileInputStream(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(disabledMetricsFile);
            NodeList nList = doc.getElementsByTagName("Metric");

            for (int i = 0; i < nList.getLength(); i++) {
                String namespaceKey = nList.item(i).getAttributes().getNamedItem("namespace").getNodeValue();
                String metricName = nList.item(i).getAttributes().getNamedItem("metricName").getNodeValue();
                if (!disabledMetrics.containsKey(namespaceKey)) {
                    disabledMetrics.put(namespaceKey, new HashSet<String>());
                }
                disabledMetrics.get(namespaceKey).add(metricName);
            }
        }
        catch(Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Initialize Amazon Cloud Watch Client
     * @return
     */
    private void initCloudWatchClient(String filePath) {
        Properties awsProperties = new Properties();
        try {
            awsProperties.load(new FileInputStream(filePath));
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        awsCredentials = new BasicAWSCredentials(awsProperties.getProperty(AWS_ACCESS_KEY), awsProperties.getProperty(AWS_SECRET_KEY));
        awsCloudWatch = new AmazonCloudWatchClient(awsCredentials);
    }

    private void setAvailableNamespaces(String filePath) {
        try {
            FileInputStream namespacesFile = new FileInputStream(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(namespacesFile);
            NodeList nList = doc.getElementsByTagName("Namespace");

            for (int i = 0; i < nList.getLength(); i++) {
                availableNamespaces.add(nList.item(i).getTextContent());
            }
        }
        catch(Exception e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Main execution method that uploads the metrics to the AppDynamics Controller
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    @Override
    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) {
        init(taskArguments);

        Iterator namespaceIterator = availableNamespaces.iterator();
        while (namespaceIterator.hasNext()) {
            String namespace = (String) namespaceIterator.next();
            MetricsManager metricsManager = metricsManagerFactory.createMetricsManager(namespace);
            Object metrics = metricsManager.gatherMetrics();
            metricsManager.printMetrics(metrics);
        }

        return new TaskOutput("AWS Cloud Watch Metric Upload Complete");
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

    public AmazonCloudWatch getAmazonCloudWatch() {
        return this.awsCloudWatch;
    }

    public HashMap<String,HashSet<String>> getDisabledMetrics() {
        return this.disabledMetrics;
    }
    public AWSCredentials getAWSCredentials() {
        return awsCredentials;
    }

    public GetMetricStatisticsRequest createGetMetricStatisticsRequest(String namespace,
                                                                        String metricName,
                                                                        String statisticsType,
                                                                        List<Dimension> dimensions) {
        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime(new Date(new Date().getTime() - 1000000000))
                .withNamespace(namespace)
                .withDimensions(dimensions)
                .withPeriod(60 * 60)
                .withMetricName(metricName)
                .withStatistics(statisticsType)
                .withEndTime(new Date());
        return getMetricStatisticsRequest;
    }
}


