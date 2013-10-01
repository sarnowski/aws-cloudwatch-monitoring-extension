package com.appdynamics.monitors.amazon.metricsmanager;

import com.amazonaws.services.cloudwatch.model.*;
import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class AutoScalingMetricsManager extends MetricsManager{

    private static final String NAMESPACE = "AWS/AutoScaling";

    public AutoScalingMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        super(amazonCloudWatchMonitor);
    }

    @Override
    public Object gatherMetrics() {

        HashMap<String, HashMap<String,List<Datapoint>>> autoScalingMetrics = new HashMap<String,HashMap<String,List<Datapoint>>>();
        List<Metric> metricsList = getMetrics(NAMESPACE, "AutoScalingGroupName");

        for (com.amazonaws.services.cloudwatch.model.Metric metric : metricsList) {
            List<Dimension> dimensions = metric.getDimensions();
            String autoScalingGroupName = dimensions.get(0).getValue();
            if (!autoScalingMetrics.containsKey(autoScalingGroupName)) {
                autoScalingMetrics.put(autoScalingGroupName, new HashMap<String, List<Datapoint>>());
            }
            if (!autoScalingMetrics.get(autoScalingGroupName).containsKey(metric.getMetricName())) {

                if (!amazonCloudWatchMonitor.isMetricDisabled(NAMESPACE, metric.getMetricName())) {
                    GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(NAMESPACE, metric.getMetricName(), "Average", dimensions);
                    GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                    autoScalingMetrics.get(autoScalingGroupName).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
                }
            }
        }
        return autoScalingMetrics;
    }

    @Override
    public void printMetrics(Object metrics) {
        HashMap<String, HashMap<String,List<Datapoint>>> autoScalingMetrics = (HashMap<String,HashMap<String,List<Datapoint>>>) metrics;
        Iterator outerIterator = autoScalingMetrics.keySet().iterator();

        while (outerIterator.hasNext()) {
            String autoScalingGroupName = outerIterator.next().toString();
            HashMap<String, List<Datapoint>> metricStatistics = autoScalingMetrics.get(autoScalingGroupName);
            Iterator innerIterator = metricStatistics.keySet().iterator();
            while (innerIterator.hasNext()) {
                String metricName = innerIterator.next().toString();
                List<Datapoint> datapoints = metricStatistics.get(metricName);
                if (datapoints != null && !datapoints.isEmpty()) {
                    Datapoint data = datapoints.get(0);
                    amazonCloudWatchMonitor.printMetric(getNamespacePrefix(), autoScalingGroupName + "|" + metricName, data.getAverage(),
                            MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                            MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                            MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                }
            }
        }
    }

    @Override
    public String getNamespacePrefix() {
        return NAMESPACE.substring(4,NAMESPACE.length()) + "|" + "GroupId|";
    }
}
