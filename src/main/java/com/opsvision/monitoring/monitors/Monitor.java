package com.opsvision.monitoring.monitors;

import org.quartz.Job;

public abstract class Monitor implements Job {
	private MonitorType monitor = null;
	protected int rate = 60;
	protected boolean enabled = false;
	
	public Monitor(MonitorType monitor) {
		this.monitor = monitor;
		init();
	}
	
	public abstract void init();
	public abstract int getRate();
	public abstract boolean isEnabled();
	
	public MonitorType getMonitor() {
		return monitor;
	}
	
	public void setMonitor(MonitorType monitor) {
		this.monitor = monitor;
	}
}
