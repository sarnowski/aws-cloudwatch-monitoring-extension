package com.appdynamics.extensions.cloudwatch.metricsmanager;

import static org.junit.Assert.*;

import org.junit.Test;

public class MetricTypeTest {
	
	@Test
	public void testValuesAllowedForAverage() {
		String[] values = {"ave", " AVE ", "averAGE", "Average"};
		
		for (String value : values) {
			MetricType type = MetricType.fromString(value);
			assertEquals(MetricType.AVE, type);
		}
	}
	
	@Test
	public void testValuesAllowedForSum() {
		String[] values = {"sum", " SUM ", "sUm", "Sum"};
		
		for (String value : values) {
			MetricType type = MetricType.fromString(value);
			assertEquals(MetricType.SUM, type);
		}
	}
	
	@Test
	public void testValuesAllowedForMaximum() {
		String[] values = {"max", " MAX ", "maximum", "Maximum", "maXIMum"};
		
		for (String value : values) {
			MetricType type = MetricType.fromString(value);
			assertEquals(MetricType.MAX, type);
		}
	}
	
	@Test
	public void testValuesAllowedForMinimum() {
		String[] values = {"min", " MIN ", "minimum", "Minimum", "minIMum"};
		
		for (String value : values) {
			MetricType type = MetricType.fromString(value);
			assertEquals(MetricType.MIN, type);
		}
	}
	
	@Test
	public void testValuesAllowedForSampleCount() {
		String[] values = {"sampleCount", " sample_count ", "sample_COUNT", "SampLeCount", "SAMPLECOUNT"};
		
		for (String value : values) {
			MetricType type = MetricType.fromString(value);
			assertEquals(MetricType.SAMPLE_COUNT, type);
		}
	}
	
	@Test
	public void testInvalidValues() {
		String[] invalidValues = {"blah", "", null, "count", "bytes"};
		
		for (String value : invalidValues) {
			try {
				MetricType.fromString(value);
				fail("Should've failed by now");
			} catch (IllegalArgumentException ex) {
				assertTrue(true);
			}
		}
	}

}
