package com.appdynamics.extensions.cloudwatch;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import com.google.common.collect.Maps;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;

/**
 * Unit test for simple App.
 */
public class AmazonCloudWatchMonitorTest {
	
	@Mock
	private MetricWriter mockMetricWriter;
	
	private AmazonCloudWatchMonitor classUnderTest;
	
	@Before
	public void setUp() throws Exception {
		classUnderTest = new AmazonCloudWatchMonitor();// spy(new AmazonCloudWatchMonitor());
		whenNew(MetricWriter.class).withArguments(any(AManagedMonitor.class), anyString()).thenReturn(mockMetricWriter);
	}
	
	@Test
	public void test() {
		Map<String, String> args = Maps.newHashMap();
		args.put("configurations","src/test/resources/AWSConfigurations.xml");
		
		classUnderTest.execute(args, null);
		
		
	}
	

}