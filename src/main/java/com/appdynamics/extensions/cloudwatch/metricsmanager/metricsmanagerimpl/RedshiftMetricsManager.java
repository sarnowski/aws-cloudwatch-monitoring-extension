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
package com.appdynamics.extensions.cloudwatch.metricsmanager.metricsmanagerimpl;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManager;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class RedshiftMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/Redshift";

    /**
     * Gather metrics for AWS/Redshift
     * @return	Map     Map containing metrics
     */
    @Override
    public Map gatherMetrics(AmazonCloudWatch awsCloudWatch, String region) {
        HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>> redshiftMetrics = new HashMap<String, HashMap<String,HashMap<String,List<Datapoint>>>>();
        List<com.amazonaws.services.cloudwatch.model.Metric> metrics = getMetrics(awsCloudWatch, NAMESPACE, "ClusterIdentifier", "NodeID");
        for (com.amazonaws.services.cloudwatch.model.Metric metric : metrics) {
            List<Dimension> dimensions = metric.getDimensions();
            String clusterIdentifier = dimensions.get(1).getValue();
            String nodeID = dimensions.get(0).getValue();
            if (!redshiftMetrics.containsKey(clusterIdentifier)) {
                redshiftMetrics.put(clusterIdentifier, new HashMap<String, HashMap<String, List<Datapoint>>>());
            }
            if (!redshiftMetrics.get(clusterIdentifier).containsKey(nodeID)) {
                redshiftMetrics.get(clusterIdentifier).put(nodeID, new HashMap<String, List<Datapoint>>());
            }
            if (!redshiftMetrics.get(clusterIdentifier).get(nodeID).containsKey(metric.getMetricName())) {

                if (!amazonCloudWatchMonitor.isMetricDisabled(NAMESPACE, metric.getMetricName())) {
                    GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(NAMESPACE, metric.getMetricName(), 
                    		getMetricType(NAMESPACE, metric.getMetricName()).getTypeName(), dimensions);
                    GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                    redshiftMetrics.get(clusterIdentifier).get(nodeID).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
                }
            }
        }

        return redshiftMetrics;
    }

    /**
     * Print metrics for AWS/Redshift
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(String region, Map metrics) {
        HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>> redshiftMetrics = (HashMap<String, HashMap<String,HashMap<String,List<Datapoint>>>>) metrics;
        Iterator clusterIdIterator = redshiftMetrics.keySet().iterator();

        while (clusterIdIterator.hasNext()) {
            String clusterIdentifier = clusterIdIterator.next().toString();
            HashMap<String, HashMap<String,List<Datapoint>>> nodeIds = redshiftMetrics.get(clusterIdentifier);
            Iterator nodeIdIterator = nodeIds.keySet().iterator();
            while (nodeIdIterator.hasNext()) {
                String nodeId = nodeIdIterator.next().toString();
                HashMap<String, List<Datapoint>> metricsMap = nodeIds.get(nodeId);
                Iterator metricsIterator = metricsMap.keySet().iterator();
                while (metricsIterator.hasNext()) {
                    String metricName = metricsIterator.next().toString();
                    List<Datapoint> datapoints = metricsMap.get(metricName);
                    if (datapoints != null && datapoints.size() > 0) {
                        Datapoint data = datapoints.get(0);
                        amazonCloudWatchMonitor.printMetric(region + "|", getNamespacePrefix() + clusterIdentifier + "|" + "NodeID|" +  nodeId + "|",metricName + "(" + data.getUnit() + ")", 
                        		getValue(NAMESPACE, metricName, data),
                                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

                    }
                }
            }
        }
    }

    /**
     * Construct namespace prefix for AWS/Redshift
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return NAMESPACE.substring(4,NAMESPACE.length()) + "|" + "ClusterIdentifier|";
    }
}
