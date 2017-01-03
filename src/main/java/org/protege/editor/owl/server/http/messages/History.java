package org.protege.editor.owl.server.http.messages;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class History implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4521102352676041770L;
	private String date;
	private String user_name;
	private String code;
	public String getCode() { return code; }
	private String name;
	private String operation;
	public String getOp() { return operation; }
	private String reference;
	
	public enum HistoryType {EVS, CONCEPT};
	
	public static History create(String[] tokens) {
		String date = tokens[0];
		String un = tokens[1];
		String code = tokens[2];
		String name = tokens[3];
		String op = tokens[4];
		String ref = null;
		if (tokens.length == 6) {		
			ref = tokens[5];
		}
		return new History(date, un, code, name, op, ref);
		
	}
	
	private History(String date, String un, String c, String n, String op, String ref) {
		this.date = date;
		user_name = un;
		code = c;
		name = n;
		operation = op;
		reference = ref;
		
	}
	
	public History(String un, String c, String n, String op, String ref) {
		this(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
				un, c, n, op, ref);		
	}
	
	
	
	public String toRecord(HistoryType type) {
		if (type == HistoryType.EVS) {
			return date + "\t" +
					user_name + "\t" +
					code + "\t" +
					name + "\t" +
					operation + "\t" +
					reference;
		} else {
			return code + "\t" +
					operation + "\t" +
					date + "\t" +
					reference;
		}

	}

}