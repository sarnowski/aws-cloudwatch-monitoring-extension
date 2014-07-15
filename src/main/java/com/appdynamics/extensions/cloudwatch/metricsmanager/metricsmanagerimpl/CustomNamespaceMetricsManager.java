package com.appdynamics.extensions.cloudwatch.metricsmanager.metricsmanagerimpl;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;

import org.apache.log4j.Logger;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.Metric;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManager;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

public class CustomNamespaceMetricsManager extends MetricsManager {
	
	private Logger logger = Logger.getLogger("com.singularity.extensions.cloudwatch.metricsmanager.CustomNamespaceMetricsManager");
	
	private String namespace;

	public CustomNamespaceMetricsManager(String namespace) {
		this.namespace = namespace;
	}

	@Override
	public Map<String, Map<String, List<Datapoint>>> gatherMetrics(
			final AmazonCloudWatch awsCloudWatch, String region) {
		
		final Map<String, Map<String,List<Datapoint>>> metrics = new ConcurrentHashMap<String, Map<String,List<Datapoint>>>();
		
		List<Metric> metricsList = getMetrics(awsCloudWatch, namespace);
		
        int count = 0;
        ExecutorCompletionService<Object> ecs = new ExecutorCompletionService<Object>(workerPool);
        
        for (final Metric metric : metricsList) {
            final String metricName = metric.getMetricName();
            
            if (isToProcessMetric(metrics, metricName)) {
            	metrics.put(metricName, new ConcurrentHashMap<String, List<Datapoint>>());
            	 
            	ecs.submit(new Callable<Object>() {
        			public Object call() throws Exception {
        				try{
        					GetMetricStatisticsRequest request =
        							createGetMetricStatisticsRequest(namespace, metricName, "Average", metric.getDimensions());
        					
        					GetMetricStatisticsResult result = awsCloudWatch.getMetricStatistics(request);
        					
        					if(logger.isDebugEnabled()){
        						logger.debug("Fetching MetricStatistics for NameSpace = " +
        								namespace + " Metric = " + metric+" Result = "+result);
        					}
        					
        					List<Datapoint> dataPoints = result.getDatapoints();
        					
        					if(dataPoints != null && !dataPoints.isEmpty()){
        						metrics.get(metricName).put(metricName, dataPoints);
        					}
        					
        				} catch (Exception e){
        					logger.error("Error while getting the MetricStatistics for NameSpace ="
        							+ namespace + " Metric " + metric, e);
        				}
        				return null;
        			}
        		});
        		++count;
            }
        }

        //Wait until its complete
        for (int i = 0; i < count; i++) {
            try {
                ecs.take().get();
            } catch (InterruptedException e) {
                logger.error("Interrupted exception", e);
            } catch (ExecutionException e) {
                logger.error("ExecutionException in MetricStatistics", e);
            }
        }
        
        return metrics;
	}
	
	private boolean isToProcessMetric(Map<String, Map<String,List<Datapoint>>> metrics, String metricName) {
		return !metrics.containsKey(metricName) && 
				!amazonCloudWatchMonitor.isMetricDisabled(namespace, metricName);
	}

	@Override
	public void printMetrics(String region,
			Map<String, Map<String, List<Datapoint>>> metrics) {
		
        for(Entry<String, Map<String, List<Datapoint>>> entry : metrics.entrySet()) {
        	
        	Map<String, List<Datapoint>> metricStatistics = entry.getValue();
        	if(logger.isDebugEnabled()) {
        		if (metricStatistics.entrySet() != null && !metricStatistics.entrySet().isEmpty()) {
        			logger.debug(String.format("Collected Metrics %5s:%-5s %5s:%-5s %5s:%-5s" , "Region",
        					region, "Namespace", namespace,
        					" Metrics ",  metricStatistics));
        		} else {
        			logger.debug(String.format("No data available for Namespace: %s Metric: %s",
							namespace, entry.getKey()));
        		}
        	}
        	
        	for(Entry<String, List<Datapoint>> entry2 : metricStatistics.entrySet()) {
        		String metricName = entry2.getKey();
        		List<Datapoint> datapoints = entry2.getValue();
        		if (datapoints != null && !datapoints.isEmpty()) {
                    Datapoint data = datapoints.get(0);
                    amazonCloudWatchMonitor.printMetric(region + "|", getNamespacePrefix(), "|" + metricName, data.getAverage(),
                            MetricWriter.METRIC_AGGREGATION_TYPE_OBSERVATION,
                            MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE,
                            MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_INDIVIDUAL);
                }
        	}
        }
	}

	@Override
	public String getNamespacePrefix() {
		return this.namespace;
	}
	
}
