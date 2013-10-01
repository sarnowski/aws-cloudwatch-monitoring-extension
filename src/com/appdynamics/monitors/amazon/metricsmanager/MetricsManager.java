package com.appdynamics.monitors.amazon.metricsmanager;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;

import java.util.*;

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

    protected List<Metric> getMetrics(String namespace, String... filterNames) {
        ListMetricsRequest request = new ListMetricsRequest();
        List<DimensionFilter> filters = new ArrayList<DimensionFilter>();

        for (String filterName : filterNames) {
            DimensionFilter dimensionFilter = new DimensionFilter();
            dimensionFilter.withName(filterName);
            filters.add(dimensionFilter);
        }

        request.withNamespace(namespace);
        request.withDimensions(filters);

        ListMetricsResult listMetricsResult = awsCloudWatch.listMetrics(request);
        return listMetricsResult.getMetrics();
    }
}
