package com.appdynamics.monitors.amazon;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public abstract class MetricsManager{

    protected AmazonCloudWatchMonitor amazonCloudWatchMonitor;
    protected AmazonCloudWatch awsCloudWatch;
    protected Map<String,HashSet<String>> disabledMetrics;

    public MetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        this.amazonCloudWatchMonitor = amazonCloudWatchMonitor;
    }

    public void initialize() {
        this.awsCloudWatch = amazonCloudWatchMonitor.getAmazonCloudWatch();
        this.disabledMetrics = amazonCloudWatchMonitor.getDisabledMetrics();
    }

    public abstract Object gatherMetrics();
    public abstract void printMetrics(Object metrics);
    public abstract String getNamespacePrefix();

    protected GetMetricStatisticsRequest createGetMetricStatisticsRequest(String namespace,
                                                                       String metricName,
                                                                       String statisticsType,
                                                                       List<Dimension> dimensions) {
        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime(new Date(new Date().getTime() - 1000000000))
                .withNamespace(namespace)
                .withDimensions(dimensions)
                .withPeriod(60 * 60)
                .withMetricName(metricName)
                .withStatistics(statisticsType)
                .withEndTime(new Date());
        return getMetricStatisticsRequest;
    }

}
