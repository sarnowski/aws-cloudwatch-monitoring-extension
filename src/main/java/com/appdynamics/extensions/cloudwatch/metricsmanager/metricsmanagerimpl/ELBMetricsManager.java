/** 
* Copyright 2013 AppDynamics 
* 
* Licensed under the Apache License, Version 2.0 (the License);
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
* 
* http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an AS IS BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.appdynamics.extensions.cloudwatch.metricsmanager.metricsmanagerimpl;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManager;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;

import org.apache.log4j.Logger;

public final class ELBMetricsManager extends MetricsManager {
	
	private Logger logger = Logger.getLogger("com.singularity.extensions.cloudwatch.metricsmanager.ELBMetricsManager");

    private static final String NAMESPACE = "AWS/ELB";

    /**
     * Gather metrics for AWS/ELB
     * @return	Map     Map containing metrics
     */
    @Override
    public Map gatherMetrics(final AmazonCloudWatch awsCloudWatch, String region) {
        List<com.amazonaws.services.cloudwatch.model.Metric> elbMetricsList = getMetrics(awsCloudWatch, NAMESPACE, "LoadBalancerName", "AvailabilityZone");

        //Top level     -- Key = LoadBalancerName,      Value = HashMap of availability zones
        //Mid level     -- Key = AvailabilityZoneName,  Value = HashMap of Metrics
        //Bottom level  -- Key = MetricName,            Value = List of datapoints
        final HashMap<String, HashMap<String, HashMap<String, List<Datapoint>>>> elbMetrics = new HashMap<String, HashMap<String,HashMap<String,List<Datapoint>>>>();

        int count = 0;
        ExecutorCompletionService<Object> ecs = new ExecutorCompletionService<Object>(workerPool);
        
        for (final com.amazonaws.services.cloudwatch.model.Metric metric : elbMetricsList) {
            final List<Dimension> dimensions = metric.getDimensions();
            final String loadBalancerName = dimensions.get(0).getValue();
            final String availabilityZone = dimensions.get(1).getValue();
            
            if (!elbMetrics.containsKey(loadBalancerName)) {
                elbMetrics.put(loadBalancerName, new HashMap<String, HashMap<String, List<Datapoint>>>());
            }
            
            if (!elbMetrics.get(loadBalancerName).containsKey(availabilityZone)) {
                elbMetrics.get(loadBalancerName).put(availabilityZone, new HashMap<String, List<Datapoint>>());
            }
            
            if (!elbMetrics.get(loadBalancerName).get(availabilityZone).containsKey(metric.getMetricName())) {

                if (!amazonCloudWatchMonitor.isMetricDisabled(NAMESPACE, metric.getMetricName())) {
                	ecs.submit(new Callable<Object>() {
                        public Object call() throws Exception {
                            try{
                            	GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest(NAMESPACE, metric.getMetricName(), 
                            			getMetricType(NAMESPACE, metric.getMetricName()).getTypeName(), dimensions);
                            	GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
                            	elbMetrics.get(loadBalancerName).get(availabilityZone).put(metric.getMetricName(), getMetricStatisticsResult.getDatapoints());
                            	
                            } catch (Exception e){
                                //Better to log it here when we have the context rather than in get()
                                logger.error("Error while getting the MetricStatistics for NameSpace ="
                                        + NAMESPACE + " Metric " + metric, e);
                            }
                            return null;
                        }
                    });
                    ++count;
                	
                }
            }
        }
        
        //Wait until its complete
        for (int i = 0; i < count; i++) {
            try {
                ecs.take().get();
            } catch (InterruptedException e) {
                logger.error("Interrupted exception", e);
            } catch (ExecutionException e) {
                // We are catching the exceptions b4hand, so this will not be executed
                logger.error("ExecutionException in MetricStatistics", e);
            }
        }

        return elbMetrics;
    }

    /**
     * Print metrics for AWS/ELB
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(String region, Map metrics) {
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
                    if (datapoints != null && datapoints.size() > 0) {
                        Datapoint data = datapoints.get(0);
                        System.out.println(metricName + ": " + data);
                        amazonCloudWatchMonitor.printMetric(region + "|", getNamespacePrefix() + loadBalancerName + "|" + "Availability Zone|" +  zoneName + "|",metricName + "(" + data.getUnit() + ")", 
                        		getValue(NAMESPACE, metricName, data),
                                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);

                    }
                }
            }
        }
    }

    /**
     * Construct namespace prefix for AWS/ELB
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return NAMESPACE.substring(4,NAMESPACE.length()) + "|" + "Load Balancer Name|";
    }
}
