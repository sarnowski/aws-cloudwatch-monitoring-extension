package com.appdynamics.monitors.cloudwatch.metricsmanager.metricsmanagerimpl;

import com.appdynamics.monitors.cloudwatch.metricsmanager.MetricsManager;

import java.util.Map;

public final class DynamoDBMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/DynamoDB";

    /**
     * Gather metrics for AWS/DynamoDB
     * @return	Map     Map containing metrics
     */
    @Override
    public Map gatherMetrics() {
        return gatherMetricsHelper(NAMESPACE, "TableName");
    }

    /**
     * Print metrics for AWS/DynamoDB
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(Map metrics) {
        printMetricsHelper(metrics, getNamespacePrefix());
    }

    /**
     * Construct namespace prefix for AWS/DynamoDB
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "TableName");
    }
}
