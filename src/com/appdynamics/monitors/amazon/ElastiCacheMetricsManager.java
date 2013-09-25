package com.appdynamics.monitors.amazon;

import com.amazonaws.services.cloudwatch.model.*;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ElastiCacheMetricsManager extends MetricsManager{

    private static final String NAMESPACE = "AWS/ElastiCache";

    public ElastiCacheMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor){
        super(amazonCloudWatchMonitor);
    }

    @Override
    public Object gatherMetrics() {
        DimensionFilter clusterIdFilter = new DimensionFilter();
        clusterIdFilter.setName("CacheClusterId");
        DimensionFilter cacheNodeIdFilter = new DimensionFilter();
        cacheNodeIdFilter.setName("CacheNodeId");
        List<DimensionFilter> filters = new ArrayList<DimensionFilter>();
        filters.add(clusterIdFilter);
        filters.add(cacheNodeIdFilter);

        ListMetricsRequest request = new ListMetricsRequest();
        request.withDimensions(filters);
        ListMetricsResult listMetricsResult = awsCloudWatch.listMetrics(request);
        List<Metric> metricsList = listMetricsResult.getMetrics();

        //Top level     -- Key = CacheClusterIds,       Value = HashMap of cache nodes
        //Mid level     -- Key = CacheNodeIds,          Value = HashMap of Metrics
        //Bottom level  -- Key = MetricName,            Value = List of datapoints
        HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>> cacheClusterMetrics = new HashMap<String, HashMap<String,HashMap<String,List<Datapoint>>>>();

        for (com.amazonaws.services.cloudwatch.model.Metric metric : metricsList) {
            List<Dimension> dimensions = metric.getDimensions();
            String cacheClusterId = dimensions.get(0).getValue();
            String cacheNodeId = dimensions.get(1).getValue();
            if (!cacheClusterMetrics.containsKey(cacheClusterId)) {
                cacheClusterMetrics.put(cacheClusterId, new HashMap<String, HashMap<String, List<Datapoint>>>());
            }
            if (!cacheClusterMetrics.get(cacheClusterId).containsKey(cacheNodeId)) {
                cacheClusterMetrics.get(cacheClusterId).put(cacheNodeId, new HashMap<String, List<Datapoint>>());
            }
            if (!cacheClusterMetrics.get(cacheClusterId).get(cacheNodeId).containsKey(metric.getMetricName())) {

                if (!amazonCloudWatchMonitor.isMetricDisabled(NAMESPACE, metric.getMetricName())) {
                    GetMetricStatisticsRequest getMetricStatisticsRequest = amazonCloudWatchMonitor.createGetMetricStatisticsRequest(NAMESPACE, metric.getMetricName(), "Average", dimensions);
                    GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                    cacheClusterMetrics.get(cacheClusterId).get(cacheNodeId).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
                }
            }
        }
        return cacheClusterMetrics;
    }

    @Override
    public void printMetrics(Object metrics) {
        HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>> elastiCacheMetrics = (HashMap<String, HashMap<String,HashMap<String,List<Datapoint>>>>) metrics;
        Iterator cacheClusterIterator = elastiCacheMetrics.keySet().iterator();

        while (cacheClusterIterator.hasNext()) {
            String cacheClusterId = cacheClusterIterator.next().toString();
            HashMap<String, HashMap<String,List<Datapoint>>> cacheNodes = elastiCacheMetrics.get(cacheClusterId);
            Iterator cacheNodesIterator = cacheNodes.keySet().iterator();
            while (cacheNodesIterator.hasNext()) {
                String cacheNodeId = cacheNodesIterator.next().toString();
                HashMap<String, List<Datapoint>> metricsMap = cacheNodes.get(cacheNodeId);
                Iterator metricsIterator = metricsMap.keySet().iterator();
                while (metricsIterator.hasNext()) {
                    String metricName = metricsIterator.next().toString();
                    List<Datapoint> datapoints = metricsMap.get(metricName);
                    if (datapoints != null && datapoints.size() > 0) {
                        Datapoint data = datapoints.get(0);
                        amazonCloudWatchMonitor.printMetric(getNamespacePrefix() + cacheClusterId + "|" + "Cache Node Id|" +  cacheNodeId + "|",metricName + "(" + data.getUnit() + ")", data.getAverage(),
                                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

                    }
                }
            }
        }
    }

    @Override
    public String getNamespacePrefix() {
        return NAMESPACE.substring(4,NAMESPACE.length()) + "|" + "Cache Cluster Id|";
    }
}
