package com.appdynamics.monitors.amazon.metricsmanager.metricsmanagerimpl;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.appdynamics.monitors.amazon.metricsmanager.MetricsManager;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class SNSMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/SNS";

    /**
     * Gather metrics for AWS/SNS
     * @return	Map     Map containing metrics
     */
    @Override
    public Map gatherMetrics() {
        HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>> snsMetrics = new HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>>();

        HashMap<String, HashMap<String, List<Datapoint>>> applicationMetrics = (HashMap)gatherMetricsHelper(NAMESPACE,"Application");
        HashMap<String, HashMap<String, List<Datapoint>>> platformMetrics = (HashMap)gatherMetricsHelper(NAMESPACE,"Platform");
        HashMap<String, HashMap<String, List<Datapoint>>> topicNameMetrics = (HashMap)gatherMetricsHelper(NAMESPACE,"TopicName");

        snsMetrics.put("Application", applicationMetrics);
        snsMetrics.put("Platform", platformMetrics);
        snsMetrics.put("TopicName", topicNameMetrics);

        return snsMetrics;
    }

    /**
     * Print metrics for AWS/SNS
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(Map metrics) {
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

    /**
     * Construct namespace prefix for AWS/SNS
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return NAMESPACE.substring(4,NAMESPACE.length()) + "|";
    }
}
