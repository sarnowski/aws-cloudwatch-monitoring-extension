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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AutoScalingMetricsManager extends MetricsManager{

    public AutoScalingMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        super(amazonCloudWatchMonitor);
    }

    private HashMap<String,HashMap<String,List<Datapoint>>> gatherAutoScalingMetrics() {
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

    private void gatherAutoScalingMetricsHelper(AutoScalingGroup currentGroup, HashMap<String,HashMap<String,List<Datapoint>>> autoScalingMetrics) {
        HashMap<String,List<Datapoint>> groupMetrics = autoScalingMetrics.get(currentGroup.getAutoScalingGroupName());
        List<EnabledMetric> enabledMetrics = currentGroup.getEnabledMetrics();
        for (EnabledMetric m : enabledMetrics) {
            GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                    .withStartTime( new Date( System.currentTimeMillis() - TimeUnit.MINUTES.toMillis( 2 ) ) )
                    .withNamespace("AWS/AutoScaling")
                    .withPeriod(60 * 60)
                    .withDimensions(new Dimension().withName("AutoScalingGroupName").withValue(currentGroup.getAutoScalingGroupName()))
                    .withMetricName(m.getMetric())
                    .withStatistics("Average")
                    .withEndTime(new Date());
            GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
            List<Datapoint> datapoints = getMetricStatisticsResult.getDatapoints();
            groupMetrics.put(m.getMetric(), datapoints);
        }
    }


    @Override
    public Object gatherMetrics() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void printMetrics(Object metrics) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getNamespacePrefix() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
