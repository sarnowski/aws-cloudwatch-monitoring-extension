package com.appdynamics.monitors.amazon;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.appdynamics.monitors.amazon.configuration.Configuration;
import com.appdynamics.monitors.amazon.configuration.ConfigurationUtil;
import com.appdynamics.monitors.amazon.metricsmanager.MetricsManager;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AmazonCloudWatchMonitor extends AManagedMonitor {

    private Logger logger = Logger.getLogger(this.getClass().getName());
    private static boolean isInitialized = false;

    private MetricsManagerFactory metricsManagerFactory;

    // The AWS Cloud Watch client that retrieves instance metrics by executing requests
    private AmazonCloudWatch awsCloudWatch;
    // This HashSet of disabled metrics is populated by reading the DisabledMetrics.xml file
    private HashMap<String,HashSet<String>> disabledMetrics;
    // This HashSet of available namespaces is populated by reading the AvailableNamespaces.xml file
    private HashSet<String> availableNamespaces;

    private AWSCredentials awsCredentials;

    private Configuration awsConfiguration;

    public AmazonCloudWatchMonitor() {
        logger.setLevel(Level.INFO);
        metricsManagerFactory = new MetricsManagerFactory(this);
    }

    /**
     * Initialize AWS credentials, disabled metrics, and supported namespaces
     * @param taskArguments
     * @return
     */
    public void initialize(Map<String,String> taskArguments) {
        if (!isInitialized) {
            long startTime = System.currentTimeMillis();
            awsConfiguration = ConfigurationUtil.getConfigurations(taskArguments.get("configurations"));
            awsCredentials = awsConfiguration.awsCredentials;
            awsCloudWatch = new AmazonCloudWatchClient(awsCredentials);
            disabledMetrics = awsConfiguration.disabledMetrics;
            availableNamespaces = awsConfiguration.availableNamespaces;
            long endTime = System.currentTimeMillis();
            printExecutionTime(startTime, endTime);
            isInitialized = true;
        }
    }

    /**
     * Main execution method that uploads the metrics to the AppDynamics Controller
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    @Override
    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) {
        logger.info("Executing AmazonMonitor...");
        initialize(taskArguments);
        logger.info("AmazonMonitor initialized");
        final Iterator namespaceIterator = availableNamespaces.iterator();
        final CountDownLatch latch = new CountDownLatch(availableNamespaces.size());
        while (namespaceIterator.hasNext()) {
            final String namespace = (String) namespaceIterator.next();
            Thread metricManagerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    MetricsManager metricsManager = metricsManagerFactory.createMetricsManager(namespace);
                    Object metrics = metricsManager.gatherMetrics();
                    logger.info("Gathered metrics for namespace: " + namespace + "       Size of metrics: " + ((HashMap) metrics).size());
                    metricsManager.printMetrics(metrics);
                    logger.info("Printed metrics for namespace: " + namespace + "       Size of metrics: " + ((HashMap) metrics).size());
                    latch.countDown();
                }
            });
            metricManagerThread.start();
        }
        try {
            latch.await();
            logger.info("All threads finished");
        }
        catch(InterruptedException e) {
            logger.error(e.getMessage());
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
    private String getMetricPrefix() {
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