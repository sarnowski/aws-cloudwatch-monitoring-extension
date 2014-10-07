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
package com.appdynamics.extensions.cloudwatch.metricsmanager;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.*;
import com.appdynamics.extensions.cloudwatch.AmazonCloudWatchMonitor;
import com.google.common.collect.Lists;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

public abstract class MetricsManager {

    private Logger logger = Logger.getLogger("com.singularity.extensions.MetricsManager");
    protected AmazonCloudWatchMonitor amazonCloudWatchMonitor;
    protected Map<String,Set<String>> disabledMetrics;
    protected ExecutorService workerPool;

    /**
     * Intializes the cloudwatch cloud watch client and the hashmap of disabled metrics
     * @return	String
     */
    public void initialize(AmazonCloudWatchMonitor amazonCloudWatchMonitor) {
        this.amazonCloudWatchMonitor = amazonCloudWatchMonitor;
        this.disabledMetrics = amazonCloudWatchMonitor.getDisabledMetrics();
    }

    /**
     * Gather metrics for a particular namespace
     * @param awsCloudWatch 
     * @return Map
     */
    public abstract Map<String, Map<String,List<Datapoint>>> gatherMetrics(AmazonCloudWatch awsCloudWatch, String region);

    /**
     * Print metrics for a particular namespace
     * @param region 
     * @param metrics Map
     */
    public abstract void printMetrics(String region, Map<String, Map<String,List<Datapoint>>> metrics);

    /**
     * Get namespace prefix
     * @return String
     */
    public abstract String getNamespacePrefix();


    /**
     * Create a GetMetricStatisticsRequest for a particular namespace
     * @param namespace             Name of the Namespace
     * @param metricName            Name of the Metric
     * @param statisticsType        Type of Statistics (i.e. Average, Sum)
     * @param dimensions            List of dimensions used to filter metrics
     * @return GetMetricStatisticsRequest
     */
    protected GetMetricStatisticsRequest createGetMetricStatisticsRequest(String namespace,
                                                                       String metricName,
                                                                       String statisticsType,
                                                                       List<Dimension> dimensions) {
        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime(DateTime.now(DateTimeZone.UTC).minusMinutes(10).toDate())
                .withNamespace(namespace)
                .withDimensions(dimensions)
                .withPeriod(60)
                .withMetricName(metricName)
                .withStatistics(statisticsType)
                .withEndTime(DateTime.now(DateTimeZone.UTC).minusMinutes(5).toDate());
        return getMetricStatisticsRequest;
    }

    /**
     * Retrieve metrics for a particular namespace using the specified filter names
     * @param awsCloudWatch 
     * @param namespace     Name of the namespace
     * @param filterNames   List of filter names (used to filter metrics)
     * @return List<Metric> List of filtered metrics for a particular namespace
     */
    protected List<Metric> getMetrics(AmazonCloudWatch awsCloudWatch, String namespace, String... filterNames) {
        ListMetricsRequest request = new ListMetricsRequest();
        List<DimensionFilter> filters = new ArrayList<DimensionFilter>();

        for (String filterName : filterNames) {
            DimensionFilter dimensionFilter = new DimensionFilter();
            dimensionFilter.withName(filterName);
            filters.add(dimensionFilter);
        }
        request.withNamespace(namespace);
        request.withDimensions(filters);
        List<Metric> metricList = Lists.newArrayList();
        ListMetricsResult listMetricsResult = awsCloudWatch.listMetrics(request);
        metricList = listMetricsResult.getMetrics();
        // Retrieves all the metrics if metricList > 500
        while (listMetricsResult.getNextToken() != null) {
			request.setNextToken(listMetricsResult.getNextToken());
			listMetricsResult = awsCloudWatch.listMetrics(request);
			metricList.addAll(listMetricsResult.getMetrics());
		}
		return metricList;
    }

    /**
     * Helper method to gather metrics for a particular namespace using certain filter names
     * @param namespace     Name of the namespace
     * @param filterNames   List of filter names (used to filter metrics)
     * @return Map          Map containing metrics for a particular namespace
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
	protected Map<String, Map<String,List<Datapoint>>> gatherMetricsHelper(final AmazonCloudWatch awsCloudWatch, final String namespace, String region, String...filterNames) {
    	final Map<String, Map<String,List<Datapoint>>> metrics = new ConcurrentHashMap<String, Map<String,List<Datapoint>>>();
    	
        List<Metric> metricsList = getMetrics(awsCloudWatch, namespace, filterNames);
        logger.info("Available metrics Size across all instances for NameSpace:" + namespace + " Region:" + region + " - " +  metricsList.size());

        //Each metric is of the form {Namespace: AWS/EC2,MetricName: DiskWriteOps,Dimensions: [{Name: InstanceId,Value: i-b0b31ff2}]}
        if(logger.isDebugEnabled()) {
        	logMetricPerInstance(metricsList, namespace, region);
        }

        int count = 0;
        ExecutorCompletionService ecs = new ExecutorCompletionService(workerPool);
        for (final Metric metric : metricsList) {
            List<Dimension> dimensions = metric.getDimensions();
            final String key = dimensions.get(0).getValue();
            
            if (!metrics.containsKey(key)) {
                metrics.put(key, new ConcurrentHashMap<String, List<Datapoint>>());
            }
            if (!metrics.get(key).containsKey(metric.getMetricName())) {
            	final String metricName = metric.getMetricName();
                if (!amazonCloudWatchMonitor.isMetricDisabled(namespace, metricName)) {
                    ecs.submit(new Callable() {
                        public Object call() throws Exception {
                            try{
                                GetMetricStatisticsRequest request =
                                        createGetMetricStatisticsRequest(namespace, metricName, 
                                        		getMetricType(namespace, metricName).getTypeName(), metric.getDimensions());
                                GetMetricStatisticsResult result = awsCloudWatch.getMetricStatistics(request);
                                if(logger.isDebugEnabled()){
                                    logger.debug("Fetching MetricStatistics for NameSpace = " +
                                            namespace + " Metric = " + metric+" Result = "+result);
                                }
                                List<Datapoint> dataPoints = result.getDatapoints();
                                if(dataPoints!=null && !dataPoints.isEmpty()){
                                    metrics.get(key).put(metricName, dataPoints);
                                }
                            } catch (Exception e){
                                //Better to log it here when we have the context rather than in get()
                                logger.error("Error while getting the MetricStatistics for NameSpace ="
                                        + namespace + " Metric " + metric, e);
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

	protected void logDataPointsPerInstance(Map<String, Map<String, List<Datapoint>>> metrics) {
		for(Entry<String, Map<String, List<Datapoint>>> instance : metrics.entrySet()) {
			String instanceId = instance.getKey();
			Map<String, List<Datapoint>> instanceMetrics = instance.getValue();
			logger.debug("Logging metrics after getting response from AWS excluding disabled metrics");
			logger.debug("Instance: " + instanceId + " Metrics Size: " + instanceMetrics.size());
			for(Entry<String, List<Datapoint>> metricsPerInstance : instanceMetrics.entrySet()) {
				String metricName = metricsPerInstance.getKey();
				List<Datapoint> dataPoints = metricsPerInstance.getValue();
				logger.debug("MetricName: " + metricName + " DataPoints: " + dataPoints);
			}
		}
	}
    
    protected void logMetricPerInstance(List<Metric> metricsList, String namespace, String region) {
    	 Map<String, List<String>> metricsPerInstance = new HashMap<String, List<String>>();
         for (Metric metric : metricsList) {
             List<Dimension> dimensions = metric.getDimensions();
             
             String instanceId = null;
             
          	if (dimensions != null && !dimensions.isEmpty()) {
         		instanceId = dimensions.get(0).getValue();
         	}
             
         	List<String> metricList = null;
         	
         	if (metricsPerInstance.containsKey(instanceId)) {
         		metricList = metricsPerInstance.get(instanceId);
         		
         	} else {
         		metricList = new ArrayList<String>();
         	}
         	
         	metricList.add(metric.getMetricName());
         	metricsPerInstance.put(instanceId, metricList);
         }
         
        for (String key : metricsPerInstance.keySet()) {
     	logger.debug(" Metrics of " + key + " belonging to " + namespace + "," + region);
	     	for (String metricName: metricsPerInstance.get(key)) {
	     		logger.debug(metricName);
	     	}
        }
    }
    /**
     * Helper method to print metrics for a particular namespace
     * @param metricsMap    Map that contains metrics for a particular namespace
     * @param prefix        Prefix to be used to display metrics on AppDynamics Metric Browser
     */
    protected void printMetricsHelper(String region, String namespace, Map<String, Map<String,List<Datapoint>>> metricsMap, String prefix) {
        Map<String, Map<String,List<Datapoint>>> instanceMetricsMap = (Map<String, Map<String, List<Datapoint>>>) metricsMap;
            for(Entry<String, Map<String, List<Datapoint>>> entry : instanceMetricsMap.entrySet()) {
            	String instandeId = entry.getKey();
            	Map<String, List<Datapoint>> metricStatistics = entry.getValue();
            	if(logger.isDebugEnabled()) {
            		logger.debug(String.format("Collected Metrics %5s:%-5s %5s:%-5s %5s:%-5s %5s:%-5s" , "Region",
                            region, "Namespace", namespace, "Instance", instandeId,
                            " Metrics ",  metricStatistics.keySet()));
            	}
            	for(Entry<String, List<Datapoint>> entry2 : metricStatistics.entrySet()) {
            		String metricName = entry2.getKey();
            		List<Datapoint> datapoints = entry2.getValue();
            		if (datapoints != null && !datapoints.isEmpty()) {
                        Datapoint data = datapoints.get(0);
                        amazonCloudWatchMonitor.printMetric(region + "|", prefix, instandeId + "|" + metricName, 
                        		getValue(namespace, metricName, data),
                                MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                                MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                                MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                    }
            	}
            }
    }
    
    protected Double getValue(String namespace, String metricName, Datapoint data) {
    	MetricType type = getMetricType(namespace, metricName);
    	Double value = null;
    	
    	switch(type) {
    		case AVE:
    			value = data.getAverage();
    			break;
    		case MAX:
    			value = data.getMaximum();
    			break;
    		case MIN:
    			value = data.getMinimum();
    			break;
    		case SUM:
    			value = data.getSum();
    			break;
    		case SAMPLE_COUNT:
    			value = data.getSampleCount();
    			break;
    	}
    	
    	return value;
    }
    
    protected MetricType getMetricType(String namespace, String metricName) {
    	Map<String, Map<String, MetricType>> metricTypes = amazonCloudWatchMonitor.getMetricTypes();
    	MetricType metricType = null;
    	
    	if (metricTypes != null && !metricTypes.isEmpty() &&
    			metricTypes.get(namespace) != null && !metricTypes.get(namespace).isEmpty()) {
    		metricType = metricTypes.get(namespace).get(metricName);
    	}
    	
    	if (metricType == null) {
    		metricType = MetricType.AVE;
    	}
    	
    	return metricType;
    }

    /**
     * Helper method to get the namespace prefix
     * @param namespace     Name of the namespace
     * @param id            Id(s) used for a namespace
     * @return String       The constructed prefix
     */
    protected String getNamespacePrefixHelper(String namespace, String id){
        StringBuilder sb = new StringBuilder(namespace.substring(4,namespace.length()));
        sb.append("|").append(id).append("|");
        return sb.toString();
    }

    public void setWorkerPool(ExecutorService workerPool) {
        this.workerPool = workerPool;
    }
}