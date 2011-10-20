package org.skype.test.simulation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.skype.test.simulation.clock.AlarmHandler;
import org.skype.test.simulation.clock.SimulatedTime;
import org.skype.test.simulation.model.Driver;
import org.skype.test.simulation.model.SimulationSystem;
import org.skype.test.simulation.monit.Monitor;

public class Main {
	private static String NODE_SHORT = "n";
	private static String BUDDY_COUNT_SHORT = "b";
	private static String THREAD_SHORT = "t";
	private static String EXECUTION_SHORT = "t";

	private static Logger LOG = Logger.getLogger(Main.class.getName());

	/**
	 * Initialize the Simulation System and the Drivers in the system
	 * @param nodes - No of Drivers/Nodes to simulate
	 * @param count - No of buddies for each Driver
	 * @param threads - Thread pool size
	 * @param time - Simulation run time
	 */
	public void setup(int nodes, int count, int threads, long time) {
		List<Double> means = new ArrayList<Double>();
		List<Double> messages = new ArrayList<Double>();
		
		for( int times = 0; times < 5; times++ ) {
		// Barrier for all the Drivers and the Nodes to setup and wait for the simulation to start
		CyclicBarrier barrier = new CyclicBarrier(nodes*2);

		final ExecutorService executor = Executors.newFixedThreadPool(threads);
		final Monitor monitor = new Monitor();

		// Fire off the Driver which fire offs Node
		Driver[] drivers = new Driver[nodes];
		for(int i=0;i<nodes;i++)
			drivers[i] = new Driver(executor, i, monitor, monitor, barrier);

		// Set the random buddies to the Drivers
		setupBuddies(drivers, count);
		//for(Driver driver:drivers) driver.printBuddyInfo();
		
		// Fire off the simulation system
		SimulationSystem simulator = new SimulationSystem(executor);
		simulator.handle(drivers);
		final Future<?> simulatorHandle = executor.submit(simulator);

		// Set the alarm for the simulated time on the Simulated Clock
		SimulatedTime.getInstance().registerAlarm(time, new AlarmHandler() {
			public void onRing() {
				LOG.info("Simulator Handle is being interrupted:"+SimulatedTime.getInstance().getCurrentMilliSeconds());
				simulatorHandle.cancel(true);
				executor.shutdown();
			}
		});

		// Let us wait for maximum 5 minutes for the simulation to complete
		try {
			executor.awaitTermination(5, TimeUnit.MINUTES);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		long endTime = SimulatedTime.getInstance().getCurrentMilliSeconds();

		// Print the metrics
		means.add(monitor.getMeanStatusChangeTime(endTime));
		messages.add(monitor.getMessagesPerMinute(endTime, nodes));
		// Shutdown the executor
		executor.shutdownNow();
		SimulatedTime.getInstance().reset();
		
		}
		System.out.println(means);
		System.out.println(messages);
	}

	private void setupBuddies(Driver[] drivers, int count) {
		List<Driver> driversSet = new ArrayList<Driver>(drivers.length);
		for(Driver d:drivers) driversSet.add(d);
		
		while(driversSet.size() > 0) {
			Driver thisDriver = driversSet.iterator().next();;
			List<Driver> subList = Util.getRandomSubset(driversSet, count - thisDriver.getNode().getBuddies().size(), thisDriver, count);
			for(Driver driver:subList) {
				thisDriver.makeBuddy(driver);
				driver.makeBuddy(thisDriver);
				if(driver.getNode().getBuddies().size() == count) driversSet.remove(driver);
			}
			driversSet.remove(thisDriver);
		}

	}

	public static void main(String[] args) {
		CommandLineParser parser = new PosixParser();
		try {
			CommandLine cmd = parser.parse(Main.getOptions(), args);
			int nodes = Integer.parseInt(cmd.getOptionValue(NODE_SHORT, "1000"));
			int count = Integer.parseInt(cmd.getOptionValue(BUDDY_COUNT_SHORT, "20"));
			int threads = Integer.parseInt(cmd.getOptionValue(THREAD_SHORT, "2500"));
			long executionTime = Long.parseLong(cmd.getOptionValue(EXECUTION_SHORT, "7776000000"));
			Main system = new Main();
			LOG.info("Simulation system is being setup with "+nodes+" nodes, buddy count "+count+", thread max count "+threads+" runs for "+executionTime);
			system.setup(nodes, count, threads, executionTime);
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	private static Options getOptions() {
		Options options = new Options();
		options.addOption(NODE_SHORT, "nodes", true, "No of nodes to be simulated");
		options.addOption(BUDDY_COUNT_SHORT, "buddies", true, "Range of buddies chosen at random");
		options.addOption(THREAD_SHORT, "threadsize", true, "Thread size to use");
		options.addOption(EXECUTION_SHORT, "executiontime", true, "Simulation execution time");
		return options;
	}
}
