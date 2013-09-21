package com.appdynamics.monitors.amazon;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import java.util.HashSet;
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
}
