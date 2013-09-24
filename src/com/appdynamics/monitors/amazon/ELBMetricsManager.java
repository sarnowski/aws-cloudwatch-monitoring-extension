package com.appdynamics.monitors.amazon;

import com.amazonaws.services.cloudwatch.model.*;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import org.apache.log4j.Logger;

import java.util.*;

public class ELBMetricsManager extends MetricsManager{

    private static final String NAMESPACE = "AWS/ELB";
    private Logger logger = Logger.getLogger(this.getClass().getName());

    public ELBMetricsManager(AmazonCloudWatchMonitor amazonCloudWatchMonitor){
        super(amazonCloudWatchMonitor);
    }

    @Override
    public Object gatherMetrics() {
        List<DimensionFilter> filters = new ArrayList<DimensionFilter>();
        DimensionFilter nameFilter = new DimensionFilter();
        nameFilter.setName("LoadBalancerName");
        DimensionFilter zoneFilter = new DimensionFilter();
        zoneFilter.setName("AvailabilityZone");
        filters.add(nameFilter);
        filters.add(zoneFilter);

        ListMetricsRequest listMetricsRequest = new ListMetricsRequest();
        listMetricsRequest.withNamespace("AWS/ELB");
        listMetricsRequest.setDimensions(filters);
        ListMetricsResult elbMetricsResult = awsCloudWatch.listMetrics(listMetricsRequest);
        List<com.amazonaws.services.cloudwatch.model.Metric> elbMetricsList = elbMetricsResult.getMetrics();

        //Top level     -- Key = LoadBalancerName,      Value = HashMap of availability zones
        //Mid level     -- Key = AvailabilityZoneName,  Value = HashMap of Metrics
        //Bottom level  -- Key = MetricName,            Value = List of datapoints
        HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>> elbMetrics = new HashMap<String, HashMap<String,HashMap<String,List<Datapoint>>>>();

        for (com.amazonaws.services.cloudwatch.model.Metric m : elbMetricsList) {
            List<Dimension> dimensions = m.getDimensions();
            String loadBalancerName = dimensions.get(0).getValue();
            String availabilityZone = dimensions.get(1).getValue();
            if (!elbMetrics.containsKey(loadBalancerName)) {
                elbMetrics.put(loadBalancerName, new HashMap<String, HashMap<String, List<Datapoint>>>());
            }
            if (!elbMetrics.get(loadBalancerName).containsKey(availabilityZone)) {
                elbMetrics.get(loadBalancerName).put(availabilityZone, new HashMap<String, List<Datapoint>>());
            }
            if (!elbMetrics.get(loadBalancerName).get(availabilityZone).containsKey(m.getMetricName())) {
                if (!disabledMetrics.containsKey(m.getMetricName())) {
                    GetMetricStatisticsRequest getMetricStatisticsRequest = amazonCloudWatchMonitor.createGetMetricStatisticsRequest(NAMESPACE, m.getMetricName(), "Average", null);
                    GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                    elbMetrics.get(loadBalancerName).get(availabilityZone).put(m.getMetricName(), getMetricStatisticsResult.getDatapoints());
                }
            }
        }

        return elbMetrics;
    }

    @Override
    public void printMetrics(Object metrics) {
        HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>> elbMetrics = (HashMap<String, HashMap<String,HashMap<String,List<Datapoint>>>>) metrics;
        Iterator loadBalancerIterator = elbMetrics.keySet().iterator();

        while (loadBalancerIterator.hasNext()) {
            String loadBalancerName = loadBalancerIterator.next().toString();
            HashMap<String, HashMap<String,List<Datapoint>>> availabilityZones = elbMetrics.get(loadBalancerName);
            Iterator zoneIterator = availabilityZones.keySet().iterator();
            while (zoneIterator.hasNext()) {
                String zoneName = zoneIterator.next().toString();
                HashMap<String, List<Datapoint>> metricsMap = availabilityZones.get(zoneName);
                Iterator metricsIterator = metricsMap.keySet().iterator();
                while (metricsIterator.hasNext()) {
                    String metricName = metricsIterator.next().toString();
                    List<Datapoint> datapoints = metricsMap.get(metricName);
                    if (datapoints != null && !datapoints.isEmpty()) {
                        Datapoint data = datapoints.get(0);
                        amazonCloudWatchMonitor.printMetric(getNamespacePrefix() + loadBalancerName + "|" + "Availability Zone|" +  availabilityZones + "|",metricName + "(" + data.getUnit() + ")", data.getAverage(),
                                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

                    }
                }
            }
        }
    }

    @Override
    public String getNamespacePrefix() {
        return NAMESPACE + "|" + "Load Balancer Name|";
    }
}
