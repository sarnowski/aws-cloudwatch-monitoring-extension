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
package com.appdynamics.extensions.cloudwatch.metricsmanager;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.appdynamics.extensions.cloudwatch.AmazonCloudWatchMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.*;
import java.util.Map.Entry;

public abstract class MetricsManager {

    private Logger logger = Logger.getLogger("com.singularity.extensions.MetricsManager");
    protected AmazonCloudWatchMonitor amazonCloudWatchMonitor;
    protected AmazonCloudWatch awsCloudWatch;
    protected Map<String,Set<String>> disabledMetrics;

    /**
     * Intializes the cloudwatch cloud watch client and the hashmap of disabled metrics
     * @param regionUrl 
     * @return	String
     */
    public void initialize(AmazonCloudWatchMonitor amazonCloudWatchMonitor, String regionUrl) {
        this.amazonCloudWatchMonitor = amazonCloudWatchMonitor;
        this.awsCloudWatch = amazonCloudWatchMonitor.getAmazonCloudWatch();
        this.awsCloudWatch.setEndpoint(regionUrl);
        this.disabledMetrics = amazonCloudWatchMonitor.getDisabledMetrics();
    }

    /**
     * Gather metrics for a particular namespace
     * @return Map
     */
    public abstract Map<String, Map<String,List<Datapoint>>> gatherMetrics();

    /**
     * Print metrics for a particular namespace
     * @param region 
     * @param namespace 
     * @param metrics Map
     */
    public abstract void printMetrics(String region, Map<String, Map<String,List<Datapoint>>> metrics);

    /**
     * Get namespace prefix
     * @return String
     */
    public abstract String getNamespacePrefix();


    /**
     * Create a GetMetricStatisticsRequest for a particular namespace
     * @param namespace             Name of the Namespace
     * @param metricName            Name of the Metric
     * @param statisticsType        Type of Statistics (i.e. Average, Sum)
     * @param dimensions            List of dimensions used to filter metrics
     * @return GetMetricStatisticsRequest
     */
    protected GetMetricStatisticsRequest createGetMetricStatisticsRequest(String namespace,
                                                                       String metricName,
                                                                       String statisticsType,
                                                                       List<Dimension> dimensions) {
        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime(DateTime.now(DateTimeZone.UTC).minusMinutes(10).toDate())
                .withNamespace(namespace)
                .withDimensions(dimensions)
                .withPeriod(60)
                .withMetricName(metricName)
                .withStatistics(statisticsType)
                .withEndTime(DateTime.now(DateTimeZone.UTC).minusMinutes(5).toDate());
        return getMetricStatisticsRequest;
    }

    /**
     * Retrieve metrics for a particular namespace using the specified filter names
     * @param namespace     Name of the namespace
     * @param filterNames   List of filter names (used to filter metrics)
     * @return List<Metric> List of filtered metrics for a particular namespace
     */
    protected List<Metric> getMetrics(String namespace, String... filterNames) {
        ListMetricsRequest request = new ListMetricsRequest();
        List<DimensionFilter> filters = new ArrayList<DimensionFilter>();

        for (String filterName : filterNames) {
            DimensionFilter dimensionFilter = new DimensionFilter();
            dimensionFilter.withName(filterName);
            filters.add(dimensionFilter);
        }

        request.withNamespace(namespace);
        request.withDimensions(filters);

        ListMetricsResult listMetricsResult = awsCloudWatch.listMetrics(request);
        return listMetricsResult.getMetrics();
    }

    /**
     * Helper method to gather metrics for a particular namespace using certain filter names
     * @param namespace     Name of the namespace
     * @param filterNames   List of filter names (used to filter metrics)
     * @return Map          Map containing metrics for a particular namespace
     */
    protected Map<String, Map<String,List<Datapoint>>> gatherMetricsHelper(String namespace, String...filterNames) {
        Map<String, Map<String,List<Datapoint>>> metrics = new HashMap<String, Map<String,List<Datapoint>>>();
        List<Metric> metricsList = getMetrics(namespace, filterNames);

        for (Metric metric : metricsList) {
            List<Dimension> dimensions = metric.getDimensions();
            String key = dimensions.get(0).getValue();
            if (!metrics.containsKey(key)) {
                metrics.put(key, new HashMap<String, List<Datapoint>>());
            }
            if (!metrics.get(key).containsKey(metric.getMetricName())) {
            	String metricName = metric.getMetricName();
                if (!amazonCloudWatchMonitor.isMetricDisabled(namespace, metricName)) {
                    GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(namespace, metricName, "Average", metric.getDimensions());
                    GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                    metrics.get(key).put(metricName, getMetricStatisticsResult.getDatapoints());
                    if(logger.isDebugEnabled()) {
                    	logger.debug("Metric " + metricName + " datapoints retrieved: " + getMetricStatisticsResult.getDatapoints());
                    }
                }
            }
        }
        return metrics;
    }
    /**
     * Helper method to print metrics for a particular namespace
     * @param metricsMap    Map that contains metrics for a particular namespace
     * @param prefix        Prefix to be used to display metrics on AppDynamics Metric Browser
     */
    protected void printMetricsHelper(String region, String namespace, Map<String, Map<String,List<Datapoint>>> metricsMap, String prefix) {
        Map<String, Map<String,List<Datapoint>>> instanceMetricsMap = (Map<String, Map<String, List<Datapoint>>>) metricsMap;
            for(Entry<String, Map<String, List<Datapoint>>> entry : instanceMetricsMap.entrySet()) {
            	String instandeId = entry.getKey();
            	Map<String, List<Datapoint>> metricStatistics = entry.getValue();
            	if(logger.isDebugEnabled()) {
            		logger.debug(String.format("Collected Metrics %5s:%-5s %5s:%-5s %5s:%-5s %5s:%-5d" , "Region", region, "Namespace", namespace, "InstanceID", instandeId, " #Metrics",  metricStatistics.size()));
            	}
            	int printedMetricsSize = 0;
            	for(Entry<String, List<Datapoint>> entry2 : metricStatistics.entrySet()) {
            		String metricName = entry2.getKey();
            		List<Datapoint> datapoints = entry2.getValue();
            		if (datapoints != null && !datapoints.isEmpty()) {
                        Datapoint data = datapoints.get(0);
                        amazonCloudWatchMonitor.printMetric(region + "|", prefix, instandeId + "|" + metricName, data.getAverage(),
                                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                        printedMetricsSize++;
                    }
            	}
            	logger.info(String.format("Printed Metrics %5s:%-5s %5s:%-5s %5s:%-5s %5s:%-5d" , "Region", region, "Namespace", namespace, "InstanceID", instandeId, " # Metrics",  printedMetricsSize));
            }
    }

    /**
     * Helper method to get the namespace prefix
     * @param namespace     Name of the namespace
     * @param id            Id(s) used for a namespace
     * @return String       The constructed prefix
     */
    protected String getNamespacePrefixHelper(String namespace, String id){
        StringBuilder sb = new StringBuilder(namespace.substring(4,namespace.length()));
        sb.append("|").append(id).append("|");
        return sb.toString();
    }
}