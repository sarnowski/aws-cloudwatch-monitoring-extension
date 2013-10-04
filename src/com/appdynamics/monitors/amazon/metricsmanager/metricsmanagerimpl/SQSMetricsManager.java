package com.appdynamics.monitors.amazon.metricsmanager.metricsmanagerimpl;

import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.appdynamics.monitors.amazon.metricsmanager.MetricsManager;

public final class SQSMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/SQS";

    public SQSMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        super(amazonCloudWatchMonitor);
    }

    @Override
    public Object gatherMetrics() {
        return gatherMetricsHelper(NAMESPACE, "QueueName");
    }

    @Override
    public void printMetrics(Object metrics) {
        printMetricsHelper(metrics, getNamespacePrefix());
    }

    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "QueueName");
    }
}
