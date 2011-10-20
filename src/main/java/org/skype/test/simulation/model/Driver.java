package org.skype.test.simulation.model;

import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import org.skype.test.simulation.clock.SimulatedTime;
import org.skype.test.simulation.message.MessageTransmissionListener;
import org.skype.test.simulation.model.Node.NodeState;
import org.skype.test.simulation.monit.StatusChangeListener;

public class Driver implements Runnable {
	private static final int MAX_SLEEP_TIME = 4000; 
	private static final long SECOND = 1000; 
	
	private ExecutorService service;
	private Integer id;
	private Node node;
	private MessageTransmissionListener transmissionListener;
	private StatusChangeListener statusChangeListener;
	private CyclicBarrier barrier;
	private Future<?> nodeHandle;
	private static Logger LOG = Logger.getLogger(SimulationSystem.class.getName());
	private Random random = new Random();


	public Driver(ExecutorService service, Integer id, MessageTransmissionListener tListener, StatusChangeListener sListener, CyclicBarrier barrier) {
		this.service = service;
		this.id = id;
		this.transmissionListener = tListener;
		this.statusChangeListener = sListener;
		this.barrier = barrier;
		// Create the node for the driver
		node = new Node(id, transmissionListener, statusChangeListener, barrier);
	}

	public void run() {
		try {
			// Randomly choose its initial state
			node.changeState((random.nextBoolean())?NodeState.ONLINE:NodeState.OFFLINE);
			LOG.info("Launching Node "+node.getId());
			nodeHandle = service.submit(node);
			// Wait for all the Drivers and the Nodes to be launched
			barrier.await();
			
			// While the Driver is not interrupted
			while(!Thread.currentThread().isInterrupted()) {
				// Simulate the sleep for random 0 to 4000 seconds
				long sleepTime = random.nextInt(MAX_SLEEP_TIME) * SECOND;
				SimulatedTime.getInstance().increment(sleepTime, true);
				// Forward the clock and switch state
				node.switchState();
				// Give chance for the node threads to execute
				Thread.yield();
			}
			nodeHandle.cancel(true);
		}catch(Throwable e){
			LOG.severe(e.toString());
			e.printStackTrace();
		}
	}

	public Node getNode() {
		return node;
	}
	public Integer getId() {
		return id;
	}
	public boolean equals(Object o) {
		if(!(o instanceof Driver)) return false;
		Driver another = (Driver) o;
		return this.id == another.id;
	}
	public int hashCode() {
		return this.id.hashCode();
	}
	public String toString() {
		return id.toString();
	}

	public void makeBuddy(Driver driver) {
		node.makeBuddy(driver.node);
	}

	public void printBuddyInfo() {
		node.printBuddyInfo();
	}

	public boolean isBuddy(Driver driver) {
		return node.isBuddy(driver.node);
	}

}
