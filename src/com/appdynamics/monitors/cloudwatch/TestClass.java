package com.appdynamics.monitors.cloudwatch;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
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

    // Initialization for local testing. Bit hacky since we are not using the monitor->execute()
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
        // testMetrics("AWS/Billing");
        System.out.println("Finished execution");
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

    public static GetMetricStatisticsRequest createGetMetricStatisticsRequest(String namespace,
                                                                          String metricName,
                                                                          String statisticsType,
                                                                          List<Dimension> dimensions) {
        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime(new Date(new Date().getTime() - 60000))
                .withNamespace(namespace)
                .withDimensions(dimensions)
                .withPeriod(60 * 60)
                .withMetricName(metricName)
                .withStatistics(statisticsType)
                .withEndTime(new Date());
        return getMetricStatisticsRequest;
    }

    private static void testMetrics(String namespace) {
        MetricsManager metricsManager = metricsManagerFactory.createMetricsManager(namespace);
        Map metrics = metricsManager.gatherMetrics();
        System.out.println("Finished testing metrics");
    }

}