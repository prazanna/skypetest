package org.skype.test.simulation.message;

import java.util.UUID;

import org.skype.test.simulation.clock.SimulatedTime;
import org.skype.test.simulation.model.Node.NodeState;

public class Message {
	private int from;
	private int to;
	private long messageTime;
	private UUID correlation;
	private NodeState newState;
	
	public Message(int fromId, int toId, NodeState state, UUID correlation) {
		this.from = fromId;
		this.to = toId;
		this.newState = state;
		this.correlation = correlation;
		messageTime = SimulatedTime.getInstance().getCurrentMilliSeconds();
	}

	public int getFrom() {
		return from;
	}

	public int getTo() {
		return to;
	}

	public long getMessageTime() {
		return messageTime;
	}

	public UUID getCorrelation() {
		return correlation;
	}

	public NodeState getNewState() {
		return newState;
	}
	
	

}
