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
package com.appdynamics.extensions.cloudwatch;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.DimensionFilter;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsRequest;
import com.amazonaws.services.cloudwatch.model.GetMetricStatisticsResult;
import com.amazonaws.services.cloudwatch.model.ListMetricsRequest;
import com.amazonaws.services.cloudwatch.model.ListMetricsResult;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.appdynamics.extensions.cloudwatch.configuration.Configuration;
import com.appdynamics.extensions.cloudwatch.configuration.ConfigurationUtil;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManager;
import com.appdynamics.extensions.cloudwatch.metricsmanager.MetricsManagerFactory;

public class TestClass {

    private static Configuration config;
    private static AmazonCloudWatch awsCloudWatch;
    private static AmazonCloudWatchMonitor monitor;
    private static MetricsManagerFactory metricsManagerFactory;

    // Initialization for local testing. Bit hacky since we are not using the monitor->execute()
    public static void init() {
        try {
            config = ConfigurationUtil.getConfigurations("src/main/resources/conf/AWSConfigurations.xml");
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
        awsCloudWatch = new AmazonCloudWatchClient(config.awsCredentials);
        monitor = new AmazonCloudWatchMonitor();
        monitor.setAmazonCloudWatch(awsCloudWatch);
        monitor.setDisabledMetrics(config.disabledMetrics);
        metricsManagerFactory = new MetricsManagerFactory(monitor);
    }

    public static void main(String[] args) {
        init();
        // testMetrics("AWS/Billing");
        //testForMetrics();
        testForDataPoints();
        System.out.println("Finished execution");
    }
    
    private static void testForDataPoints() {
		awsCloudWatch.setEndpoint(monitor.getRegionvsurls().get("us-east-1"));
		List<Dimension> dimensions = new ArrayList<Dimension>();
		Dimension dimension = new Dimension();
		dimension.setName("InstanceId");
		dimension.setValue("i-2aa66f4e");
		dimensions.add(dimension);
		GetMetricStatisticsRequest getMetricStatisticsRequest = createGetMetricStatisticsRequest("AWS/EC2", "CPUUtilization", "Average", dimensions);
        GetMetricStatisticsResult getMetricStatisticsResult = awsCloudWatch.getMetricStatistics(getMetricStatisticsRequest);
        System.out.println(getMetricStatisticsResult.getDatapoints());
	}

    public static void testDimensions() {
        ArrayList<Dimension> dimensions = new ArrayList<Dimension>();
        Dimension d1 = new Dimension();
        d1.setName("ClusterIdentifier");
        d1.setValue("cluster1");
        dimensions.add(d1);
        GetMetricStatisticsRequest request = createGetMetricStatisticsRequest("AWS/Redshift", "CPUUtilization", "Average", dimensions);
        GetMetricStatisticsResult result = awsCloudWatch.getMetricStatistics(request);
        System.out.println("done");
    }

    public static GetMetricStatisticsRequest createGetMetricStatisticsRequest(String namespace,
                                                                          String metricName,
                                                                          String statisticsType,
                                                                          List<Dimension> dimensions) {
        GetMetricStatisticsRequest getMetricStatisticsRequest = new GetMetricStatisticsRequest()
                .withStartTime(new Date(new Date().getTime() - 1000 * 60 * 60 * 24))
                .withNamespace(namespace)
                .withDimensions(dimensions)
                .withPeriod(60 * 60)
                .withMetricName(metricName)
                .withStatistics(statisticsType, "Maximum")
                .withEndTime(new Date()).withUnit(StandardUnit.Percent);
        return getMetricStatisticsRequest;
    }

    private static void testMetrics(String namespace) {
        MetricsManager metricsManager = metricsManagerFactory.createMetricsManager(namespace, "monitoring.us-west-2.amazonaws.com");
        Map metrics = metricsManager.gatherMetrics();
        System.out.println("Finished testing metrics");
    }
    
    private static void testForMetrics() {
    	awsCloudWatch.setEndpoint("monitoring.us-west-2.amazonaws.com");
		ListMetricsRequest request = new ListMetricsRequest();
		List<DimensionFilter> filters = new ArrayList<DimensionFilter>();
		DimensionFilter dimensionFilter = new DimensionFilter();
		dimensionFilter.withName("InstanceId");
		filters.add(dimensionFilter);
		request.setNamespace("AWS/EC2");
		request.setDimensions(filters);
		ListMetricsResult listMetrics = awsCloudWatch.listMetrics(request);
		System.out.println(listMetrics.getMetrics());
    }
}