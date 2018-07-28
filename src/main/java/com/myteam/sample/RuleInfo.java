package com.myteam.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RuleInfo {
	private String ruleName;
	private int startRow;
	private int lastRow;
	private List<Integer> conditionColumns = new ArrayList<Integer>();
	private Map<String, List<String>> map = new HashMap<String, List<String>>();
	
	public RuleInfo(String ruleName) {
		setRuleName(ruleName);
	}
}
