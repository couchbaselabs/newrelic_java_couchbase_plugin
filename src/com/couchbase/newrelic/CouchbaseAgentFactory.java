package com.couchbase.newrelic;

import java.util.Map;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.AgentFactory;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

public class CouchbaseAgentFactory extends AgentFactory {

	// Yes this is a strange name but the last part of the name shows up in the UI
	public static final String GUID = "com.couchbase.newrelic.plugin.couchbase";
	public static final String VERSION = "0.0.1";
	
	@Override
	public Agent createConfiguredAgent(Map<String, Object> properties) throws ConfigurationException {
		
		// Properties object picks up values from our plugin.json file
		String name = (String) properties.get("name");
		String host = (String) properties.get("host");
		Integer port = ((Long) properties.get("port")).intValue();
		
		// Check the properties values
		if ((name == null) || (host == null) || (port == null)) {
			throw new ConfigurationException("Properties cannot be null, check plugin.json");
		}
		
		CouchbaseAgent agent = new CouchbaseAgent(GUID, VERSION, name, host, port);
		return agent;
	}

}
