package com.couchbase.newrelic;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.newrelic.metrics.publish.Agent;

public class CouchbaseAgent extends Agent {

	private String name;
	private String host;
	private int port;

	public CouchbaseAgent(String GUID, String version, String name, String host, int port) {
		super(GUID, version);
		
		this.name = name;
		this.host = host;
		this.port = port;
	}

	/**
	 * The getAgentName() is the new method signature as of 2.0.1 of the Java SDK
	 */
	@Override
	public String getAgentName() {
		return name;
	}

	@Override
	public void pollCycle() {
		
		try {
			// Start by getting the list of pools, then met metrics for each one 
			getPoolList();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void getPoolList() throws IOException {
		URL url = new URL("http://" + host + ":" + port + "/pools");
		JSONObject response = callApi(url);
		if (response != null) {
        	JSONArray pools = (JSONArray)response.get("pools");
			
        	Iterator<JSONObject> iterPool = pools.iterator();
        	while (iterPool.hasNext()) {
        		JSONObject poolInfo = iterPool.next();
        		getPool(host, port, poolInfo);
        	}
        }
	}
	
	private void getPool(String host, Integer port, JSONObject poolInfo) throws IOException {
		System.out.println("Checking pool info " + poolInfo.get("name"));
		
		poolInfo.get("uri");
		URL url = new URL("http://" + host + ":" + port + poolInfo.get("uri"));
		
		JSONObject pool = CouchbaseAgent.callApi(url);
		String metricPrefix = "Pool/" + (String) pool.get("name") + "/";	
		
		if (pool != null) {
			JSONObject storage = (JSONObject) pool.get("storageTotals");
			if (storage != null) {
				JSONObject ram = (JSONObject) storage.get("ram");
				if (ram != null) {
					reportLong(metricPrefix + "ram/total", "bytes", (Long) ram.get("total"));
					reportLong(metricPrefix + "ram/used", "byte", (Long)ram.get("used"));
				}
				JSONObject hdd = (JSONObject) storage.get("hdd");
				if (hdd != null) {
					reportLong(metricPrefix + "hdd/total", "bytes", (Long) hdd.get("total"));
					reportLong(metricPrefix + "hdd/used", "bytes", (Long) hdd.get("used"));
					reportLong(metricPrefix + "hdd/free", "bytes", (Long) hdd.get("free"));
				}
			}
		}
	}
	
	private void reportLong(String metric, String units, Long value) {
		if (value != null) {
			reportMetric(metric, units, value);
		}
	}
	
	public static JSONObject callApi(URL url) throws IOException {
		JSONObject response = null;
        InputStream inputStream = null;
        HttpURLConnection connection = null;
        System.out.println("Call API: " + url);
        
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("Accept", "application/json");
            inputStream = connection.getInputStream();
            response = (JSONObject)JSONValue.parse(new InputStreamReader(inputStream));
        } catch (IOException e) {
            System.out.println(e.getStackTrace());
        } finally {
        	
        	// Cleanup
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {}
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return response;

	}
}
