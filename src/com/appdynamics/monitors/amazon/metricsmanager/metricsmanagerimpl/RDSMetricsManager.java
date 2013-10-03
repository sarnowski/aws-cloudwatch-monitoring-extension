package com.appdynamics.monitors.amazon.metricsmanager.metricsmanagerimpl;

import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.appdynamics.monitors.amazon.metricsmanager.MetricsManager;

public class RDSMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/RDS";

    public RDSMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor){
        super(amazonCloudWatchMonitor);
    }

    @Override
    public Object gatherMetrics() {
        return gatherMetricsHelper(NAMESPACE, "DBInstanceIdentifier");
    }

    @Override
    public void printMetrics(Object metrics) {
        printMetricsHelper(metrics, getNamespacePrefix());
    }

    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "DBInstanceIdentifier");
    }
}
