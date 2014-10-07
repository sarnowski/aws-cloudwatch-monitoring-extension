package com.appdynamics.extensions.cloudwatch.metricsmanager;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.appdynamics.extensions.cloudwatch.AmazonCloudWatchMonitor;
import com.google.common.collect.Maps;

@RunWith(MockitoJUnitRunner.class)
public class MetricsManagerTest {
	
	@Mock
	private AmazonCloudWatchMonitor mockAmazonCloudWatchMonitor;
	
	private MetricsManager classUnderTest;
	
	private static final String TEST_NAMESPACE = "TEST";
	
	@Before
	public void setUp() {
		classUnderTest = new MetricsManager() {
			
			@Override
			public void printMetrics(String region,
					Map<String, Map<String, List<Datapoint>>> metrics) {
			}
			
			@Override
			public String getNamespacePrefix() {
				return TEST_NAMESPACE;
			}
			
			@Override
			public Map<String, Map<String, List<Datapoint>>> gatherMetrics(
					AmazonCloudWatch awsCloudWatch, String region) {
				return null;
			}
		};
		
		Whitebox.setInternalState(classUnderTest, "amazonCloudWatchMonitor", mockAmazonCloudWatchMonitor);
		when(mockAmazonCloudWatchMonitor.getMetricTypes()).thenReturn(getTestMetricTypes());
	}
	
	@Test
	public void testValueReturnedForAverage() {
		Double result = classUnderTest.getValue(TEST_NAMESPACE, "metricNeedsAve", getTestDatapoint());
		assertEquals(Double.valueOf(1.00), result);
	}
	
	@Test
	public void testValueReturnedForSum() {
		Double result = classUnderTest.getValue(TEST_NAMESPACE, "metricNeedsSum", getTestDatapoint());
		assertEquals(Double.valueOf(5.00), result);
	}
	
	@Test
	public void testValueReturnedForMin() {
		Double result = classUnderTest.getValue(TEST_NAMESPACE, "metricNeedsMin", getTestDatapoint());
		assertEquals(Double.valueOf(2.00), result);
	}
	
	@Test
	public void testValueReturnedForMax() {
		Double result = classUnderTest.getValue(TEST_NAMESPACE, "metricNeedsMax", getTestDatapoint());
		assertEquals(Double.valueOf(3.00), result);
	}
	
	@Test
	public void testValueReturnedForSampleCount() {
		Double result = classUnderTest.getValue(TEST_NAMESPACE, "metricNeedsSampleCount", getTestDatapoint());
		assertEquals(Double.valueOf(4.00), result);
	}
	
	@Test
	public void testDefaultValueReturnedIsAverage() {
		Double result = classUnderTest.getValue(TEST_NAMESPACE, "metricNotInMap", getTestDatapoint());
		assertEquals(Double.valueOf(1.00), result);
	}
	
	private Datapoint getTestDatapoint() {
		Datapoint testDatapoint = new Datapoint();
		testDatapoint.setAverage(1.00);
		testDatapoint.setMinimum(2.00);
		testDatapoint.setMaximum(3.00);
		testDatapoint.setSampleCount(4.00);
		testDatapoint.setSum(5.00);
		
		return testDatapoint;
	}
	
	private Map<String, Map<String, MetricType>> getTestMetricTypes() {
		Map<String, MetricType> metricNamesWithType = Maps.newHashMap();
		metricNamesWithType.put("metricNeedsAve", MetricType.AVE);
		metricNamesWithType.put("metricNeedsSum", MetricType.SUM);
		metricNamesWithType.put("metricNeedsMax", MetricType.MAX);
		metricNamesWithType.put("metricNeedsMin", MetricType.MIN);
		metricNamesWithType.put("metricNeedsSampleCount", MetricType.SAMPLE_COUNT);
		
		Map<String, Map<String, MetricType>> testMetricTypes = Maps.newHashMap();
		testMetricTypes.put(TEST_NAMESPACE, metricNamesWithType);
		
		return testMetricTypes;
	}

}
