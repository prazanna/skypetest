package org.skype.test.simulation.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;

public class SimulationSystem implements Runnable {
	private static Logger LOG = Logger.getLogger(SimulationSystem.class.getName());
	private Queue<Driver> pendingQueue = new LinkedList<Driver>();
	private Map<Driver, Future<?>> drivers = new HashMap<Driver, Future<?>>();
	private ExecutorService service;

	public SimulationSystem(ExecutorService service) {
		this.service = service;
	}
	
	/**
	 * Handles the drivers and digests into the system. It does not have any guarantee of bring the drivers up instantly
	 * @param driver Driver to bring into the system
	 */
	public void handle(Driver... driver) {
		pendingQueue.addAll(Arrays.asList(driver));
	}

	public void run() {
		while (!Thread.currentThread().isInterrupted()) {
			for (Driver driver; (driver = pendingQueue.poll()) != null;){
				LOG.info("Launching Driver "+driver.getId());
				drivers.put(driver, service.submit(driver));
			}
		}
		LOG.info("Shutting down simulation system");
		shutdownDrivers();
	}

	private void shutdownDrivers() {
		for(Driver driver:drivers.keySet())
			if(drivers.get(driver) != null)
				drivers.get(driver).cancel(true);
	}

	/**
	 * @return Return a copy of all the Drivers in the system
	 */
	public Driver[] getDrivers(){
		Set<Driver> ds = drivers.keySet();
		return ds.toArray(new Driver[ds.size()]);
	}
}
