package com.couchbase.newrelic;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import com.newrelic.metrics.publish.configuration.Config;


public class ConnectionTest {
	List<AgentInfo> Agents = new ArrayList<AgentInfo>();
	
	@Before
	public void setUp() {
		try {
			Config.init();
			
			// Check newrelic.json 
			if (Config.getValue("license_key") != null) {
				System.out.println("Able to read newrelic.json");
				System.out.println("License key: " + Config.getValue("license_key"));
			} else {
				fail("Unable to read newrelic.json");
			}
			
			// Check plugin.json
			if (Config.getValue("agents") != null) {
				System.out.println("Able to read plugin.json");
				
				// Read the Agent array
				readAgentArray();
			} else {
				fail("Unable to read agents array from plugin.json");
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
	
	
	
	@Test
	public void getPoolInfo() throws IOException {
		
		Iterator<AgentInfo> iterAgent = Agents.iterator();
		while(iterAgent.hasNext()) {
			AgentInfo ai = iterAgent.next();
			String name = ai.getName();
			String host = ai.getHost();
			int port = ai.getPort();
			URL url = new URL("http://" + host + ":" + port + "/pools");
			JSONObject response = CouchbaseAgent.callApi(url);

            if (response == null) {
            	fail(name + " invalid response.");
            } else {
            	System.out.println("Metrics for: " + name);
            	JSONArray pools = (JSONArray)response.get("pools");
				
            	Iterator<JSONObject> iterPool = pools.iterator();
            	while (iterPool.hasNext()) {
            		JSONObject poolInfo = iterPool.next();
            		getPool(host, port, poolInfo);
            	}
            }
		}
	}

	private void getPool(String host, Integer port, JSONObject poolInfo) throws IOException {
		System.out.println("Checking pool info " + poolInfo.get("name"));
		
		poolInfo.get("uri");
		URL url = new URL("http://" + host + ":" + port + poolInfo.get("uri"));
		
		JSONObject pool = CouchbaseAgent.callApi(url);
		if (pool == null) {
			fail("We got an empty pool!");
		} else {
			System.out.println(pool);
			JSONObject storage = (JSONObject) pool.get("storageTotals");
			JSONObject ram = (JSONObject) storage.get("ram");
			System.out.println(ram);
			JSONObject hdd = (JSONObject) storage.get("hdd");
			System.out.println(hdd);
		}
	}
	
	private void readAgentArray() {
		if (Config.getValue("agents") != null) {
            if ( !(Config.getValue("agents") instanceof JSONArray) ) {
                fail("Plugin 'agents' JSON configuration must be an array");
            }
            
            JSONArray json = Config.getValue("agents");
            for (int i = 0; i < json.size(); i++) {
                JSONObject obj = (JSONObject) json.get(i);
                
                @SuppressWarnings("unchecked")
                Map<String, Object> properties = obj;
    			String name = (String) properties.get("name");
            	String host = (String) properties.get("host");
        		Integer port = ((Long) properties.get("port")).intValue();
        		
        		AgentInfo ai = new AgentInfo(name, host, port);
        		Agents.add(ai);
            }
		} else {
			fail("Unable to read agents array from plugin.json");
		}
	}
	
	private class AgentInfo {
		private String name;
		private String host;
		private int port;
		
		public AgentInfo(String name, String host, int port) {
			this.name = name;
			this.host = host;
			this.port = port;
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getHost() {
			return host;
		}
		public void setHost(String host) {
			this.host = host;
		}
		public int getPort() {
			return port;
		}
		public void setPort(int port) {
			this.port = port;
		}
	}
}
