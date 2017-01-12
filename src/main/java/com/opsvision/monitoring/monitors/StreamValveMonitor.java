package com.opsvision.monitoring.monitors;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.apache.log4j.Logger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

import com.opsvision.monitoring.utils.PropertyHandler;
import com.opsvision.monitoring.utils.SnmpTrap;

@PersistJobDataAfterExecution
@DisallowConcurrentExecution
public class StreamValveMonitor extends Monitor {
	private static final Logger logger = Logger.getLogger(StreamValveMonitor.class);
	private static PropertyHandler props = PropertyHandler.getInstance();
	private int port = 5003;
	private int timeout = 15000;
	private int specificTrap = 2;
	private int maxsize  = 1024;
	private boolean isRunning = true;

	public StreamValveMonitor() {
		super(MonitorType.StreamValve);
	}

	@Override
	public void init() {
		rate = Integer.parseInt(props.getValue(
				"monitor.streamvalve.polling.rate", "60"));

		enabled = Boolean.parseBoolean(props.getValue(
				"monitor.streamvalve.enabled", "false"));
		
		port = Integer.parseInt(props.getValue(
				"monitor.streamvalve.port", "5003"));
		
		timeout = Integer.parseInt(props.getValue(
				"monitor.streamvalve.timeout", "3000"));	
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
		JobDataMap map = context.getJobDetail().getJobDataMap();

		if (map.containsKey("StreamValveRunningState")) {
			isRunning = map.getBooleanFromString("StreamValveRunningState");
		}
		
		// Get a Datagram Socket and attempt to listen for a data stream
		try (DatagramSocket socket = new DatagramSocket(port)) {
			socket.setSoTimeout(timeout);

			byte[] buffer = new byte[maxsize];
			DatagramPacket packet = new DatagramPacket(buffer, maxsize);

			try {
				socket.receive(packet);

				// If the previous state is false, then we need to send a clear
				if (!isRunning) {
					logger.info("StreamValve return to normal, sending clear notification");
					VariableBinding variableBinding = new VariableBinding(
							new OID("1.3.6.1.4.1.44132.4.1.2"), new Integer32(1));
					SnmpTrap.sendTrap(specificTrap, new VariableBinding[]{variableBinding});
				}

				// Set the state
				context.getJobDetail().getJobDataMap().putAsString("StreamValveRunningState", true);

			} catch (IOException e) {
				// If we get here, we were unable to detect a video stream
				// so we will send a trap to inform the NMS system. First
				// We create the appropriate VariableBinding and then pass
				// that to the SnmpTrap wrapper for submission.
				logger.warn("Unable to detect video stream, sending notification");
				VariableBinding variableBinding = new VariableBinding(
						new OID("1.3.6.1.4.1.44132.4.1.2"),new Integer32(2));
				SnmpTrap.sendTrap(specificTrap, new VariableBinding[]{variableBinding});

				// Set the state
				context.getJobDetail().getJobDataMap().putAsString("StreamValveRunningState", false);
			}

		} catch (SocketException e) {
			logger.error(e.getMessage());
			return;
		}

		logger.debug("StreamValve video check completed");
	}

}
