package org.opentosca.nodetypeimplementations;

/**
 * Logger for displaying messages in IAs
 *
 * Created by Marc Schmid on 16.09.18.
 */
public class IALogger {

	//the class using the logger
	private Class classz;

	/**
	 * Create object of the logger
	 * @param classz the class the logger is created
	 */
	public IALogger(Class classz){
		this.classz = classz;
	}

	/**
	 * Print out the message with the classname in the std
	 * @param message the message to pring
	 */
	public void debug(String message){
		String className = classz.getName();
		System.out.println(className + ": " + message);
	}

}
