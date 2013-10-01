package com.appdynamics.monitors.amazon.metricsmanager;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class EC2MetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/EC2";

    public EC2MetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor){
        super(amazonCloudWatchMonitor);
    }

    @Override
    public Object gatherMetrics() {
        HashMap<String, HashMap<String, List<Datapoint>>> cloudWatchMetrics = new HashMap<String, HashMap<String,List<Datapoint>>>();
        List<com.amazonaws.services.cloudwatch.model.Metric> instanceMetrics = getMetrics(NAMESPACE, "InstanceId");

        for (com.amazonaws.services.cloudwatch.model.Metric metric : instanceMetrics) {
            Dimension dimension  = metric.getDimensions().get(0);
                if (!cloudWatchMetrics.containsKey(dimension.getValue())) {
                    cloudWatchMetrics.put(dimension.getValue(), new HashMap<String,List<Datapoint>>());
                }
                if (!amazonCloudWatchMonitor.isMetricDisabled(NAMESPACE, metric.getMetricName())) {
                    List<Dimension> dimensionsList = new ArrayList<Dimension>();
                    dimensionsList.add(dimension);
                    GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(NAMESPACE, metric.getMetricName(), "Average", dimensionsList);
                    GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                    cloudWatchMetrics.get(dimension.getValue()).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
                }
            }
        return cloudWatchMetrics;
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
        return NAMESPACE.substring(4,NAMESPACE.length()) + "|" + "InstanceId|";
    }
}
