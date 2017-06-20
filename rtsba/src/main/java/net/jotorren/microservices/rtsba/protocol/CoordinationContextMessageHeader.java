package net.jotorren.microservices.rtsba.protocol;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface CoordinationContextMessageHeader {

	String RTSBA_CONTEXT  			= "Link";
	String RTSBA_CONTEXT_RELATION 	= "rel=\"RTSBA-coordination-context\"";
	String RTSBA_REGISTER 			= "RTSBA-register";
	String RTSBA_SEQUENCE 			= "RTSBA-sequence";
	
	static String getTxContextUri(String header){
		 Pattern p = Pattern.compile("<.*?>; " + RTSBA_CONTEXT_RELATION);
		 Matcher m = p.matcher(header);
		 if (m.find()) {
		     Matcher m2 = Pattern.compile("<.*?>").matcher(m.group());
		     m2.find();
		     return m2.group().replace("<", "").replace(">", "");
		 }
		 
		 return null;
		 
	}
}
