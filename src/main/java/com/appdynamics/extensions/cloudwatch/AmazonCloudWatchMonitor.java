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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.appdynamics.TaskInputArgs;
import com.appdynamics.extensions.ArgumentsValidator;
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
	private Map<String, Set<String>> disabledMetrics;
	private Set<String> availableNamespaces;
	private Set<String> availableRegions;
	private AWSCredentials credentials;
	private Configuration configuration;
	private String metric_prefix;

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
		metricsManagerFactory = new MetricsManagerFactory(this);
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
			isInitialized = true;
			logger.info("AmazonMonitor initialized");
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

			final CountDownLatch taskChecker = new CountDownLatch(availableNamespaces.size() * availableRegions.size());
			ExecutorService threadPool = Executors.newFixedThreadPool(availableNamespaces.size() * availableRegions.size());
			for (final String region : availableRegions) {
				final AmazonCloudWatch awsCloudWatch = new AmazonCloudWatchClient(credentials);
				for (final String namespace : availableNamespaces) {
					threadPool.execute(new Runnable() {
						public void run() {
							fetchAndPrintMetrics(awsCloudWatch, namespace, region);
							taskChecker.countDown();
						}
					});
				}
			}
			taskChecker.await();
			threadPool.shutdown();
			logger.info("Finished Executing CloudWatchMonitor...");
			return new TaskOutput("AWS Cloud Watch Metric Upload Complete Successfully");
		} catch (Exception e) {
			logger.error("Exception ", e);
			return new TaskOutput("AWS Cloud Watch Metric Upload Failed");
		}
	}

	private void fetchAndPrintMetrics(AmazonCloudWatch awsCloudWatch, String namespace, String region) {
		MetricsManager metricsManager = metricsManagerFactory.createMetricsManager(namespace);
		awsCloudWatch.setEndpoint(regionVsURLs.get(region));
		Map<String, Map<String, List<Datapoint>>> metrics = metricsManager.gatherMetrics(awsCloudWatch, region);
		// Logging number of instances for which metrics
		// were collected
		logger.info(String.format("Running Instances Count in AWS - %5s:%-5s %5s:%-5s %5s:%-5d", "Region", region, "Namespace", namespace,
				"#Instances", metrics.size()));
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
			MetricWriter metricWriter = getMetricWriter(getMetricPrefix() + region + namespacePrefix + metricName, aggregation, timeRollup, cluster);
			if (logger.isDebugEnabled()) {
				logger.debug("Metric: " + getMetricPrefix() + region + namespacePrefix + metricName + " value: " + metricValue);
			}
			metricWriter.printMetric(String.valueOf((long) metricValue));
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