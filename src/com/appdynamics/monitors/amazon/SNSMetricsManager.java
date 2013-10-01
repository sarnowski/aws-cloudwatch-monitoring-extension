package com.appdynamics.monitors.amazon;

import com.amazonaws.services.cloudwatch.model.*;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class SNSMetricsManager extends MetricsManager{

    private static final String NAMESPACE = "AWS/SNS";

    public SNSMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        super(amazonCloudWatchMonitor);
    }

    @Override
    public Object gatherMetrics() {
        HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>> snsMetrics = new HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>>();

        HashMap<String, HashMap<String, List<Datapoint>>> applicationMetrics = getDimensionMetrics("Application");
        HashMap<String, HashMap<String, List<Datapoint>>> platformMetrics = getDimensionMetrics("Platform");
        HashMap<String, HashMap<String, List<Datapoint>>> topicNameMetrics = getDimensionMetrics("TopicName");

        snsMetrics.put("Application", applicationMetrics);
        snsMetrics.put("Platform", platformMetrics);
        snsMetrics.put("TopicName", topicNameMetrics);

        return snsMetrics;
    }

    @Override
    public void printMetrics(Object metrics) {
        HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>> snsMetrics = (HashMap<String, HashMap<String,HashMap<String,List<Datapoint>>>>) metrics;
        Iterator dimensionFilterIterator = snsMetrics.keySet().iterator();

        while (dimensionFilterIterator.hasNext()) {
            String metricType = dimensionFilterIterator.next().toString();
            HashMap<String, HashMap<String,List<Datapoint>>> dimensionMetrics = snsMetrics.get(metricType);
            Iterator dimensionIterator = dimensionMetrics.keySet().iterator();
            while (dimensionIterator.hasNext()) {
                String dimensionId = dimensionIterator.next().toString();
                HashMap<String, List<Datapoint>> metricsMap = dimensionMetrics.get(dimensionId);
                Iterator metricsIterator = metricsMap.keySet().iterator();
                while (metricsIterator.hasNext()) {
                    String metricName = metricsIterator.next().toString();
                    List<Datapoint> datapoints = metricsMap.get(metricName);
                    if (datapoints != null && datapoints.size() > 0) {
                        Datapoint data = datapoints.get(0);
                        amazonCloudWatchMonitor.printMetric(getNamespacePrefix() + metricType + "|" +  dimensionId + "|",metricName + "(" + data.getUnit() + ")", data.getAverage(),
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
        return NAMESPACE.substring(4,NAMESPACE.length()) + "|";
    }

    private HashMap<String, HashMap<String, List<Datapoint>>> getDimensionMetrics(String dimensionFilterName) {
        HashMap<String, HashMap<String, List<Datapoint>>> dimensionMetrics = new HashMap<String, HashMap<String, List<Datapoint>>>();

        List<Metric> metricsList = getMetrics(NAMESPACE, dimensionFilterName);

        for (com.amazonaws.services.cloudwatch.model.Metric metric : metricsList) {
            List<Dimension> dimensions = metric.getDimensions();
            String dimensionName = dimensions.get(0).getValue();
            if (!dimensionMetrics.containsKey(dimensionName)) {
                dimensionMetrics.put(dimensionName, new HashMap<String, List<Datapoint>>());
            }
            if (!dimensionMetrics.get(dimensionName).containsKey(metric.getMetricName())) {

                if (!amazonCloudWatchMonitor.isMetricDisabled(NAMESPACE, metric.getMetricName())) {
                    GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(NAMESPACE, metric.getMetricName(), "Average", dimensions);
                    GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                    dimensionMetrics.get(dimensionName).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
                }
            }
        }
        return dimensionMetrics;
    }
}
