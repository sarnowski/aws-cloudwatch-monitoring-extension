package com.appdynamics.monitors.amazon.metricsmanager.metricsmanagerimpl;

import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.appdynamics.monitors.amazon.metricsmanager.MetricsManager;

public final class DynamoDBMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/DynamoDB";

    public DynamoDBMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor){
        super(amazonCloudWatchMonitor);
    }

    @Override
    public Object gatherMetrics() {
        return gatherMetricsHelper(NAMESPACE, "TableName");
    }

    @Override
    public void printMetrics(Object metrics) {
        printMetricsHelper(metrics, getNamespacePrefix());
    }

    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "TableName");
    }
}
