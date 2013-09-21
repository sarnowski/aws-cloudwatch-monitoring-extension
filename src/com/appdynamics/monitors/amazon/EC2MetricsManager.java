package com.appdynamics.monitors.amazon;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.*;

public class EC2MetricsManager extends MetricsManager {

    private HashMap<String, List<String>> disabledMetrics = new HashMap<String, List<String>>();
    private static final String NAMESPACE = "AWS/EC2";

    public EC2MetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor){
        super(amazonCloudWatchMonitor);
    }

    @Override
    public Object gatherMetrics() {
        HashMap<String, HashMap<String, List<Datapoint>>> cloudWatchMetrics = new HashMap<String, HashMap<String,List<Datapoint>>>();

        List<DimensionFilter> filter = new ArrayList<DimensionFilter>();
        DimensionFilter instanceIdFilter = new DimensionFilter();
        instanceIdFilter.setName("InstanceId");
        filter.add(instanceIdFilter);
        ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
        listMetricsRequest.setDimensions(filter);
        ListMetricsResult instanceMetricsResult = awsCloudWatch.listMetrics(listMetricsRequest);
        List<com.amazonaws.services.cloudwatch.model.Metric> instanceMetrics = instanceMetricsResult.getMetrics();

        for (com.amazonaws.services.cloudwatch.model.Metric m : instanceMetrics) {
            List<Dimension> dimensions = m.getDimensions();
            for (Dimension dimension : dimensions) {
                if (!cloudWatchMetrics.containsKey(dimension.getValue())) {
                    cloudWatchMetrics.put(dimension.getValue(), new HashMap<String,List<Datapoint>>());
                }
                gatherInstanceMetricsHelper(m, dimension, cloudWatchMetrics);
            }
        }
        return cloudWatchMetrics;
    }

    private void gatherInstanceMetricsHelper(com.amazonaws.services.cloudwatch.model.Metric metric,
                                             Dimension dimension,
                                             HashMap<String, HashMap<String, List<Datapoint>>> cloudWatchMetrics) {
        if(disabledMetrics.containsKey(metric.getMetricName())) {
            return;
        }
        GetMetricStatisticsRequest getMetricStatisticsRequest = amazonCloudWatchMonitor.createGetMetricStatisticsRequest(NAMESPACE, metric.getMetricName(), "Average", null);
        GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
        cloudWatchMetrics.get(dimension.getValue()).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
    }

    @Override
    public void printMetrics(Object metrics) {
        HashMap<String, HashMap<String,List<Datapoint>>> cloudWatchMetrics = (HashMap<String,HashMap<String,List<Datapoint>>>) metrics;
        Iterator outerIterator = cloudWatchMetrics.keySet().iterator();

        while (outerIterator.hasNext()) {
            String instanceId = outerIterator.next().toString();
            HashMap<String, List<Datapoint>> metricStatistics = cloudWatchMetrics.get(instanceId);
            Iterator innerIterator = metricStatistics.keySet().iterator();
            while (innerIterator.hasNext()) {
                String metricName = innerIterator.next().toString();
                List<Datapoint> datapoints = metricStatistics.get(metricName);
                if (datapoints != null && !datapoints.isEmpty()) {
                    Datapoint data = datapoints.get(0);
                    amazonCloudWatchMonitor.printMetric(getNamespacePrefix(), instanceId + "|" + metricName + "(" + data.getUnit() + ")", data.getAverage(),
                            MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                            MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                            MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                }
            }
        }
    }

    @Override
    public String getNamespacePrefix() {
        return NAMESPACE + "|" + "InstanceId|";
    }
}
