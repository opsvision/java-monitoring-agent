package com.opsvision.monitoring;

import org.apache.log4j.Logger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import com.opsvision.monitoring.monitors.Monitor;
import com.opsvision.monitoring.monitors.MonitorFactory;
import com.opsvision.monitoring.monitors.MonitorType;

public class MonitoringAgent {
	
	private final static String VERSION = "1.0"; 
	
	private static final Logger logger = Logger.getLogger(MonitoringAgent.class);
	private static Scheduler scheduler = null;
	private static boolean stopRequested = false;
	private static boolean isRunning = false;

	public static void start(String[] args) {
		logger.debug("Monitoring Agent: " + VERSION);
		
		try {
			// Get a handle for the Scheduler
			scheduler = new StdSchedulerFactory().getScheduler();

			// Start the Scheduler
			scheduler.start();
			
			// Add enabled jobs to the scheduler
			for (MonitorType type : MonitorType.values()) {
				Monitor m = MonitorFactory.getMonitor(type);
				if (m.isEnabled()) {
					logger.debug(type + " is enabled");
					addJobTrigger(scheduler, m.getClass(), m.getRate());
				}
			}
			
			// Toggle the running flag
			isRunning = true;
			
		} catch(SchedulerException e) {
			logger.error(e.getMessage());
		}
	}

	/**
	 * This method is used to stop our agent activities. If using the
	 * Apache Commons - Procrun utility to wrap this as a service, 
	 * then this method is called to shutdown the agent.
	 * 
	 * @param args unused
	 */
	public static void stop(String[] args) {
		logger.debug("Modeo Agent shutting down");

		// Toggle the stop flag
		stopRequested = true;

		if (scheduler != null) {
			try {
				scheduler.shutdown(true);

			} catch (SchedulerException ignore) {
				logger.warn(ignore.getMessage());
			}
		}
	}

	/**
	 * If you are using the Advanced Installer to wrap this agent
	 * so it can run as a Windows service, then this method is
	 * used to stop the agent activities.
	 *
	 */
	public static void stop() {
		stop(new String[] { "Args Not Used" });
	}

	/**
	 * The main entry point from the command line. We will simply
	 * call the {@link #start(String[])} method.
	 * 
	 * @param args unused
	 */
	public static void main(String[] args) {
		logger.info("Starting Modeo Agent, Version " + VERSION);

		// We have to go into a loop so that the Windows service
		// doesn't exit after starting
		while(!stopRequested) {
			while(!isRunning) {
				start(args);
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	/**
	 * This method is used to create both a JobDetail and Trigger. The method
	 * then adds the JobDetail and Trigger to the Scheduler. Note that we are
	 * suppressing warnings for using raw types and checking so we need to be
	 * careful about sending classes that do not implement the Job interface.
	 * 
	 * @param scheduler An instance of the Quartz Scheduler
	 * @param jobClass A class implementing the Quartz Job interface
	 * @param rate An integer value representing the seconds between job runs
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static void addJobTrigger(Scheduler scheduler, Class jobClass, Integer rate) {
		// Create a job detail
		JobDetail job = JobBuilder.newJob(jobClass)
				.withIdentity(jobClass.getSimpleName(), "modeo").build();

		// Create the trigger
		Trigger trigger = TriggerBuilder
				.newTrigger()
				.withIdentity(jobClass.getSimpleName() + "Trigger", "modeo")
				.withSchedule(
						SimpleScheduleBuilder.simpleSchedule()
								.withIntervalInSeconds(rate).repeatForever())
				.build();

		try {
			scheduler.scheduleJob(job, trigger);

		} catch (SchedulerException e) {
			logger.error(e.getMessage());
		}
	}
}
