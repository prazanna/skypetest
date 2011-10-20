package org.skype.test.simulation;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class SingleLineFormatter extends Formatter{

	@Override
	public String format(LogRecord record) {
		return String.format("%1$tY%1$tm%1$td-%1$tH%1$tM%1$tS %2$s %3$s: %4$s\n",
				record.getMillis(),
				record.getLevel(), 
				record.getSourceClassName(),
				record.getMessage()
				);
	}

	public static void main(String[] args) {
		Logger log = Logger.getLogger(SingleLineFormatter.class.getName());

		System.out.println("log="+log);
		for (int i=0; i<3; i++){
			log.info("message "+i);
		}
	}
}
