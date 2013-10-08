package com.appdynamics.monitors.cloudwatch.configuration;

import com.amazonaws.auth.AWSCredentials;

import java.util.*;

public class Configuration {

    // This HashSet of disabled metrics is populated by reading the DisabledMetrics.xml file
    public Map disabledMetrics;
    // This HashSet of available namespaces is populated by reading the AvailableNamespaces.xml file
    public Set availableNamespaces;

    public AWSCredentials awsCredentials;

    public Configuration() {
        disabledMetrics = Collections.synchronizedMap(new HashMap<String, HashSet<String>>());
        availableNamespaces = new HashSet<String>();
    }
}
