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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManager;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

public final class ElastiCacheMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/ElastiCache";

    /**
     * Gather metrics for AWS/ElastiCache
     * @return	Map     Map containing metrics
     */
    @Override
    public Map gatherMetrics() {
        List<Metric> metricsList = getMetrics(NAMESPACE, "CacheClusterId", "CacheNodeId");

        //Top level     -- Key = CacheClusterIds,       Value = HashMap of cache nodes
        //Mid level     -- Key = CacheNodeIds,          Value = HashMap of Metrics
        //Bottom level  -- Key = MetricName,            Value = List of datapoints
        Map<String, Map<String, Map<String, List<Datapoint>>>> cacheClusterMetrics = new HashMap<String, Map<String, Map<String,List<Datapoint>>>>();

        for (com.amazonaws.services.cloudwatch.model.Metric metric : metricsList) {
            List<Dimension> dimensions = metric.getDimensions();
            String cacheClusterId = dimensions.get(0).getValue();
            String cacheNodeId = dimensions.get(1).getValue();
            if (!cacheClusterMetrics.containsKey(cacheClusterId)) {
                cacheClusterMetrics.put(cacheClusterId, new HashMap<String, Map<String, List<Datapoint>>>());
            }
            if (!cacheClusterMetrics.get(cacheClusterId).containsKey(cacheNodeId)) {
                cacheClusterMetrics.get(cacheClusterId).put(cacheNodeId, new HashMap<String, List<Datapoint>>());
            }
            if (!cacheClusterMetrics.get(cacheClusterId).get(cacheNodeId).containsKey(metric.getMetricName())) {

                if (!amazonCloudWatchMonitor.isMetricDisabled(NAMESPACE, metric.getMetricName())) {
                    GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(NAMESPACE, metric.getMetricName(), "Average", dimensions);
                    GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                    cacheClusterMetrics.get(cacheClusterId).get(cacheNodeId).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
                }
            }
        }
        return cacheClusterMetrics;
    }

    /**
     * Print metrics for AWS/ElastiCache
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(String region, Map metrics) {
        Map<String, Map<String, Map<String, List<Datapoint>>>> elastiCacheMetrics = (Map<String, Map<String, Map<String, List<Datapoint>>>>) metrics;
        for(Entry<String, Map<String, Map<String, List<Datapoint>>>> cacheClusterIterator : elastiCacheMetrics.entrySet()) {
        	String cacheClusterId = cacheClusterIterator.getKey();
        	Map<String, Map<String, List<Datapoint>>> cacheNodes = cacheClusterIterator.getValue();
        	for(Entry<String, Map<String, List<Datapoint>>> cacheNodesIterator : cacheNodes.entrySet()) {
        		String cacheNodeId = cacheNodesIterator.getKey();
        		Map<String, List<Datapoint>> metricsMap = cacheNodesIterator.getValue();
        		for(Entry<String, List<Datapoint>> metricsIterator : metricsMap.entrySet()) {
        			String metricName = metricsIterator.getKey();
        			List<Datapoint> datapoints = metricsIterator.getValue();
        			if (datapoints != null && datapoints.size() > 0) {
                        Datapoint data = datapoints.get(0);
                        amazonCloudWatchMonitor.printMetric(region + "|", getNamespacePrefix() + cacheClusterId + "|" + "Cache Node Id|" +  cacheNodeId + "|",metricName + "(" + data.getUnit() + ")", data.getAverage(),
                                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

                    }
        		}
        	}
        }
    }

    /**
     * Construct namespace prefix for AWS/ElastiCache
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return NAMESPACE.substring(4,NAMESPACE.length()) + "|" + "Cache Cluster Id|";
    }
}
