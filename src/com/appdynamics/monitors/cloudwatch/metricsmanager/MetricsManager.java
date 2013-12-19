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
package com.appdynamics.monitors.cloudwatch.metricsmanager;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.appdynamics.monitors.cloudwatch.AmazonCloudWatchMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.apache.log4j.Logger;

import java.util.*;

public abstract class MetricsManager{

    private static final int ONE_MINUTE = 60000;
    private Logger logger = Logger.getLogger(this.getClass().getName());
    protected AmazonCloudWatchMonitor amazonCloudWatchMonitor;
    protected AmazonCloudWatch awsCloudWatch;
    protected Map<String,HashSet<String>> disabledMetrics;

    /**
     * Intializes the cloudwatch cloud watch client and the hashmap of disabled metrics
     * @return	String
     */
    public void initialize(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        this.amazonCloudWatchMonitor = amazonCloudWatchMonitor;
        this.awsCloudWatch = amazonCloudWatchMonitor.getAmazonCloudWatch();
        this.disabledMetrics = amazonCloudWatchMonitor.getDisabledMetrics();
    }

    /**
     * Gather metrics for a particular namespace
     * @return Map
     */
    public abstract Map gatherMetrics();

    /**
     * Print metrics for a particular namespace
     * @param metrics Map
     */
    public abstract void printMetrics(Map metrics);

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
                .withStartTime(new Date(new Date().getTime()- ONE_MINUTE))
                .withNamespace(namespace)
                .withDimensions(dimensions)
                .withPeriod(60 * 60)
                .withMetricName(metricName)
                .withStatistics(statisticsType)
                .withEndTime(new Date());
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
    protected Map gatherMetricsHelper(String namespace, String...filterNames) {
        HashMap<String, HashMap<String,List<Datapoint>>> metrics = new HashMap<String,HashMap<String,List<Datapoint>>>();
        List<Metric> metricsList = getMetrics(namespace, filterNames);

        for (com.amazonaws.services.cloudwatch.model.Metric metric : metricsList) {
            List<Dimension> dimensions = metric.getDimensions();
            String key = dimensions.get(0).getValue();
            if (!metrics.containsKey(key)) {
                metrics.put(key, new HashMap<String, List<Datapoint>>());
            }
            if (!metrics.get(key).containsKey(metric.getMetricName())) {
                if (!amazonCloudWatchMonitor.isMetricDisabled(namespace, metric.getMetricName())) {
                    GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(namespace, metric.getMetricName(), "Average", metric.getDimensions());
                    GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                    metrics.get(key).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
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
    protected void printMetricsHelper(Map metricsMap, String prefix) {
        HashMap<String, HashMap<String,List<Datapoint>>> metrics = (HashMap<String,HashMap<String,List<Datapoint>>>) metricsMap;
        Iterator outerIterator = metrics.keySet().iterator();

        while (outerIterator.hasNext()) {
            String id = outerIterator.next().toString();
            HashMap<String, List<Datapoint>> metricStatistics = metrics.get(id);
            Iterator innerIterator = metricStatistics.keySet().iterator();
            while (innerIterator.hasNext()) {
                String metricName = innerIterator.next().toString();
                List<Datapoint> datapoints = metricStatistics.get(metricName);
                if (datapoints != null && !datapoints.isEmpty()) {
                    Datapoint data = datapoints.get(0);
                    amazonCloudWatchMonitor.printMetric(prefix, id + "|" + metricName, data.getAverage(),
                            MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                            MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                            MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                }
            }
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