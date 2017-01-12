package com.opsvision.monitoring.monitors;

import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import com.opsvision.monitoring.utils.PropertyHandler;
import com.opsvision.monitoring.utils.SnmpGet;
import com.opsvision.monitoring.utils.SnmpTrap;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class LiebertMonitor extends Monitor {
	private static final Logger logger = Logger.getLogger(LiebertMonitor.class);
	private static PropertyHandler props = PropertyHandler.getInstance();
	private JobDataMap map = null;
	private String host = "localhost";
	private int port = 3027;
	private String snmpcomm = "LiebertEM";

	public LiebertMonitor() {
		super(MonitorType.Liebert);
	}

	@Override
	public void init() {
		rate = Integer.parseInt(props.getValue(
				"monitor.liebert.polling.rate", "60"));

		enabled = Boolean.parseBoolean(props.getValue(
				"monitor.liebert.enabled", "false"));
		
		host = props.getValue("monitor.liebert.host", "localhost");
		
		port = Integer.parseInt(props.getValue(
				"monitor.liebert.port", "3027"));
		
		snmpcomm = props.getValue("monitor.liebert.snmpcomm", "LiebertEM");
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
		// Fetch the persistent job data
		map = context.getJobDetail().getJobDataMap();

		// Check the upsBatteryStatus and save the state
		int batteryStatus = checkBatteryStatus(map);
		context.getJobDetail().getJobDataMap().put("LiebertBatteryStatus", batteryStatus);

		// Check the upsOutputSource
		int outputSource = checkOutputSource(map);
		context.getJobDetail().getJobDataMap().put("LiebertOutputSource", outputSource);
	}

	/**
	 * Method for performing an SNMP get for the battery status
	 */
	private int checkBatteryStatus(JobDataMap map) {
		logger.debug("Performing Liebert UPS battery status check");

		// Get the previous battery status value from the job details
		int previousBatteryStatus = 2 ;  // start off normal
		if (map.containsKey("LiebertBatteryStatus")) {
			previousBatteryStatus = map.getIntValue("LiebertBatteryStatus");
		}

		ResponseEvent upsBatteryStatusEvent = SnmpGet.doGet(host, Integer.toString(port), snmpcomm, "1.3.6.1.2.1.33.1.2.1.0");
		if (upsBatteryStatusEvent == null) {
			logger.error("Failed to retrieve the Liebert UPS battery status");
			return 1; // unknown
		}

		int batteryStatus = upsBatteryStatusEvent.getResponse().get(0).getVariable().toInt();
		if (batteryStatus != 2) {
			// Send the status value
			logger.warn("Detected non-normal battery status, sending notification");
			VariableBinding variableBinding = new VariableBinding(
					new OID("1.3.6.1.4.1.44132.4.1.3"),new Integer32(batteryStatus));
			SnmpTrap.sendTrap(3, new VariableBinding[]{variableBinding});

		} else {
			if (previousBatteryStatus != 2) {
				// Clear Trap
				logger.info("Battery status return to normal, sending clear notification");
				VariableBinding variableBinding = new VariableBinding(
						new OID("1.3.6.1.4.1.44132.4.1.3"),new Integer32(2));
				SnmpTrap.sendTrap(3, new VariableBinding[]{variableBinding});
			}
		}

		logger.debug("Liebert UPS battery status check completed");
		return batteryStatus;
	}

	/**
	 * Method for performing an SNMP get for the output source
	 */
	private int checkOutputSource(JobDataMap map) {
		logger.debug("Performing Liebert UPS output source check");

		// Get the previous output source value from the job details
		int previousOutputSource = 3; // start off normal
		if (map.containsKey("LiebertOutputSource")) {
			previousOutputSource = map.getIntValue("LiebertOutputSource");
		}

		// upsOutputSource
		ResponseEvent upsOutputSourceEvent = SnmpGet.doGet(host, Integer.toString(port), snmpcomm, "1.3.6.1.2.1.33.1.4.1.0");
		if (upsOutputSourceEvent == null) {
			logger.error("Failed to retrieve the Liebert UPS output status");
			return 1; // other
		}

		int outputSource = upsOutputSourceEvent.getResponse().get(0).getVariable().toInt();
		if (outputSource != 3) {
			logger.warn("Detected non-normal output source, sending notification");
			VariableBinding variableBinding = new VariableBinding(
					new OID("1.3.6.1.4.1.44132.4.1.4"),new Integer32(outputSource));
			SnmpTrap.sendTrap(4, new VariableBinding[]{variableBinding});

		} else {
			if (previousOutputSource != 3) {
				// Clear
				logger.info("Output source return to normal, sending clear notification");
				VariableBinding variableBinding = new VariableBinding(
						new OID("1.3.6.1.4.1.44132.4.1.4"), new Integer32(3));
				SnmpTrap.sendTrap(4, new VariableBinding[]{variableBinding});
			}
		}

		logger.debug("Liebert UPS output source check completed");
		return outputSource;
	}
}
