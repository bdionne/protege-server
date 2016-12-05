package org.protege.editor.owl.server.http.messages;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class History implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -4521102352676041770L;
	private String user_name;
	private String code;
	private String name;
	private String operation;
	private String reference;
	
	private HistoryType type = HistoryType.EVS;
	
	private enum HistoryType {EVS, CONCEPT};
	
	public History(String un, String c, String n, String op, String ref) {
		type = HistoryType.CONCEPT;
		user_name = un;
		code = c;
		name = n;
		operation = op;
		reference = ref;
	}
	
	public History(String c, String op, String ref) {
		code = c;
		operation = op;
		reference = ref;
	}
	
	public String toRecord() {
		if (type == HistoryType.EVS) {
			return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "\t" +
					user_name + "\t" +
					code + "\t" +
					name + "\t" +
					operation + "\t" +
					reference;
		} else {
			return code + "\t" +
					operation + "\t" +
					LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "\t" +
					reference;
		}

	}

}
