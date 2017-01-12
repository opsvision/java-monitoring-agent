package com.opsvision.monitoring.monitors;

public class MonitorFactory {
	public static Monitor getMonitor(MonitorType type) {
		Monitor monitor = null;
		
		switch(type) {
		case Heartbeat:
			monitor = new HeartbeatMonitor();
			break;
			
		case GatesAir:
			monitor = new GatesAirMonitor();
			break;
			
		case Liebert:
			monitor = new LiebertMonitor();
			break;
			
		case StreamValve:
			monitor = new StreamValveMonitor();
			break;
			
		case Web:
			monitor = new WebMonitor();
			break;
		}
		
		return monitor;
	}
}
