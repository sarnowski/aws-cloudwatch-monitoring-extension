/** 
* Copyright 2013 AppDynamics 
* 
* Licensed under the Apache License, Version 2.0 (the License);
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an AS IS BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.appdynamics.extensions.cloudwatch;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.appdynamics.extensions.cloudwatch.configuration.Configuration;
import com.appdynamics.extensions.cloudwatch.configuration.ConfigurationUtil;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManager;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManagerFactory;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;

public final class AmazonCloudWatchMonitor extends AManagedMonitor {

    private Logger logger = Logger.getLogger("com.singularity.extensions.AmazonCloudWatchMonitor");
    private static boolean isInitialized = false;

    private MetricsManagerFactory metricsManagerFactory;
    private AmazonCloudWatch awsCloudWatch;
    private Map disabledMetrics;
    private Set availableNamespaces;
    private AWSCredentials awsCredentials;
    private Configuration awsConfiguration;

    public AmazonCloudWatchMonitor() {
    	String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
        logger.info(msg);
        System.out.println(msg);
        metricsManagerFactory = new MetricsManagerFactory(this);
    }

    /**
     * Initialize AWS credentials, disabled metrics, and supported namespaces
     * @param taskArguments
     * @return
     */
    public void initialize(Map<String,String> taskArguments) throws Exception{
        if (!isInitialized) {
            awsConfiguration = ConfigurationUtil.getConfigurations(taskArguments.get("configurations"));
            awsCredentials = awsConfiguration.awsCredentials;
            awsCloudWatch = new AmazonCloudWatchClient(awsCredentials);
            disabledMetrics = awsConfiguration.disabledMetrics;
            availableNamespaces = awsConfiguration.availableNamespaces;
            isInitialized = true;
            logger.info("AmazonMonitor initialized");
        }
    }

    /**
     * Main execution method that uploads the metrics to the AppDynamics Controller
     * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map, com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
     */
    public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) {
        try {
            logger.info("Executing CloudWatchMonitor...");
            initialize(taskArguments);

            final Iterator namespaceIterator = availableNamespaces.iterator();
            final CountDownLatch latch = new CountDownLatch(availableNamespaces.size());
            while (namespaceIterator.hasNext()) {
                final String namespace = (String) namespaceIterator.next();
                Thread metricManagerThread = new Thread(new Runnable() {
                    public void run() {
                        MetricsManager metricsManager = metricsManagerFactory.createMetricsManager(namespace);
                        Map metrics = metricsManager.gatherMetrics();
                        metricsManager.printMetrics(metrics);
                        logger.info(String.format("%15s: %30s %15s %5d" , "Namespace", namespace, " # Metrics",  metrics.size()));
                        latch.countDown();
                    }
                });
                metricManagerThread.start();
            }
            latch.await();
            logger.info("All threads finished");
            logger.info("Finished Executing CloudWatchMonitor...");
            return new TaskOutput("AWS Cloud Watch Metric Upload Complete Successfully");
        }
        catch(Exception e) {
            logger.error("Exception ", e);
            return new TaskOutput("AWS Cloud Watch Metric Upload Failed");
        }
    }

    /**
     * Get the Amazon Cloud Watch Client
     * @return	AmazonCloudWatch
     */
    public AmazonCloudWatch getAmazonCloudWatch() {
        return this.awsCloudWatch;
    }
    /**
     * Get the hashmap of disabled metrics
     * @return  HashMap
     */
    public Map getDisabledMetrics() {
        return this.disabledMetrics;
    }
    /**
     * Set the Amazon Cloud Watch Client
     */
    public void setAmazonCloudWatch(AmazonCloudWatch awsCloudWatch) {
        this.awsCloudWatch = awsCloudWatch;
    }
    /**
     * Set the hashmap of disabled metrics
     */
    public void setDisabledMetrics(Map disabledMetrics) {
        this.disabledMetrics = disabledMetrics;
    }
    /**
     * Get the AWS Credentials
     * @return	AWSCredentials
     */
    public AWSCredentials getAWSCredentials() {
        return awsCredentials;
    }
    /**
     * Check for disabled metrics in particular namespaces
     * @return	boolean
     */
    public boolean isMetricDisabled(String namespace, String metricName) {
        boolean result = false;
        if (disabledMetrics.get(namespace) != null) {
            if (((HashSet)disabledMetrics.get(namespace)).contains(metricName)) {
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
            logger.error("NullPointerException: ", e);
        }
    }

    /**
     * Metric Prefix
     * @return	String
     */
    private String getMetricPrefix() {
        return "Custom Metrics|Amazon Cloud Watch|";
    }
    
    public static String getImplementationVersion() {
        return AmazonCloudWatchMonitor.class.getPackage().getImplementationTitle();
    }
}