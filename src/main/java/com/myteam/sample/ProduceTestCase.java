package com.myteam.sample;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.kie.api.runtime.ExecutionResults;
import org.kie.server.api.marshalling.MarshallingFormat;

public class ProduceTestCase {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		String filepath = "電力料金.xls";
	    String url = "http://localhost:8080/kie-server/services/rest/server";
	    String user = "dmAdmin";
	    String password = "redhatdm1!";
	    MarshallingFormat format = MarshallingFormat.JSON;

		KieServerRestClient.init(url, user, password, format);
	    
	    String container_id = "sample_1.0.0";
	    String session_id = "kSession01";
		
		Logger logger = Logger.getLogger(ProduceTestCase.class.getName());
		
		Map<String, RuleInfo> conditionColumns = DTable.getRuleInfo(new File(filepath));
		Map<String, List<List<String>>> res = new HashMap<String, List<List<String>>>();


        Map<String, String> testdata = new HashMap<String, String>();

		for (RuleInfo ruleInfo : conditionColumns.values()) {
			Map<String, List<String>> map = ruleInfo.getMap();
			//logger.log(Level.INFO, map);
			List<List<String>> vars = new ArrayList<List<String>>();
			ruleInfo.getConditionColumns().forEach(nKey -> {
				vars.add(map.get(nKey.toString()));
			});
			
			logger.log(Level.INFO, casecount(vars));
			
			List<Supplier<Stream<String>>> list = new ArrayList<Supplier<Stream<String>>>();

			vars.stream().forEach(l -> {
				list.add(()->l.stream());
			});
			res.put(ruleInfo.getRuleName(), new ArrayList<List<String>>());
        	String id = new String("power");

			Cartesian.go((a, b) -> a + "," + b, list).forEach(str -> {
				List<String> ele = new ArrayList<String>(Arrays.asList(str.split(",")));
		        Power power = new Power();
	        	setMembers(power, ele);
	        	power.setRule(null);
	        	String powercsv = power.toCsv();
	        	KieServerRestClient.getFacts().put(id, power);
	        	try {
					ExecutionResults results = KieServerRestClient.executeCommands(container_id, session_id);
					power = (Power) results.getValue(id);
					//System.out.println(powercsv);
					//logger.log(Level.INFO, powercsv);
				} catch (Exception e) {
					e.printStackTrace();
				}

	            if (power.getRule() != null && testdata.get(power.getRule()) == null) {
	            	testdata.put(power.getRule(), powercsv);
	            	logger.log(Level.INFO, power);
	            }
			});
		}
        File file = new File("TestData.csv");
        PrintWriter writer = new PrintWriter(file);
        writer.println("Contract,Wat");
        testdata.entrySet().stream()
        	.sorted(java.util.Map.Entry.comparingByKey())
        	.forEach(s -> writer.println(s.getValue() + "," + s.getKey()));
        writer.flush();
        logger.log(Level.INFO, "Done!!");
	}
	
	private static void setMembers(Power power, List<String>fact) {
		if (fact.size() >= 1 && !fact.get(0).equals("")) power.setContract(fact.get(0));
		if (fact.size() >= 2 && !fact.get(1).equals("")) power.setWat(Double.parseDouble(fact.get(1)));
	}
	
	public static BigDecimal casecount(List<List<String>> list) {
		BigDecimal bd = new BigDecimal(1);
		for (int n = 0; n < list.size(); n++) {
			bd = bd.multiply(BigDecimal.valueOf((long)list.get(n).size()));
		}
		return bd;
	}
}
