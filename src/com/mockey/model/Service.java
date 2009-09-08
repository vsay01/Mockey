/*
 * Copyright 2008-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mockey.model;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpHost;
import org.testng.log4testng.Logger;

import com.mockey.ClientExecuteProxy;
import com.mockey.OrderedMap;
import com.mockey.storage.IMockeyStorage;
import com.mockey.storage.StorageRegistry;

/**
 * A Service is a remote url that can be called.
 * 
 * @author chad.lafontaine
 * 
 */
public class Service implements PersistableItem, ExecutableService {

    public final static int SERVICE_RESPONSE_TYPE_PROXY = 0;
    public final static int SERVICE_RESPONSE_TYPE_STATIC_SCENARIO = 1;
    public final static int SERVICE_RESPONSE_TYPE_DYNAMIC_SCENARIO = 2;
    
    private Long id;
    private String serviceName;
    private String description;
    private Long defaultScenarioId;
    private Long errorScenarioId;
    private String httpContentType;
    private int hangTime = 500;
    private OrderedMap<Scenario> scenarios = new OrderedMap<Scenario>();
    private int serviceResponseType = SERVICE_RESPONSE_TYPE_PROXY;
    private String httpMethod = "GET";
    private Url realServiceUrl;
	private List<FulfilledClientRequest> fulfilledRequests;
	
	private static Logger logger = Logger.getLogger(Service.class);
	private static IMockeyStorage store = StorageRegistry.MockeyStorage;
	
    public List<FulfilledClientRequest> getFulfilledRequests() {
		return fulfilledRequests;
	}

	public void setFulfilledRequests(List<FulfilledClientRequest> transactions) {
		this.fulfilledRequests = transactions;
	}    

	// default constructor for xml.
	// DO NOT REMOVE.  DO NOT CALL.
    public Service() {
    }

    public Service(Url realServiceUrl) {
        this.realServiceUrl = realServiceUrl;        
        this.setServiceName("Auto-Generated Service");
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public Long getDefaultScenarioId() {
        return defaultScenarioId;
    }

    public void setDefaultScenarioId(Long defaultScenarioId) {
        this.defaultScenarioId = defaultScenarioId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String name) {
        this.serviceName = name;
    }

    public int getHangTime() {
        return hangTime;
    }

    public void setHangTime(int hangTime) {
        this.hangTime = hangTime;
    }

    public List<Scenario> getScenarios() {
        return scenarios.getOrderedList();
    }

    public Scenario getScenario(Long scenarioId) {
        return (Scenario) scenarios.get(scenarioId);
    }

    public void deleteScenario(Long scenarioId) {
        this.scenarios.remove(scenarioId);
    }

    public Scenario updateScenario(Scenario scenario) {
        scenario.setServiceId(this.id);
        return (Scenario)this.scenarios.save(scenario);
    }

    public String getMockServiceUrl() {
        if (this.realServiceUrl!=null){
            return realServiceUrl.getFullUrl();    
        } else {
            return "";
        }
    }

    /**
     * Helper method.
     * 
     * @return returns a the full URI path to this service, pre-pending
     *         "/service" to the mock service URL
     */
    public String getServiceUrl() {
        return (Url.MOCK_SERVICE_PATH + this.getMockServiceUrl());
    }

    public Url getUrl(){
        return this.realServiceUrl;
    }

    public void setRealServiceUrl(Url realServiceUrl) {
        this.realServiceUrl = realServiceUrl;
    }
    
    public void setRealServiceUrlByString(String realServiceUrl) {
        this.realServiceUrl = new Url(realServiceUrl);
    }

    public String getHttpContentType() {
        return httpContentType;
    }

    public void setHttpContentType(String httpContentType) {
        this.httpContentType = httpContentType;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Service name:").append(this.getServiceName()).append("\n");
        sb.append("Mock URL:").append(this.getMockServiceUrl()).append("\n");
        sb.append("Real URL:").append(this.getRealServiceUrl()).append("\n");
        sb.append("Scheme:").append(this.getUrl().getScheme()).append("\n");
        sb.append("Default scenario ID:").append(this.getDefaultScenarioId()).append("\n");
        sb.append("HTTP Content:").append(this.getHttpContentType()).append("\n");
        sb.append("Hang time:");
        sb.append(this.getHangTime());
        sb.append("\n");

        return sb.toString();
    }

    public void setId(Long id) {
        this.id = id;

        // Recursively set this ID to child Scenarios, if any exist.
        for (Scenario scenario : getScenarios()) {
            scenario.setServiceId(this.id);
            this.updateScenario(scenario);
        }
    }

    public Long getId() {
        return id;
    }

    public String getRealServiceUrl() {
        if (this.realServiceUrl != null) {
            return String.valueOf(realServiceUrl);
        } else {
            return "";
        }
    }

    public HttpHost getHttpHost() {
        return new HttpHost(realServiceUrl.getHost(), realServiceUrl.getPort(), realServiceUrl.getScheme());
    }

    public void setServiceResponseType(int serviceResponseType) {
        this.serviceResponseType = serviceResponseType;
    }

    public int getServiceResponseType() {
    	// If no scenarios, then proxy is automatically on.
    	if (this.getScenarios().size()==0) {
    		return SERVICE_RESPONSE_TYPE_PROXY;
    	} else {
    		return serviceResponseType;	
    	}
    }

    public void setErrorScenarioId(Long errorScenarioId) {
        this.errorScenarioId = errorScenarioId;
    }

    public Long getErrorScenarioId() {
        return errorScenarioId;
    }
    
    public Scenario getErrorScenario() {
    	// FIND SERVICE ERROR, IF EXIST.
        for(Scenario scenario : this.getScenarios()) {
        	if (scenario.getId()==this.getErrorScenarioId()) {
        		return scenario;
        	}
        }
        // No service error defined, therefore, let's use the universal
        // error.
        return StorageRegistry.MockeyStorage.getUniversalErrorScenario();
    }
    
    public Boolean isReferencedInAServicePlan() {
    		Boolean isReferenced = false;
		for(ServicePlan plan : StorageRegistry.MockeyStorage.getServicePlans()) {
			for(PlanItem planItem : plan.getPlanItemList()) {
				if(planItem.getServiceId().equals(this.getId())) {
					isReferenced = true;
					break;
				}
			}
		}
		return isReferenced;
    }

	
	public ResponseFromService Execute(RequestFromClient request) {
		ResponseFromService response = null;
        if (this.getServiceResponseType() == Service.SERVICE_RESPONSE_TYPE_PROXY) {
        	response = proxyTheRequest(request);
        } else if (this.getServiceResponseType() == Service.SERVICE_RESPONSE_TYPE_DYNAMIC_SCENARIO) {
        	response = executeDynamicScenario(request);
        } else if (this.getServiceResponseType() == Service.SERVICE_RESPONSE_TYPE_STATIC_SCENARIO) {
        	response = executeStaticScenario();
        }
        return response;
	}
	
    private ResponseFromService proxyTheRequest(RequestFromClient request) {

		logger.debug("proxying a moxie.");
		// If proxy on, then
		// 1) Capture request message.
		// 2) Set up a connection to the real service URL
		// 3) Forward the request message to the real service URL
		// 4) Read the reply from the real service URL.
		// 5) Save request + response as a historical scenario.

		// There are 2 proxy things going on here:
		// 1. Using Mockey as a 'proxy' to a real service.
		// 2. The proxy server between Mockey and the real service.
		//
		// For the proxy server between Mockey and the real service,
		// we do the following:
		ProxyServerModel proxyServer = store.getProxy();
		ClientExecuteProxy clientExecuteProxy = new ClientExecuteProxy();
		ResponseFromService response = null;
		try {
			logger.debug("Initiating request through proxy");
			response = clientExecuteProxy
					.execute(proxyServer, this, request);
		} catch (Exception e) {
			// We're here for various reasons.
			// 1) timeout from calling real service.
			// 2) unable to parse real response.
			// 3) magic!
			// Before we throw an exception, check:
			// (A) does this mock service have a default error response. If
			// no, then
			// (B) see if Mockey has a universal error response
			// If neither, then throw the exception.
			response = new ResponseFromService();

			Scenario error = this.getErrorScenario();
			if (error != null) {
				response.setBody(error.getResponseMessage());
			} else {
				response.setBody("No scenario defined. Also, we encountered this error: "
								+ e.getClass() + ": " + e.getMessage());
			}
		}
		return response;
	}

	private ResponseFromService executeStaticScenario() {

		logger.debug("mockeying a static scenario");

		// Proxy is NOT on. Therefore we use a scenario to figure out a reply.
		// Either:
		// 1) Based on matching the request message to one of the scenarios
		// or
		// 2) Based on scenario selected.
		//
		Scenario scenario = this.getScenario(this.getDefaultScenarioId());
		ResponseFromService response = new ResponseFromService();

		if (scenario != null) {
			response.setBody(scenario.getResponseMessage());
		} else {
			response.setBody("NO SCENARIO SELECTED");
		}
		return response;
	}

	private ResponseFromService executeDynamicScenario(RequestFromClient request) {

		logger.debug("mockeying a dynamic scenario.");
		String rawRequestData = "";
		try {
			rawRequestData = new String();
			if (!request.hasPostBody()) {
				// OK..let's build the request message from Params.
				// Is this a HACK? I dunno yet.
				logger
						.debug("Request message is EMPTY; building request message out of Parameters. ");
				rawRequestData = request.buildParameterRequest();
			} else {
				rawRequestData = request.getBodyInfo();
			}
		} catch (UnsupportedEncodingException e) {
			// uhm.
		}

		ResponseFromService response = new ResponseFromService();
		List<Scenario> scenarios = this.getScenarios();
		Iterator<Scenario> iter = scenarios.iterator();
		String messageMatchFound = null;
		while (iter.hasNext()) {
			Scenario scenario = iter.next();
			logger.debug("Checking: '" + scenario.getMatchStringArg()
					+ "' in Scenario message: \n" + rawRequestData);
			int indexValue = -1;
			if (request.hasPostBody()) {
				indexValue = request.getBodyInfo().indexOf(
						scenario.getMatchStringArg());
			} else {
				indexValue = rawRequestData.indexOf(scenario
						.getMatchStringArg());
			}

			if ((indexValue > -1)) {
				logger.debug("FOUND - matching '"
						+ scenario.getMatchStringArg() + "' ");
				messageMatchFound = scenario.getResponseMessage();
				break;
			}
		}
		if (messageMatchFound == null) {
			messageMatchFound = "Big fat ERROR:[Be sure to view source to see more...] \n"
					+ "Your setting is 'match scenario' but there is no matching scenario to incoming message: \n"
					+ rawRequestData;
		}
		response.setBody(messageMatchFound);
		return response;
	}
}