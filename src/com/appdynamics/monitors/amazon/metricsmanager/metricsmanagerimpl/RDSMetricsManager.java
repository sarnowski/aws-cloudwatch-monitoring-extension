package com.appdynamics.monitors.amazon.metricsmanager.metricsmanagerimpl;

import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.appdynamics.monitors.amazon.metricsmanager.MetricsManager;

import java.util.Map;

public final class RDSMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/RDS";

    public RDSMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor){
        super(amazonCloudWatchMonitor);
    }

    /**
     * Gather metrics for AWS/RDS
     * @return	Map     Map containing metrics
     */
    @Override
    public Map gatherMetrics() {
        return gatherMetricsHelper(NAMESPACE, "DBInstanceIdentifier");
    }

    /**
     * Print metrics for AWS/RDS
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(Map metrics) {
        printMetricsHelper(metrics, getNamespacePrefix());
    }

    /**
     * Construct namespace prefix for AWS/RDS
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "DBInstanceIdentifier");
    }
}
