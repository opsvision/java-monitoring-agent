package com.opsvision.monitoring.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;


public class PropertyHandler {
	final static Logger logger = Logger.getLogger(PropertyHandler.class);
	private static PropertyHandler _instance = null;
	private Properties props = new Properties();

	/**
	 * Singleton constructor
	 */
	private PropertyHandler() {
		logger.debug("Loading application properties");
		try {
			InputStream config = PropertyHandler.class.getClassLoader().getResourceAsStream("config.properties");

			if (config == null) {
				logger.error("Failed to locate the config file (config.properties)");
				System.exit(-1);
			}

			// load the properties
			props.load(config);

			// close the stream
			config.close();

			logger.debug("Loaded " + props.size() + " application properties");

		} catch (IOException e) {
			logger.error("Failed to load config file: " + e.getMessage());
			System.exit(-1);
		}
	}

	/**
	 * Method to return an instance of the PropertyHandler
	 *
	 * @return an instance of the PropertyHandler
	 */
	public static synchronized PropertyHandler getInstance() {
		if (_instance == null)
			_instance = new PropertyHandler();
		return _instance;
	}

	/**
	 * Method for returning a specific Property
	 *
	 * @param propKey The Property key
	 * @return The value of the Property requested
	 */
	public String getValue(String propKey) {
		return props.getProperty(propKey);
	}

	/**
	 * Method for returning a specific Property or a default value if not found
	 *
	 * @param propKey The Property key
	 * @param defaultValue The value to use if the Property key is not found
	 * @return The Property value
	 */
	public String getValue(String propKey, String defaultValue) {
		return props.getProperty(propKey, defaultValue);
	}

	/**
	 * Method for returning multiple Properties using a regular expression
	 *
	 * @param regex A regular expression to use when searching for Property keys
	 * @return The Properties found using the key regular expression
	 */
	public Map<String, String> getProperties(String regex) {
		Map<String, String> properties = new HashMap<String, String>();

		Enumeration<Object> enumeration = props.keys();
		while(enumeration.hasMoreElements()) {
			String key = (String)enumeration.nextElement();
			if (key.matches(regex)) {
				properties.put(key, props.getProperty(key));
			}
		}

		return properties;
	}
}
