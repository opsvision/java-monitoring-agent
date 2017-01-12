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
public class GatesAirMonitor extends Monitor {
	private static final Logger logger = Logger.getLogger(GatesAirMonitor.class);
	private static PropertyHandler props = PropertyHandler.getInstance();
	private String healthStatusOID = "1.3.6.1.4.1.37504.3.2.1.3.4.1.2.1";
	private String rfPowerOID = "1.3.6.1.4.1.37504.3.2.4.3.1.1.1.1";
	private String host = "localhost";
	private int port = 161;
	private String snmpcomm = "public";

	public GatesAirMonitor() {
		super(MonitorType.GatesAir);
	}
	
	// Internal Enum for health status
	private enum HealthStatus {
		NOT_PRESENT(1), ERROR(2), INITIALIZING(3), IDLE(4),
		OPERATIVE(5), ALARM(6), WARNING(7), FACTORY(8),
		UPGRADING(9), RF_DOWN(10), RF_OFF(11);

		@SuppressWarnings("unused")
		private int value;
		HealthStatus(int value) {
			this.value = value;
		}
	}
	
	// Internal Enum for power status
	private enum PowerStatus {
		UP(1), DOWN(2);

		@SuppressWarnings("unused")
		private int value;
		PowerStatus(int value) {
			this.value = value;
		}
	}

	@Override
	public void init() {
		rate = Integer.parseInt(props.getValue(
				"monitor.gatesair.polling.rate", "60"));

		enabled = Boolean.parseBoolean(props.getValue(
				"monitor.gatesair.enabled", "false"));
		
		host = props.getValue("monitor.gatesair.host", "localhost");
		
		port = Integer.parseInt(props.getValue(
				"monitor.gatesair.port", "161"));
		
		snmpcomm = props.getValue("monitor.gatesair.snmpcomm", "public");
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
		// Perform checks
		checkHealthStatus(context);
		checkRFPowerStatus(context);
	}

	/**
	 *
	 * @param context the context for the Quartz job
	 */
	private void checkHealthStatus(JobExecutionContext context) {
		logger.debug("Performing GatesAir Health Status Check");

		// Default to an operative(5) state
		HealthStatus healthStatus = HealthStatus.OPERATIVE;

		// Fetch the health status and make sure we get a value
		ResponseEvent statusEvent = SnmpGet.doGet(host, Integer.toString(port), snmpcomm, healthStatusOID);
		if (statusEvent == null) {
			logger.error("Failed to get a response from the GatesAir system for HS");
			return;
		}

		// Retrieve the previous health status if known
		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		if (jobDataMap.containsKey("GatesAirHealthStatus")) {
			try {
				healthStatus = (HealthStatus)jobDataMap.get("GatesAirHealthStatus");

			} catch (ClassCastException e) {
				logger.error("Failed to cast the job data properly");
			}
		}

		try {
			int status = statusEvent.getResponse().get(0).getVariable().toInt();

			// The following switch/class block will (order is important)...
			//  (1) evaluate the current status
			//  (2) take appropriate action
			//  (3) capture the value for storage
			switch (status) {
				case 1: // Not Present
					logger.warn("Health Status reports NOT PRESENT - sending warning notification");
					sendHealthTrap(status);
					healthStatus = HealthStatus.NOT_PRESENT;
					break;

				case 2: // Error
					logger.warn("Health Status reports ERROR - sending warning notification");
					sendHealthTrap(status);
					healthStatus = HealthStatus.ERROR;
					break;

				case 3: // Initializing
					logger.warn("Health Status reports INITIALIZING - sending warning notification");
					sendHealthTrap(status);
					healthStatus = HealthStatus.INITIALIZING;
					break;

				case 4:  // Idle
					logger.warn("Health Status reports IDLE - sending warning notification");
					sendHealthTrap(status);
					healthStatus = HealthStatus.IDLE;
					break;

				case 5:  // Operative
					// If the previous state (healthStatus) is not operative, then send a clear
					if (healthStatus != HealthStatus.OPERATIVE) {
						logger.warn("Health Status reports OPERATIVE - sending clear");
						sendHealthTrap(status);
					}
					healthStatus = HealthStatus.OPERATIVE;
					break;

				case 6:  // Alarm
					logger.warn("Health Status reports ALARM - sending notification");
					sendHealthTrap(status);
					healthStatus = HealthStatus.ALARM;
					break;

				case 7:  // Warning
					logger.warn("Health Status reports WARNING - sending notification");
					sendHealthTrap(status);
					healthStatus = HealthStatus.WARNING;
					break;

				case 8: // Factory
					logger.warn("Health Status reports FACTORY - sending warning notification");
					sendHealthTrap(status);
					healthStatus = HealthStatus.FACTORY;
					break;

				case 9: // Upgrading
					logger.warn("Health Status reports UPGRADING - sending warning notification");
					sendHealthTrap(status);
					healthStatus = HealthStatus.UPGRADING;
					break;

				case 10: // rfDown
					logger.warn("Health Status reports RF_DOWN - sending warning notification");
					sendHealthTrap(status);
					healthStatus = HealthStatus.RF_DOWN;
					break;

				case 11: // rfOff
					logger.warn("Health Status reports RF_OFF - sending warning notification");
					sendHealthTrap(status);
					healthStatus = HealthStatus.RF_OFF;
					break;

				default:  // Unknown
					logger.warn("Failed to properly determine health state: " + status);
					sendHealthTrap(status);
					healthStatus = HealthStatus.WARNING;
					break;
			}

		} catch (NullPointerException e) {
			logger.error("Failed to get the Health Status value from the response");
			return;
		}

		// Store the job data for persistence
		context.getJobDetail().getJobDataMap().put("GatesAirHealthStatus", healthStatus);
		logger.debug("GatesAir Health Status Check Completed");
	}

	/**
	 *
	 * @param context the context for the Quartz job
	 */
	private void checkRFPowerStatus(JobExecutionContext context) {
		logger.debug("Performing GatesAir RF Power Status Check");

		// Set the default power state
		PowerStatus powerStatus = PowerStatus.UP;

		ResponseEvent statusEvent = SnmpGet.doGet(host, Integer.toString(port), snmpcomm, rfPowerOID);
		if (statusEvent == null) {
			logger.error("Failed to get a response from the GatesAir system for RF");
			return;
		}

		JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
		if (jobDataMap.containsKey("GatesAirRunningState")) {
			powerStatus = (PowerStatus)jobDataMap.get("GatesAirRunningState");
		}

		try {
			int status = statusEvent.getResponse().get(0).getVariable().toInt();

			switch(status) {
				case 2: // Down
					logger.warn("The GatesAir RF Power state appears to be off, sending notification");
					sendRFTrap(status);
					powerStatus = PowerStatus.DOWN;
					break;

				default:
					if (powerStatus != PowerStatus.UP) {
						logger.info("The GatesAir RF Power state has returned to normal, sending clear notification");
						sendRFTrap(1);
					}
					powerStatus = PowerStatus.UP;
					break;
			}

		} catch (NullPointerException e) {
			logger.error("Failed to retrieve the RF power status");
			return;
		}

		// Store the job data for persistence
		context.getJobDetail().getJobDataMap().put("GatesAirRunningState", powerStatus);
		logger.debug("GatesAir RF Power Status Check Completed");
	}

	/**
	 * This is a helper method for DRY coding
	 *
	 * @param status the integer value representing the RF status
	 */
	private void sendRFTrap(int status) {
		VariableBinding variableBinding = new VariableBinding(
				new OID("1.3.6.1.4.1.44132.4.1.7"),new Integer32(status));
		SnmpTrap.sendTrap(6, new VariableBinding[]{variableBinding});
	}

	/**
	 * This is a helper method for DRY coding
	 *
	 * @param status the integer value representing the current health status
	 */
	private void sendHealthTrap(int status) {
		VariableBinding variableBinding = new VariableBinding(
				new OID("1.3.6.1.4.1.44132.4.1.8"),new Integer32(status));
		SnmpTrap.sendTrap(7, new VariableBinding[]{variableBinding});
	}

}
