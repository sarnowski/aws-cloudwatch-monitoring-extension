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

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;

import org.apache.log4j.Logger;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.cloudwatch.ec2.EC2InstanceNameManager;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManager;

public final class EC2MetricsManager extends MetricsManager {
	
	private Logger logger = Logger.getLogger(EC2MetricsManager.class);

    private static final String NAMESPACE = "AWS/EC2";
    
    private EC2InstanceNameManager ec2InstanceNameManager;
    
    private boolean useNameInMetrics;
    
    public EC2MetricsManager(EC2InstanceNameManager ec2InstanceNameManager, boolean useNameInMetrics) {
		this.ec2InstanceNameManager = ec2InstanceNameManager;
		this.useNameInMetrics = useNameInMetrics;
	}

	/**
     * Gather metrics for AWS/EC2
     * @return	Map     Map containing metrics
     */
    @Override
    public Map<String, Map<String,List<Datapoint>>> gatherMetrics(final AmazonCloudWatch awsCloudWatch, String region) {
    	final Map<String, Map<String,List<Datapoint>>> metrics = new ConcurrentHashMap<String, Map<String,List<Datapoint>>>();
    	
        List<Metric> metricsList = getMetrics(awsCloudWatch, NAMESPACE, "InstanceId");
        logger.info("Available metrics Size across all instances for NameSpace:" + NAMESPACE + " Region:" + region + " - " +  metricsList.size());

        //Each metric is of the form {Namespace: AWS/EC2,MetricName: DiskWriteOps,Dimensions: [{Name: InstanceId,Value: i-b0b31ff2}]}
        if(logger.isDebugEnabled()) {
        	logMetricPerInstance(metricsList, NAMESPACE, region);
        }

        int count = 0;
        ExecutorCompletionService<Object> ecs = new ExecutorCompletionService<Object>(workerPool);
        for (final Metric metric : metricsList) {
            List<Dimension> dimensions = metric.getDimensions();
            final String key = getInstanceName(region, dimensions.get(0).getValue());
            
            if (!metrics.containsKey(key)) {
                metrics.put(key, new ConcurrentHashMap<String, List<Datapoint>>());
            }
            if (!metrics.get(key).containsKey(metric.getMetricName())) {
            	final String metricName = metric.getMetricName();
                if (!amazonCloudWatchMonitor.isMetricDisabled(NAMESPACE, metricName)) {
                    ecs.submit(new Callable<Object>() {
                        public Object call() throws Exception {
                            try{
                                GetMetricStatisticsRequest request =
                                        createGetMetricStatisticsRequest(NAMESPACE, metricName, 
                                        		getMetricType(NAMESPACE, metricName).getTypeName(), metric.getDimensions());
                                GetMetricStatisticsResult result = awsCloudWatch.getMetricStatistics(request);
                                if(logger.isDebugEnabled()){
                                    logger.debug("Fetching MetricStatistics for NameSpace = " +
                                            NAMESPACE + " Metric = " + metric+" Result = "+result);
                                }
                                List<Datapoint> dataPoints = result.getDatapoints();
                                if(dataPoints!=null && !dataPoints.isEmpty()){
                                    metrics.get(key).put(metricName, dataPoints);
                                }
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
        
        // Logging metricName and datapoints corresponding to each instanceID
        if(logger.isDebugEnabled()) {
        	logDataPointsPerInstance(metrics);
        }
        
        return metrics;
    	
    }

    /**
     * Print metrics for AWS/EC2
     * @param metrics   Map containing metrics
     */
    @Override
    public void printMetrics(String region, Map<String, Map<String,List<Datapoint>>> metrics) {
        printMetricsHelper(region, NAMESPACE, metrics, getNamespacePrefix());
    }

    /**
     * Construct namespace prefix for AWS/EC2
     * @return String   Namespace prefix
     */
    @Override
    public String getNamespacePrefix() {
        return getNamespacePrefixHelper(NAMESPACE, "Instance");
    }
    
    private String getInstanceName(String region, String instanceId) {
    	if (useNameInMetrics) {
    		return ec2InstanceNameManager.getInstanceName(region, instanceId);
    	}
    	
    	return instanceId;
    }
}
