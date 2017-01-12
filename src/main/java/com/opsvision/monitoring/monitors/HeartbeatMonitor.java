package com.opsvision.monitoring.monitors;

import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;

import com.opsvision.monitoring.utils.PropertyHandler;
import com.opsvision.monitoring.utils.SnmpTrap;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class HeartbeatMonitor extends Monitor {
	private static final Logger logger = Logger.getLogger(HeartbeatMonitor.class);
	private static PropertyHandler props = PropertyHandler.getInstance();
	
	public HeartbeatMonitor() {
		super(MonitorType.Heartbeat);
	}

	@Override
	public void init() {
		rate = Integer.parseInt(props.getValue(
				"monitor.heartbeat.polling.rate", "60"));
		
		enabled = Boolean.parseBoolean(props.getValue(
				"monitor.heartbeat.enabled", "false"));
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
		logger.debug("Sending Heartbeat");
		
		VariableBinding variableBinding = new VariableBinding(
				new OID("1.3.6.1.4.1.44132.4.1.1"), new OctetString("Heartbeat"));
		SnmpTrap.sendTrap(1, new VariableBinding[]{variableBinding});

	}
}
