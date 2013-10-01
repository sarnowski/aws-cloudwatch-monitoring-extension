package com.appdynamics.monitors.amazon;

import com.amazonaws.auth.AWSCredentials;

import java.util.HashMap;
import java.util.HashSet;

public class Configuration {

    // This HashSet of disabled metrics is populated by reading the DisabledMetrics.xml file
    public HashMap<String,HashSet<String>> disabledMetrics = new HashMap<String,HashSet<String>>();
    // This HashSet of available namespaces is populated by reading the AvailableNamespaces.xml file
    public HashSet<String> availableNamespaces = new HashSet<String>();

    public AWSCredentials awsCredentials;

}
