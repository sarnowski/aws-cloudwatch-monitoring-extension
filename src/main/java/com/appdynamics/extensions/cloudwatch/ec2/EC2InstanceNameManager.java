package com.appdynamics.extensions.cloudwatch.ec2;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.google.common.base.Strings;

public class EC2InstanceNameManager {
	
	private static final Logger logger = Logger.getLogger(EC2InstanceNameManager.class);
	
	private static final Map<String, String> regionEndPoints;
	
	private static final int SLEEP_TIME_IN_MINS = 5;
	
	private final Map<String, String> ec2Instances = new ConcurrentHashMap<String, String>();
	
	private ExecutorService ec2WorkerPool;
	
	private AWSCredentials awsCredentials;
	
	private boolean initialised;
	
	private String tagFilterName;
	
	private String tagKey;
	
	static {
		Map<String, String> tmpRegionEndpoints = new HashMap<String, String>();

		tmpRegionEndpoints.put("us-east-1", "ec2.us-east-1.amazonaws.com");
		tmpRegionEndpoints.put("us-west-2", "ec2.us-west-2.amazonaws.com");
		tmpRegionEndpoints.put("us-west-1", "ec2.us-west-1.amazonaws.com");
		tmpRegionEndpoints.put("eu-west-1", "ec2.eu-west-1.amazonaws.com");
		tmpRegionEndpoints.put("ap-southeast-1", "ec2.ap-southeast-1.amazonaws.com");
		tmpRegionEndpoints.put("ap-southeast-2", "ec2.ap-southeast-2.amazonaws.com");
		tmpRegionEndpoints.put("ap-northeast-1", "ec2.ap-northeast-1.amazonaws.com");
		tmpRegionEndpoints.put("sa-east-1", "ec2.sa-east-1.amazonaws.com");

		regionEndPoints = Collections.unmodifiableMap(tmpRegionEndpoints);
	}
	
	public EC2InstanceNameManager(AWSCredentials awsCredentials, 
			String tagFilterName, String tagKey) {
		this.awsCredentials = awsCredentials;
		this.tagFilterName = tagFilterName;
		this.tagKey = tagKey;
		this.ec2WorkerPool = Executors.newFixedThreadPool(5);
	}

	public void initialise(final Set<String> availableRegions) {
		if (!initialised) {
			retrieveInstances(availableRegions);
	        initiateBackgroundTask(SLEEP_TIME_IN_MINS, availableRegions);
	        initialised = true;
		}
	}
	
	public String getInstanceName(String region, String instanceId) {
		if (ec2Instances.containsKey(instanceId)) {
			return ec2Instances.get(instanceId);
			
		} else {
			retrieveInstancesPerRegion(region, instanceId);
			
			if (ec2Instances.containsKey(instanceId)) {
				return ec2Instances.get(instanceId);
					
			} else {
				ec2Instances.put(instanceId, instanceId);
				return instanceId;
			}
		}
	}
	
	private void initiateBackgroundTask(long delay, final Set<String> availableRegions) {
		logger.info("Initiating background task");
		
		ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
		service.scheduleAtFixedRate(
				new Runnable() {
					public void run() {
						retrieveInstances(availableRegions);
					}
				},
				
				delay, SLEEP_TIME_IN_MINS, TimeUnit.MINUTES);
	}
	
	private void retrieveInstances(Set<String> availableRegions) {
		logger.info("Retrieving instances details...");
		ExecutorCompletionService<Object> ecs = new ExecutorCompletionService<Object>(ec2WorkerPool);
		
		int count = 0;
		for (final String region : availableRegions) {
			ecs.submit(new Callable<Object>() {
				public Object call() throws Exception {
					try{
						retrieveInstancesPerRegion(region);
					} catch (Exception e){
	                    logger.error("Error while retrieving instances in region " + region, e);
	                }
					
					return null;
				}
				
			});
			++count;
		}
		
        for (int i = 0; i < count; i++) {
            try {
                ecs.take().get();
                
            } catch (InterruptedException e) {
                logger.error("Interrupted exception", e);
                
            } catch (ExecutionException e) {
                logger.error("ExecutionException in DescribeInstancesRequest", e);
            }
        }
	}
	
	private void retrieveInstancesPerRegion(String region, String... instanceIds) {
		AmazonEC2Client ec2Client = new AmazonEC2Client(awsCredentials);
		ec2Client.setEndpoint(regionEndPoints.get(region));
		
		Filter filter = new Filter();
		filter.setName(tagFilterName);
		filter.setValues(Arrays.asList(tagKey));
		
		DescribeInstancesRequest request = new DescribeInstancesRequest();
		request.setFilters(Arrays.asList(filter));
		
		if (instanceIds != null && instanceIds.length > 0) {
			request.setInstanceIds(Arrays.asList(instanceIds));
		}
		
		DescribeInstancesResult result = ec2Client.describeInstances(request);
		
		if (result != null) {
			List<Reservation> reservations = result.getReservations();
			
			while (!Strings.isNullOrEmpty(result.getNextToken())) {
				request.setNextToken(result.getNextToken());
				result = ec2Client.describeInstances(request);
				reservations.addAll(result.getReservations());
			}
			
			for (Reservation reservation : result.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
					ec2Instances.put(instance.getInstanceId(), 
							getInstanceName(instance.getInstanceId(), instance.getTags()));
				}
			}
		}	
		
	}
	
	public Map<String, String> getEc2Instances() {
		return ec2Instances;
	}
	
	private String getInstanceName(String defaultValue, List<Tag> tags) {
		for (Tag tag : tags) {
			if (tagKey.equals(tag.getKey())) {
				if (!Strings.isNullOrEmpty(tag.getValue())) {
					return tag.getValue();
				}
				
				break;
			}
		}
		
		return defaultValue;
	}

}
