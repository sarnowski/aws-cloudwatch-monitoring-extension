package com.appdynamics.monitors.amazon.metricsmanager;

import com.amazonaws.services.cloudwatch.model.*;
import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class BillingMetricsManager extends MetricsManager{

    private static final String NAMESPACE = "AWS/Billing";

    public BillingMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        super(amazonCloudWatchMonitor);
    }

    @Override
    public Object gatherMetrics() {
        HashMap<String, HashMap<String, List<Datapoint>>> billingMetrics = new HashMap<String, HashMap<String,List<Datapoint>>>();
        List<com.amazonaws.services.cloudwatch.model.Metric> metricsList = getMetrics(NAMESPACE, "Currency", "ServiceName");

        for (com.amazonaws.services.cloudwatch.model.Metric metric : metricsList) {
            Dimension serviceName  = metric.getDimensions().get(0);
            Dimension currency = metric.getDimensions().get(1);
            if (!billingMetrics.containsKey(serviceName.getValue())) {
                billingMetrics.put(serviceName.getValue(), new HashMap<String,List<Datapoint>>());
            }
            if (!amazonCloudWatchMonitor.isMetricDisabled(NAMESPACE, metric.getMetricName())) {
                List<Dimension> dimensionsList = new ArrayList<Dimension>();
                dimensionsList.add(serviceName);
                dimensionsList.add(currency);
                GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(NAMESPACE, metric.getMetricName(), "Average", dimensionsList);
                GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                billingMetrics.get(serviceName.getValue()).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
            }
        }
        return billingMetrics;
    }

    @Override
    public void printMetrics(Object metrics) {
        HashMap<String, HashMap<String,List<Datapoint>>> billingMetrics = (HashMap<String,HashMap<String,List<Datapoint>>>) metrics;
        Iterator outerIterator = billingMetrics.keySet().iterator();

        while (outerIterator.hasNext()) {
            String serviceName = outerIterator.next().toString();
            HashMap<String, List<Datapoint>> metricStatistics = billingMetrics.get(serviceName);
            Iterator innerIterator = metricStatistics.keySet().iterator();
            while (innerIterator.hasNext()) {
                String metricName = innerIterator.next().toString();
                List<Datapoint> datapoints = metricStatistics.get(metricName);
                if (datapoints != null && !datapoints.isEmpty()) {
                    Datapoint data = datapoints.get(0);
                    amazonCloudWatchMonitor.printMetric(getNamespacePrefix(), serviceName + "|" + metricName + "(" + data.getUnit() + ")", data.getAverage(),
                            MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                            MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                            MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                }
            }
        }
    }

    @Override
    public String getNamespacePrefix() {
        return NAMESPACE.substring(4,NAMESPACE.length()) + "|" + "ServiceName|";
    }
}
