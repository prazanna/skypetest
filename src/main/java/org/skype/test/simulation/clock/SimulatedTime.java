package org.skype.test.simulation.clock;

import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.skype.test.simulation.model.SimulationSystem;


public class SimulatedTime {
	private final static SimulatedTime singleton = new SimulatedTime();
	private static Logger LOG = Logger.getLogger(SimulationSystem.class.getName());
	private AtomicLong nodeSkipCount = new AtomicLong(0);
	private AtomicLong nodeSkipTotal = new AtomicLong(0);
	private AtomicLong driverSkipCount = new AtomicLong(0);
	private AtomicLong driverSkipTotal = new AtomicLong(0);
	
	private SimulatedTime() {
		
	}
	
	public static SimulatedTime getInstance() {
		return singleton;
	}
	
	private volatile AtomicLong time = new AtomicLong(0);
	private volatile long alarmTime = Long.MAX_VALUE;
	private volatile AlarmHandler handler;
	
	public long getCurrentMilliSeconds(){
		return time.get();
	}

	/**
	 * We do not need the overhead of synchronization. increment is a frequent operation.
	 * If we have weak guarantee on alarm we can fasten up things.
	 * @param period
	 */
	public void increment(long period, boolean isDriver) {
		LOG.fine("Clock fastforward by "+period+"ms. New Time "+time.get());
		time.addAndGet(period);
		if(isDriver) {
			driverSkipCount.incrementAndGet();
			driverSkipTotal.addAndGet(period);
		}else{
			nodeSkipCount.incrementAndGet();
			nodeSkipTotal.addAndGet(period);
		}
		synchronized(time) {
			long current = time.get();
			if(handler != null && current > alarmTime) {
				synchronized(handler) {
					if(handler != null) {
						handler.onRing();
						handler = null;
					}
				}
			}
		}
	}

	/**
	 * This only provides a weak guarantee than the alarm will be off after the alarmtimeset.
	 * It does not guarantee that alarm will be set off immediately after the time first reaches beyond alarm time
	 * @param period
	 */	
	public void registerAlarm(long time, AlarmHandler handler) {
		alarmTime = time;
		this.handler = handler;
	}

	public void setCurrentTimeMilliSeconds(long expectedTime, long newTime) {
		time.compareAndSet(expectedTime, newTime);
	}

	public void reset() {
		time.set(0);
		nodeSkipCount.set(0);
		nodeSkipTotal = new AtomicLong(0);
		driverSkipCount = new AtomicLong(0);
		driverSkipTotal = new AtomicLong(0);		
		alarmTime = Long.MAX_VALUE;
		handler = null;
	}
	
	public long getDriverSkipTotal() {
		return driverSkipTotal.get();
	}
	public double getNodeSkipPropotion() {
		return (double)nodeSkipTotal.get() / time.get();
	}

	public double getSkipCount() {
		return nodeSkipCount.get() + driverSkipCount.get();
	}
}
