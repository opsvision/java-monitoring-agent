package com.opsvision.monitoring.monitors;

import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;

import com.opsvision.monitoring.utils.PropertyHandler;
import com.opsvision.monitoring.utils.SnmpTrap;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class WebMonitor extends Monitor {
	private static final Logger logger = Logger.getLogger(WebMonitor.class);
	private static PropertyHandler props = PropertyHandler.getInstance();
	private String host = "localhost";
	private int port = 80;
	private String username = "admin";
	private String password = "password";
	private double freqlow = 47.81;
	private double freqhigh = 57.83;
	private boolean previousState = true;
	private boolean currentState = true;

	public WebMonitor() {
		super(MonitorType.Web);
	}

	@Override
	public void init() {
		rate = Integer.parseInt(props.getValue(
				"monitor.web.om.polling.rate", "60"));

		enabled = Boolean.parseBoolean(props.getValue(
				"monitor.web.om.enabled", "false"));
		
		host = props.getValue("monitor.web.om.host", "localhost");
		
		port = Integer.parseInt(props.getValue(
				"monitor.web.om.port", "80"));
		
		username = props.getValue("monitor.web.om.username", "admin");
		
		password = props.getValue("monitor.web.om.password", "password");
		
		freqlow = Double.parseDouble(props.getValue(
				"monitor.web.om.freqlow", "47.81"));
		
		freqhigh = Double.parseDouble(props.getValue(
				"monitor.web.om.freqhigh", "57.83"));
	}
	
	@Override
	public int getRate() {
		return rate;
	}
	
	@Override
	public boolean isEnabled() {
		return enabled;
	}

	public void execute(JobExecutionContext context) throws JobExecutionException {
		logger.debug("Starting O&M Web status checks");

		JobDataMap map = context.getJobDetail().getJobDataMap();
		if (map.containsKey("OMWebRunningState")) {
			previousState = map.getBooleanFromString("OMWebRunningState");
		}

		try {
			String url = "http://" + host + ":" + port
					+ "/cgi_ipradio?type=103";

			// Log into the site using the credentials
			logger.debug("Attempting login as " + username + " @ " + url);
			Connection.Response response = Jsoup.connect(url)
					.data("username", username).data("password", password)
					.data("type", "10").userAgent("Mozilla")
					.method(Connection.Method.POST).timeout(30 * 1000) // 30
																		// seconds
					.execute();

			// Extract the cookies from the login; we should have one cookie
			Map<String, String> loginCookies = response.cookies();
			if (loginCookies.size() == 0) {
				logger.error("Failed to log into O&M web page");
				return;
			}

			logger.debug("Fetching page using stored login cookies");
			Document document = Jsoup.connect(url).cookies(loginCookies).get();

			// Make sure we actually retrieved a document
			if (document == null) {
				logger.error("Failed to retrieve O&M web page content");
				return;
			}

			// Extract the <td> tags
			Elements tds = document.getElementsByTag("td");
			// logger.debug("Found " + tds.size() + " <td> tags");

			for (Element td : tds) {
				if (td.text().contains("Forward RF Level")) {
					Element sibling = td.nextElementSibling();
					String value = sibling.text().split("\\s+")[0];
					if (value == null) {
						logger.error("Failed to extract the Forward RF Level value");
						return;
					}

					logger.debug("Found Forward RF Level: " + value);
					logger.debug("Measuring against nominal range:" + freqlow
							+ " to " + freqhigh);

					// Forward RF Level is low
					if (Float.parseFloat(value) <= freqlow) {
						logger.debug("Forward RF Level low, sending trap");
						trapWrapper(3, value);

						currentState = false;

						// Forward RF Level is high
					} else if (Float.parseFloat(value) >= freqhigh) {
						logger.debug("Forward RF Level high, sending trap");
						trapWrapper(2, value);

						currentState = false;

						// Forward RF Level is normal
					} else {
						logger.debug("Forward RF Level is nominal");

						// If the current value is within nominal range, but the
						// previous status is false, then we send a clear event
						if (!previousState) {
							logger.debug("Forward RF Level has return to nominal, sending clear");
							trapWrapper(1, value);
						}

						currentState = true;
					}

					// Once we find the appropriate tag,
					// we can leave the for() loop
					break;
				}
			}

		} catch (IOException e) {
			logger.error("There was an error processing the O&M web page: "
					+ e.getMessage());
			return;
		}

		// Save the state
		context.getJobDetail().getJobDataMap().putAsString("OMWebRunningState", currentState);
		logger.debug("O&M Web status checks completed");
	}

	/**
	 * This method is a wrapper for sending a trap so we just have to supply the
	 * state and value.
	 *
	 * @param state The current state of the component
	 * @param value  the current value of the component
	 */
	private void trapWrapper(int state, String value) {
		// State
		VariableBinding variableBinding1 = new VariableBinding(new OID(
				"1.3.6.1.4.1.44132.4.1.5"), new Integer32(state));

		// Value
		VariableBinding variableBinding2 = new VariableBinding(new OID(
				"1.3.6.1.4.1.44132.4.1.6"), new OctetString(
				String.valueOf(Float.parseFloat(value))));

		// Trap
		SnmpTrap.sendTrap(5, new VariableBinding[] {
				variableBinding1, variableBinding2 });
	}
}
