package com.appdynamics.extensions.cloudwatch.metricsmanager;

public enum MetricType {
	
	AVE("Average"),
	MAX("Maximum"),
	MIN("Minimum"),
	SUM("Sum"),
	SAMPLE_COUNT("SampleCount");
	
	private String typeName;
	
	MetricType(String typeName) {
		this.typeName = typeName;
	}
	
	public String getTypeName() {
		return typeName;
	}

	public static MetricType fromString(String name) {
		String trimmedName = name != null ? name.trim() : "";
		
		if (!trimmedName.isEmpty()) {
			for (MetricType type : MetricType.values()) {
				if (type.name().equalsIgnoreCase(trimmedName) || 
						type.typeName.equalsIgnoreCase(trimmedName)) {
					return type;
				}
			}
		}
		
		throw new IllegalArgumentException(name + " is not a valid metric type.");
	}
}
