package com.appdynamics.monitors.amazon;

import com.amazonaws.services.cloudwatch.model.*;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class StorageGatewayMetricsManager extends MetricsManager{

    private static final String NAMESPACE = "AWS/StorageGateway";

    public StorageGatewayMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        super(amazonCloudWatchMonitor);
    }

    @Override
    public Object gatherMetrics() {
        HashMap<String, HashMap<String,List<Datapoint>>> gatewayMetrics = new HashMap<String,HashMap<String,List<Datapoint>>>();
        List<Metric> metricsList = getMetrics(NAMESPACE, "GatewayId");

        for (com.amazonaws.services.cloudwatch.model.Metric metric : metricsList) {
            List<Dimension> dimensions = metric.getDimensions();
            String gatewayId = dimensions.get(0).getValue();
            if (!gatewayMetrics.containsKey(gatewayId)) {
                gatewayMetrics.put(gatewayId, new HashMap<String, List<Datapoint>>());
            }
            if (!gatewayMetrics.get(gatewayId).containsKey(metric.getMetricName())) {
                if (!amazonCloudWatchMonitor.isMetricDisabled(NAMESPACE, metric.getMetricName())) {
                    GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(NAMESPACE, metric.getMetricName(), "Average", dimensions);
                    GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                    gatewayMetrics.get(gatewayId).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
                }
            }
        }
        return gatewayMetrics;
    }

    @Override
    public void printMetrics(Object metrics) {
        HashMap<String, HashMap<String,List<Datapoint>>> gatewayMetrics = (HashMap<String,HashMap<String,List<Datapoint>>>) metrics;
        Iterator outerIterator = gatewayMetrics.keySet().iterator();

        while (outerIterator.hasNext()) {
            String gatewayId = outerIterator.next().toString();
            HashMap<String, List<Datapoint>> metricStatistics = gatewayMetrics.get(gatewayId);
            Iterator innerIterator = metricStatistics.keySet().iterator();
            while (innerIterator.hasNext()) {
                String metricName = innerIterator.next().toString();
                List<Datapoint> datapoints = metricStatistics.get(metricName);
                if (datapoints != null && !datapoints.isEmpty()) {
                    Datapoint data = datapoints.get(0);
                    amazonCloudWatchMonitor.printMetric(getNamespacePrefix(), gatewayId + "|" + metricName, data.getAverage(),
                            MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                            MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                            MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                }
            }
        }
    }

    @Override
    public String getNamespacePrefix() {
        return NAMESPACE.substring(4,NAMESPACE.length()) + "|" + "GatewayId|";
    }
}
