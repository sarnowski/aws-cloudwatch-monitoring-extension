package com.appdynamics.monitors.cloudwatch;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.*;
import com.appdynamics.monitors.cloudwatch.configuration.Configuration;
import com.appdynamics.monitors.cloudwatch.configuration.ConfigurationUtil;
import com.appdynamics.monitors.cloudwatch.metricsmanager.MetricsManager;
import com.appdynamics.monitors.cloudwatch.metricsmanager.MetricsManagerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class TestClass {

    private static Configuration config;
    private static AmazonCloudWatch awsCloudWatch;
    private static AmazonCloudWatchMonitor monitor;
    private static MetricsManagerFactory metricsManagerFactory;

    // Initialzation for local testing. Bit hacky since we are not using the monitor->execute()
    public static void init() {
        config = ConfigurationUtil.getConfigurations("conf/AWSConfigurations.xml");
        awsCloudWatch = new AmazonCloudWatchClient(config.awsCredentials);
        monitor = new AmazonCloudWatchMonitor();
        monitor.setAmazonCloudWatch(awsCloudWatch);
        monitor.setDisabledMetrics(config.disabledMetrics);
        metricsManagerFactory = new MetricsManagerFactory(monitor);
    }

    public static void main(String[] args) {
        init();
        //testRedshiftMetrics();
        //List<Metric> metrics = getListMetrics("AWS/Redshift", "ClusterIdentifier");
        testRoute53Metrics();
        //testDimensions();
        System.out.println("done");
    }

    public static void testRedshiftMetrics() {
        MetricsManager redshift = metricsManagerFactory.createMetricsManager("AWS/Redshift");
        Map metrics = redshift.gatherMetrics();
        System.out.println("Done collecting Redshfit metrics");
    }

    public static void testDimensions() {
        ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
        Dimension d1 = new Dimension();
        d1.setName("ClusterIdentifier");
        d1.setValue("cluster1");
        dimensions.add(d1);
        GetMetricStatisticsRequest request = createGetMetricStatisticsRequest("AWS/Redshift", "CPUUtilization", "Average", dimensions);
        GetMetricStatisticsResult result = awsCloudWatch.getMetricStatistics(request);
        System.out.println("done");
    }

    public static List<Metric> getListMetrics(String namespace, String...filterNames) {
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

    public static GetMetricStatisticsRequest createGetMetricStatisticsRequest(String namespace,
                                                                          String metricName,
                                                                          String statisticsType,
                                                                          List<Dimension> dimensions) {
        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime(new Date(new Date().getTime() - 1800000))
                .withNamespace(namespace)
                .withDimensions(dimensions)
                .withPeriod(60 * 60)
                .withMetricName(metricName)
                .withStatistics(statisticsType)
                .withEndTime(new Date());
        return getMetricStatisticsRequest;
    }

    public static void testRoute53Metrics() {
        ListMetricsRequest request = new ListMetricsRequest();
        request.withNamespace("AWS/Route53");
        ListMetricsResult result = awsCloudWatch.listMetrics(request);
        System.out.println("Done collecting Route53 metrics");
    }
}