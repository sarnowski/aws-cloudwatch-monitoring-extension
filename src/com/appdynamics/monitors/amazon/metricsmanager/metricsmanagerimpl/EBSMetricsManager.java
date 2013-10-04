package com.appdynamics.monitors.amazon.metricsmanager.metricsmanagerimpl;

import com.appdynamics.monitors.amazon.AmazonCloudWatchMonitor;
import com.appdynamics.monitors.amazon.metricsmanager.MetricsManager;
import org.apache.log4j.Logger;

public final class EBSMetricsManager extends MetricsManager {

    private static final String NAMESPACE = "AWS/EBS";
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public EBSMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor){
        super(amazonCloudWatchMonitor);
    }

    @Override
    public Object gatherMetrics() {
        return gatherMetricsHelper(NAMESPACE, "VolumeId");
    }

    @Override
    public void printMetrics(Object metrics) {
        printMetricsHelper(metrics, getNamespacePrefix());
    }

    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "VolumeId");
    }
}
