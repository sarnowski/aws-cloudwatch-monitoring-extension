package com.appdynamics.monitors.amazon;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.EnableMetricsCollectionRequest;
import com.amazonaws.services.autoscaling.model.EnabledMetric;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class AutoScalingMetricsManager extends MetricsManager{

    private HashMap<String, List<String>> disabledMetrics = new HashMap<String, List<String>>();
    private static final String NAMESPACE = "AWS/AutoScaling";

    public AutoScalingMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        super(amazonCloudWatchMonitor);
    }

    //TODO: FIX HELPER
    private void gatherAutoScalingMetricsHelper(AutoScalingGroup currentGroup, HashMap<String,HashMap<String,List<Datapoint>>> autoScalingMetrics) {
        HashMap<String,List<Datapoint>> groupMetrics = autoScalingMetrics.get(currentGroup.getAutoScalingGroupName());
        List<EnabledMetric> enabledMetrics = currentGroup.getEnabledMetrics();
        for (EnabledMetric metric : enabledMetrics) {
            if(disabledMetrics.containsKey(metric.getMetric())) {
                return;
            }
            List<Dimension> dimensionsList = new ArrayList<Dimension>();
            dimensionsList.add(new Dimension().withName("AutoScalingGroupName").withValue(currentGroup.getAutoScalingGroupName()));
            GetMetricStatisticsRequest getMetricStatisticsRequest = amazonCloudWatchMonitor.createGetMetricStatisticsRequest(NAMESPACE, metric.getMetric(), "Average", dimensionsList);
            GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
            List<Datapoint> datapoints = getMetricStatisticsResult.getDatapoints();
            groupMetrics.put(metric.getMetric(), datapoints);
        }
    }

    @Override
    public Object gatherMetrics() {
        HashMap<String, HashMap<String,List<Datapoint>>> autoScalingMetrics = new HashMap<String,HashMap<String,List<Datapoint>>>();
        AWSCredentials awsCredentials = new BasicAWSCredentials("AKIAJTB7DYHGUBXOS7BQ", "jbW+aoHbYjFHSoTKrp+U1LEzdMZpvuGLETZuiMyc");
        AmazonAutoScalingClient amazonAutoScalingClient = new AmazonAutoScalingClient(awsCredentials);
        DescribeAutoScalingGroupsResult describeAutoScalingGroupsResult = amazonAutoScalingClient.describeAutoScalingGroups();
        List<AutoScalingGroup> autoScalingGroupList = describeAutoScalingGroupsResult.getAutoScalingGroups();
        for (AutoScalingGroup autoScalingGroup : autoScalingGroupList) {
            String groupName = autoScalingGroup.getAutoScalingGroupName();
            if (!autoScalingMetrics.containsKey(groupName)) {
                autoScalingMetrics.put(groupName, new HashMap<String,List<Datapoint>>());
                //TODO: remove this. Ask Pranta
                EnableMetricsCollectionRequest request = new EnableMetricsCollectionRequest();
                request.setAutoScalingGroupName(groupName);
                request.setGranularity("1Minute");
                amazonAutoScalingClient.enableMetricsCollection(request);

            }
            gatherAutoScalingMetricsHelper(autoScalingGroup, autoScalingMetrics);
        }
        return autoScalingMetrics;
    }

    @Override
    public void printMetrics(Object metrics) {
        HashMap<String, HashMap<String,List<Datapoint>>> autoScalingMetrics = (HashMap<String,HashMap<String,List<Datapoint>>>) metrics;
        Iterator outerIterator = autoScalingMetrics.keySet().iterator();

        while (outerIterator.hasNext()) {
            String autoScalingGroupName = outerIterator.next().toString();
            HashMap<String, List<Datapoint>> metricStatistics = autoScalingMetrics.get(autoScalingGroupName);
            Iterator innerIterator = metricStatistics.keySet().iterator();
            while (innerIterator.hasNext()) {
                String metricName = innerIterator.next().toString();
                List<Datapoint> datapoints = metricStatistics.get(metricName);
                if (datapoints != null && !datapoints.isEmpty()) {
                    Datapoint data = datapoints.get(0);
                    amazonCloudWatchMonitor.printMetric(getNamespacePrefix(), autoScalingGroupName + "|" + metricName, data.getAverage(),
                            MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                            MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                            MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                }
            }
        }
    }

    @Override
    public String getNamespacePrefix() {
        return NAMESPACE + "|" + "GroupId|";
    }
}
