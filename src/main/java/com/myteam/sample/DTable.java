package com.myteam.sample;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;

public class DTable {
	static final String RULETABLE = "RuleTable";
	static final String CONDITION = "CONDITION";

	@SuppressWarnings("deprecation")
	public static Map<String, RuleInfo> getRuleInfo(File file) {
		Map<String, RuleInfo> conditionColumns = new HashMap<String, RuleInfo>();

		Logger logger = Logger.getLogger(DTable.class.getName());

		Workbook workbook = null;
		try {
			workbook = WorkbookFactory.create(file);
		} catch (EncryptedDocumentException | InvalidFormatException | IOException e) {
			e.printStackTrace();
		}
		Sheet sheet = workbook.getSheetAt(0);
		
		String ruleTableName = "";
		
		int rowNum = sheet.getLastRowNum();
		for ( int i = 0; i < rowNum; i++ ) {
			Row row = sheet.getRow(i);
			if ( row == null ) {
				if ( ruleTableName != "") {
					RuleInfo ri = conditionColumns.get(ruleTableName);
					ri.setLastRow(i - 1);
				}
				continue;
			}
			int cellNum = row.getLastCellNum();
			for (int n = 0; n < cellNum; n++) {
				Cell cell = row.getCell(n);
				if (cell == null) continue;
				switch(cell.getCellType()) {
				case Cell.CELL_TYPE_NUMERIC:
				case Cell.CELL_TYPE_BOOLEAN:
				case Cell.CELL_TYPE_BLANK:
					break;
				case Cell.CELL_TYPE_STRING:
				default:
					String cellValue = cell.getStringCellValue();
					if ( cellValue.startsWith(RULETABLE)) {
						if (ruleTableName != null && !ruleTableName.equals("")) {
							RuleInfo ruleInfo = conditionColumns.get(ruleTableName);
							ruleInfo.setLastRow(i-1);
						}
						ruleTableName = cellValue;
						conditionColumns.put(ruleTableName, new RuleInfo(cellValue));
					} else if (cellValue.equals(CONDITION)) {
						RuleInfo ruleInfo = conditionColumns.get(ruleTableName);
						ruleInfo.getConditionColumns().add(n);
						conditionColumns.put(ruleTableName, ruleInfo);
						ruleInfo.setStartRow(i + 4);
						ruleInfo.setLastRow(sheet.getLastRowNum());
					}
				}
			}
		}
					
		for(RuleInfo rule : conditionColumns.values()) {
			for (int i = rule.getStartRow(); i <= rule.getLastRow(); i++) {
				Row row = sheet.getRow(i);
				if ( row == null ) continue;
				Map<String, List<String>> map = rule.getMap();
				for (Integer n : rule.getConditionColumns()) {
					Cell cell = row.getCell(n);
					if (cell == null) continue;
					switch (cell.getCellType()) {
					case Cell.CELL_TYPE_NUMERIC:
						double num = cell.getNumericCellValue();
						Double dnum = new Double(num);
						//String strNum = Double.toString(num);
						//if ( strNum == null || strNum.equals("")) continue;
						//putColumnValues(Integer.toString(n), Double.toString(num-1), map);
						//putColumnValues(Integer.toString(n), Double.toString(num), map);
						//putColumnValues(Integer.toString(n), Double.toString(num+1), map);
						putColumnValues(Integer.toString(n), Integer.toString(dnum.intValue()), map);
						break;
					case Cell.CELL_TYPE_STRING:
					default:
						String value = cell.getStringCellValue();
						if (value == null || value.equals("")) {
							putColumnValues(Integer.toString(n), "", map);
							continue;
						}
						String[] strs = value.split(",");
						for (int x = 0; x < strs.length; x++ ) {
							if (NumberUtils.isNumber(strs[x])) {
								Double d = Double.parseDouble(strs[x]);
								//putColumnValues(Integer.toString(n), Double.toString(d-1), map);
								//putColumnValues(Integer.toString(n), Double.toString(d), map);
								//putColumnValues(Integer.toString(n), Double.toString(d+1), map);								
								putColumnValues(Integer.toString(n), Integer.toString(d.intValue()), map);
							} else {
								putColumnValues(Integer.toString(n), strs[x], map);
							}
						}
						break;
					}
				}
			}
		}
		//logger.log(Level.INFO, conditionColumns);
		return conditionColumns;
	}

	private static void putColumnValues(String n, String value, Map<String, List<String>> map) {
		if ( !map.containsKey(n)) {
			if (n.equals("0") || n.equals("1")) map.put(n, new ArrayList<String>());
			else map.put(n, new ArrayList<String>(Arrays.asList("")));
		}
		List<String> list = map.get(n);
		if ( !list.contains(value) )
			list.add(value);
		map.put(n, list);
	}
	
	public static void main(String[] args) {
		Logger logger = Logger.getLogger(DTable.class.getName());

		String filepath = "電力料金.xls";

		Map<String, RuleInfo> conditionColumns = getRuleInfo(new File(filepath));		
		logger.log(Level.INFO, conditionColumns);
		
		for (RuleInfo rule : conditionColumns.values()) {
			//logger.log(Level.INFO, rule);
			Map<String, List<String>> map = rule.getMap();
				
			List<Supplier<Stream<String>>> list = new ArrayList<Supplier<Stream<String>>>();
			//map.keySet().stream().forEach(System.out::println);
			map.values().stream().forEach(l -> {
				list.add(()->l.stream());
			});
			
			//if (rule.getRuleName().equals("RuleTable 電力量料金表"))
			Cartesian.go((a, b) -> a + "," + b, list)
							.forEach(System.out::println);
		}
		
	}
}
