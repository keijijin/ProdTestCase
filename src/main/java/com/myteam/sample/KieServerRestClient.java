package com.myteam.sample;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.drools.core.command.runtime.BatchExecutionCommandImpl;
import org.drools.core.command.runtime.rule.FireAllRulesCommand;
import org.drools.core.command.runtime.rule.InsertObjectCommand;
import org.kie.api.command.Command;
import org.kie.api.runtime.ExecutionResults;
import org.kie.server.api.marshalling.MarshallingFormat;
import org.kie.server.api.model.KieContainerResource;
import org.kie.server.api.model.KieContainerResourceList;
import org.kie.server.api.model.KieServerInfo;
import org.kie.server.api.model.KieServiceResponse.ResponseType;
import org.kie.server.api.model.ServiceResponse;
import org.kie.server.client.KieServicesClient;
import org.kie.server.client.KieServicesConfiguration;
import org.kie.server.client.KieServicesFactory;
import org.kie.server.client.RuleServicesClient;

public class KieServerRestClient {

    private static final String URL = "http://localhost:8080/kie-server/services/rest/server";
    private static final String USER = "dmAdmin";
    private static final String PASSWORD = "redhatdm1!";

    private static final String CONTAINER_ID = "sample_1.0.0";
    private static final String SESSION_ID = "kSession01";

    private static final MarshallingFormat FORMAT = MarshallingFormat.JSON;

    private static KieServicesConfiguration conf;
    private static KieServicesClient kieServicesClient;
    private static Map<String, Object> facts;

    public static void init(String url, String user, String password, MarshallingFormat format) {
		setConf(KieServicesFactory.newRestConfiguration(url, user, password));
        getConf().setMarshallingFormat(format);
        kieServicesClient = KieServicesFactory.newKieServicesClient(getConf()); 
        if (getFacts() == null)
        	setFacts(new HashMap<String, Object>());    	
    }
    
    public static void init() {
    	init(URL, USER, PASSWORD, FORMAT);
    }
    
	public static void main(String[] args) {

		init();
		
		Power power= new Power();
    	power.setContract("60A");
    	power.setWat(301);

    	facts.put("power", power);
    	
		try {
			ExecutionResults results = executeCommands();
			if (results != null) {
				Collection<String> ids = results.getIdentifiers();
				ids.forEach(id -> {
					System.out.println(id + " : " + results.getValue(id));
				});
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
    
	public static ExecutionResults executeCommands() throws Exception {
		return executeCommands(CONTAINER_ID, SESSION_ID);
	}
	
	public static ExecutionResults executeCommands(String ContainerId, String SessionId) throws Exception {
		 //System.out.println("== Sending commands to the server ==");
		 RuleServicesClient rulesClient = kieServicesClient.getServicesClient(RuleServicesClient.class);

		 List<Command<?>> commands = new ArrayList<Command<?>>();

		 facts.forEach((id,fact) -> {
			 commands.add(new InsertObjectCommand(fact, id));
		 });
		 
		 FireAllRulesCommand fireAllRulesCmd = new FireAllRulesCommand();
		 commands.add(fireAllRulesCmd);

		 BatchExecutionCommandImpl executionCommand = new BatchExecutionCommandImpl(commands);
		 executionCommand.setLookup(SessionId);

		 ServiceResponse<ExecutionResults> executeResponse = rulesClient.executeCommandsWithResults(ContainerId,
					executionCommand);
		 
		 if(executeResponse.getType() == ResponseType.SUCCESS) {
			//System.out.println("Commands executed with success! Response: ");
			return executeResponse.getResult();
		 } else {
			System.out.println("Error executing rules. Message: ");
			System.out.println(executeResponse.getMsg());
			return null;
		}		
	}
	
	public void listContainers() {
		  KieContainerResourceList containersList = kieServicesClient.listContainers().getResult();
		  List<KieContainerResource> kieContainers = containersList.getContainers();
		  System.out.println("Available containers: ");
		  for (KieContainerResource container : kieContainers) {
		      System.out.println("\t" + container.getContainerId() + " (" + container.getReleaseId() + ")");
		  }
	}
	
	public void listCapabilities() {
	    KieServerInfo serverInfo = kieServicesClient.getServerInfo().getResult();
	    System.out.print("Server capabilities:");
	    for(String capability: serverInfo.getCapabilities()) {
	        System.out.print(" " + capability);
	    }
	    System.out.println();
	}
	
	public static KieServicesConfiguration getConf() {
		return conf;
	}

	public static void setConf(KieServicesConfiguration conf) {
		KieServerRestClient.conf = conf;
	}

	public static KieServicesClient getKieServicesClient() {
		return kieServicesClient;
	}

	public static void setKieServicesClient(KieServicesClient kieServicesClient) {
		KieServerRestClient.kieServicesClient = kieServicesClient;
	}

	public static Map<String, Object> getFacts() {
		return facts;
	}

	public static void setFacts(Map<String, Object> facts) {
		KieServerRestClient.facts = facts;
	}

	public static String getUrl() {
		return URL;
	}

	public static String getUser() {
		return USER;
	}

	public static String getPassword() {
		return PASSWORD;
	}

	public static String getContainerId() {
		return CONTAINER_ID;
	}

	public static String getSessionId() {
		return SESSION_ID;
	}

	public static MarshallingFormat getFormat() {
		return FORMAT;
	}

 }
