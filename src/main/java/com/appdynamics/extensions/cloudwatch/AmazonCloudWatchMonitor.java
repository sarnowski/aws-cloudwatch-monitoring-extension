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

import static com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManagerFactory.AWS_EC2_NAMESPACE;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.ArgumentsValidator;
import com.appdynamics.extensions.cloudwatch.configuration.Configuration;
import com.appdynamics.extensions.cloudwatch.configuration.ConfigurationUtil;
import com.appdynamics.extensions.cloudwatch.ec2.EC2InstanceNameManager;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricType;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManager;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManagerFactory;
import com.appdynamics.extensions.cloudwatch.metricsmanager.metricsmanagerimpl.CustomNamespaceMetricsManager;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;

import org.apache.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.*;

public class AmazonCloudWatchMonitor extends AManagedMonitor {

	private Logger logger = Logger.getLogger("com.singularity.extensions.AmazonCloudWatchMonitor");
	private boolean isInitialized = false;
    private ExecutorService awsWorkerPool;
    private ExecutorService awsMetricWorkerPool;

	private MetricsManagerFactory metricsManagerFactory;
	private Map<String, Set<String>> disabledMetrics;
	private Map<String, Map<String, MetricType>> metricTypes;
	private Set<String> availableNamespaces;
	private Set<String> availableRegions;
	private AWSCredentials credentials;
	private Configuration configuration;
	private String metric_prefix;
	private EC2InstanceNameManager ec2InstanceNameManager;
	private boolean useEc2InstanceNameInMetrics;

	private static final Map<String, String> regionVsURLs;

	static {
		Map<String, String> tmpRegionVsURLs = new HashMap<String, String>();

		tmpRegionVsURLs.put("ap-southeast-1", "monitoring.ap-southeast-1.amazonaws.com");
		tmpRegionVsURLs.put("eu-west-1", "monitoring.eu-west-1.amazonaws.com");
		tmpRegionVsURLs.put("us-east-1", "monitoring.us-east-1.amazonaws.com");
		tmpRegionVsURLs.put("us-west-1", "monitoring.us-west-1.amazonaws.com");
		tmpRegionVsURLs.put("us-west-2", "monitoring.us-west-2.amazonaws.com");
		tmpRegionVsURLs.put("ap-southeast-2", "monitoring.ap-southeast-2.amazonaws.com");
		tmpRegionVsURLs.put("ap-northeast-1", "monitoring.ap-northeast-1.amazonaws.com");
		tmpRegionVsURLs.put("sa-east-1", "monitoring.sa-east-1.amazonaws.com");

		regionVsURLs = Collections.unmodifiableMap(tmpRegionVsURLs);
	}

	private static final Map<String, String> DEFAULT_ARGS = new HashMap<String, String>() {
		{
			put("configurations", "monitors/CloudWatchMonitor/conf/AWSConfigurations.xml");
			put(TaskInputArgs.METRIC_PREFIX, "Custom Metrics|Amazon Cloud Watch|");
		}
	};

	public AmazonCloudWatchMonitor() {
		String msg = "Using Monitor Version [" + getImplementationVersion() + "]";
		logger.info(msg);
		System.out.println(msg);
    }

	/**
	 * Initialize AWS credentials, disabled metrics, and supported namespaces
	 *
	 * @param taskArguments
	 * @return
	 */
	public void initialize(Map<String, String> taskArguments) throws Exception {
		if (!isInitialized) {
			taskArguments = ArgumentsValidator.validateArguments(taskArguments, DEFAULT_ARGS);
			metric_prefix = taskArguments.get(TaskInputArgs.METRIC_PREFIX);
			configuration = ConfigurationUtil.getConfigurations(taskArguments.get("configurations"));
			credentials = configuration.awsCredentials;
			disabledMetrics = configuration.disabledMetrics;
			availableNamespaces = configuration.availableNamespaces;
			availableRegions = configuration.availableRegions;
			metricTypes = configuration.metricTypes;
			isInitialized = true;
            awsWorkerPool = Executors.newFixedThreadPool(5);
            awsMetricWorkerPool = Executors.newFixedThreadPool(20);
            metricsManagerFactory = new MetricsManagerFactory(this);
            useEc2InstanceNameInMetrics = configuration.useNameInMetrics;
            initializeEC2InstanceNameManager();
            logger.info("AmazonMonitor initialized");
		}
	}
	
	private void initializeEC2InstanceNameManager() {
		if (useEc2InstanceNameInMetrics && availableNamespaces.contains(AWS_EC2_NAMESPACE)) {
			ec2InstanceNameManager = new EC2InstanceNameManager(
					credentials, configuration.tagFilterName, 
					configuration.tagKey);
			ec2InstanceNameManager.initialise(availableRegions);
		}
	}

	/**
	 * Main execution method that uploads the metrics to the AppDynamics
	 * Controller
	 *
	 * @see com.singularity.ee.agent.systemagent.api.ITask#execute(java.util.Map,
	 *      com.singularity.ee.agent.systemagent.api.TaskExecutionContext)
	 */
	public TaskOutput execute(Map<String, String> taskArguments, TaskExecutionContext taskExecutionContext) {
        try {
            logger.info("Executing CloudWatchMonitor...");
            initialize(taskArguments);
            ExecutorCompletionService ecs = new ExecutorCompletionService(awsWorkerPool);
            int count = 0;
            for (final String region : availableRegions) {
                for (final String namespace : availableNamespaces) {
                    ecs.submit(new Callable() {
                        public Object call() throws Exception {
                            fetchAndPrintMetrics(namespace, region);
                            return null;
                        }
                    });
                    ++count;
                }
            }
            
            //Not sure if we need to wait.
            for (int i = 0; i < count; i++) {
                ecs.take().get();
            }
            logger.info("Finished Executing CloudWatchMonitor...");
            return new TaskOutput("AWS Cloud Watch Metric Upload Complete Successfully");
        } catch (Exception e) {
            logger.error("Exception ", e);
            return new TaskOutput("AWS Cloud Watch Metric Upload Failed");
        }
    }

	private void fetchAndPrintMetrics(String namespace, String region) {
        AmazonCloudWatch awsCloudWatch = new AmazonCloudWatchClient(credentials);
        awsCloudWatch.setEndpoint(regionVsURLs.get(region));
		MetricsManager metricsManager = metricsManagerFactory.createMetricsManager(namespace);
        metricsManager.setWorkerPool(awsMetricWorkerPool);
		Map<String, Map<String, List<Datapoint>>> metrics = metricsManager.gatherMetrics(awsCloudWatch, region);
		// Logging number of instances for which metrics
		// were collected
		
		if (metricsManager instanceof CustomNamespaceMetricsManager) {
			logger.info(String.format("Custom Namespace Metrics Count - %5s:%-5s %5s:%-5s %5s:%-5d", "Region", region, "Namespace", namespace,
					"#Metric Size", metrics.size()));
		} else {
			logger.info(String.format("Running Instances Count in AWS - %5s:%-5s %5s:%-5s %5s:%-5d", "Region", region, "Namespace", namespace,
					"#Instances", metrics.size()));
		}
		
		metricsManager.printMetrics(region, metrics);

	}
	
	
	
	/**
	 * Get the Amazon Cloud Watch Client
	 *
	 * @return AmazonCloudWatch
	 */

	/**
	 * Get the hashmap of disabled metrics
	 *
	 * @return HashMap
	 */
	public Map<String, Set<String>> getDisabledMetrics() {
		return this.disabledMetrics;
	}

	public static Map<String, String> getRegionvsurls() {
		return regionVsURLs;
	}

	/**
	 * Set the Amazon Cloud Watch Client
	 */

	/**
	 * Set the hashmap of disabled metrics
	 */
	public void setDisabledMetrics(Map<String, Set<String>> disabledMetrics) {
		this.disabledMetrics = disabledMetrics;
	}

	public Set<String> getAvailableRegions() {
		return availableRegions;
	}

	public void setAvailableRegions(Set<String> availableRegions) {
		this.availableRegions = availableRegions;
	}

	/**
	 * Get the AWS Credentials
	 *
	 * @return AWSCredentials
	 */
	public AWSCredentials getAWSCredentials() {
		return credentials;
	}

	/**
	 * Get the Metric Types for specific 
	 * 
	 * @return Metric Types
	 */
	public Map<String, Map<String, MetricType>> getMetricTypes() {
		return metricTypes;
	}

	/**
	 * Check for disabled metrics in particular namespaces
	 *
	 * @return boolean
	 */
	public boolean isMetricDisabled(String namespace, String metricName) {
		boolean result = false;
		if (disabledMetrics.get(namespace) != null) {
			if ((disabledMetrics.get(namespace)).contains(metricName)) {
				result = true;
			}
		}
		return result;
	}

	public EC2InstanceNameManager getEc2InstanceNameManager() {
		return ec2InstanceNameManager;
	}

	public boolean isUseEc2InstanceNameInMetrics() {
		return useEc2InstanceNameInMetrics;
	}

	/**
	 * Returns the metric to the AppDynamics Controller.
	 *
	 * @param namespacePrefix
	 *            Name of the Prefix
	 * @param metricName
	 *            Name of the Metric
	 * @param metricValue
	 *            Value of the Metric
	 * @param aggregation
	 *            Average OR Observation OR Sum
	 * @param timeRollup
	 *            Average OR Current OR Sum
	 * @param cluster
	 *            Collective OR Individual
	 */
	public void printMetric(String region, String namespacePrefix, String metricName, double metricValue, String aggregation, String timeRollup,
			String cluster) {
		try {
            String value = new BigDecimal(metricValue).setScale(0, RoundingMode.HALF_UP).toString();
            MetricWriter metricWriter = getMetricWriter(getMetricPrefix() + region + namespacePrefix + metricName, aggregation, timeRollup, cluster);
			if (logger.isDebugEnabled()) {
				logger.debug("Metric: " + getMetricPrefix() + region + namespacePrefix + metricName + " value: "+metricValue+" -> " + value);
			}
			metricWriter.printMetric(value);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	/**
	 * Metric Prefix
	 *
	 * @return String
	 */
	private String getMetricPrefix() {
		return metric_prefix + "|";
	}

	public static String getImplementationVersion() {
		return AmazonCloudWatchMonitor.class.getPackage().getImplementationTitle();
	}

}