package org.skype.test.simulation.monit;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.skype.test.simulation.clock.SimulatedTime;
import org.skype.test.simulation.message.Message;
import org.skype.test.simulation.message.MessageTransmissionListener;

public class Monitor implements MessageTransmissionListener, StatusChangeListener {
	private volatile Map<String, Long> correlationMap = new ConcurrentHashMap<String, Long>();
	private volatile AtomicLong statusChangeCount = new AtomicLong(1L);
	private volatile AtomicLong totalStatusChangeTime = new AtomicLong(1L);
	private volatile AtomicLong messageCount = new AtomicLong(0);
	private static final float STATUS_CHANGE_AFFECT_PROPABILITY = 0.2f;
	private static final double MINUTE = 60*1000;
	
	public void onTransmitMessage(Message message) {
		messageCount.incrementAndGet();
	}
	public void onStateChange(Integer node, UUID correlation) {
		correlationMap.put(node + "__" +correlation.toString(), SimulatedTime.getInstance().getCurrentMilliSeconds());
	}
	public void onBuddyStateChange(int sourceNodeId, UUID correlation, int node) {
		Long stateChangeTime = correlationMap.get(sourceNodeId + "__" + correlation.toString());
		Long elapsed = SimulatedTime.getInstance().getCurrentMilliSeconds() - stateChangeTime;
		totalStatusChangeTime.addAndGet(elapsed);
		statusChangeCount.getAndIncrement();
	}

	
	public double getMeanStatusChangeTime(long endTime) {
		// A lot of elapsed time will be affected by clock skipping. Lets introduce some correction
		double averageSkip = (double)endTime / SimulatedTime.getInstance().getSkipCount();
		System.out.println("Average Skip - "+averageSkip);
		double propableAffectedStatusChangeCount = statusChangeCount.get() * STATUS_CHANGE_AFFECT_PROPABILITY;
		double correctedTotalStatusChangeTime = totalStatusChangeTime.get() - (averageSkip * propableAffectedStatusChangeCount);
		System.out.println("Corrected - "+correctedTotalStatusChangeTime+", original "+totalStatusChangeTime.get());
		return correctedTotalStatusChangeTime / totalStatusChangeTime.get();
	}

	public double getMessagesPerMinute(long endTime, int totalNodes) {
		double nodeOperationTime = endTime - SimulatedTime.getInstance().getDriverSkipTotal();
		double result = ((double)messageCount.get() / (nodeOperationTime)) * MINUTE * totalNodes;
		return result;
	}

}
