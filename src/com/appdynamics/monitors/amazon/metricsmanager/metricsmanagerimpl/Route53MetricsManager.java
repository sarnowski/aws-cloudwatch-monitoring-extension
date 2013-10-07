package com.appdynamics.monitors.amazon.metricsmanager.metricsmanagerimpl;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53Client;
import com.amazonaws.services.route53.model.HealthCheck;
import com.amazonaws.services.route53.model.ListHealthChecksResult;
import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.appdynamics.monitors.amazon.metricsmanager.MetricsManager;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.*;

public final class Route53MetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/Route53";
    private AmazonRoute53 amazonRoute53;

    /**
     * Gather metrics for AWS/Route53
     * @return	Map     Map containing metrics
     */
    @Override
    public Map gatherMetrics() {
        ListHealthChecksResult listHealthChecksResult = amazonRoute53.listHealthChecks();
        List<HealthCheck> healthChecks = listHealthChecksResult.getHealthChecks();
        HashMap<String,List<Datapoint>> healthCheckMetrics = new HashMap<String, List<Datapoint>>();

        for (HealthCheck healthCheck : healthChecks) {
            String healthCheckId = healthCheck.getId();
            if (!healthCheckMetrics.containsKey(healthCheckId)) {
                healthCheckMetrics.put(healthCheckId, new ArrayList<Datapoint>());
            }
            List<Dimension> dimensions = new ArrayList<Dimension>();
            Dimension healthCheckDimension = new Dimension();

            healthCheckDimension.setName("HealthCheckId");
            healthCheckDimension.setValue(healthCheckId);
            dimensions.add(healthCheckDimension);
            GetMetricStatisticsRequest request = createGetMetricStatisticsRequest("AWS/Route53", "HealthCheckStatus", "Average", dimensions);
            GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(request);
            healthCheckMetrics.put(healthCheckId, getMetricStatisticsResult.getDatapoints());
        }
        return healthCheckMetrics;
    }

    /**
     * Print metrics for AWS/Route53
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(Map metrics) {
        HashMap<String,List<Datapoint>> healthCheckMetrics = (HashMap<String,List<Datapoint>> )metrics;
        Iterator healthCheckIdIterator = healthCheckMetrics.keySet().iterator();

        while (healthCheckIdIterator.hasNext()) {
            String healthCheckId = healthCheckIdIterator.next().toString();
            List<Datapoint> datapoints = healthCheckMetrics.get(healthCheckId);
            if (datapoints != null && !datapoints.isEmpty()) {
                Datapoint data = datapoints.get(0);
                amazonCloudWatchMonitor.printMetric(getNamespacePrefix(), healthCheckId + "|" + "HealthCheckStatus", data.getAverage(),
                        MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                        MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                        MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
            }
        }
    }

    /**
     * Construct namespace prefix for AWS/Route53
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return NAMESPACE.substring(4,NAMESPACE.length()) + "|" + "HealthCheckId|";
    }
}
