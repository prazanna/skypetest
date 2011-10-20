package org.skype.test.simulation.model;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;
import org.skype.test.simulation.clock.SimulatedTime;
import org.skype.test.simulation.message.Message;
import org.skype.test.simulation.message.MessageTransmissionListener;
import org.skype.test.simulation.monit.StatusChangeListener;

/**
 * Invariants
 * - If lastStateChangeCorrelation changes then the monitor knows about it
 * - Only one thread can modify a node state, since new state is dependent on oldstate 
 * Non Invariants
 * - Buddies Map and the Buddies State Map does not have to be in sync. There might be more buddies than there are in the buddy state map. We do not do anything based on the buddy state so its okay.
 * - We do not support buddy Removal. So its okay to lookup the buddy map while iterating on the lookup map
 * 
 * @author prajaper
 *
 */
public class Node implements Runnable {
	public enum NodeState {
		ONLINE, OFFLINE, UNKNOWN
	}

	private static final long MINUTE = 60000L;

	private volatile NodeState state = NodeState.UNKNOWN;
	private volatile AtomicReference<UUID> lastStateChangeCorrelation = new AtomicReference<UUID>(UUID.randomUUID());
	private volatile ConcurrentMap<Integer, Node> buddies = new ConcurrentHashMap<Integer, Node>();
	private volatile ConcurrentMap<Integer, NodeState> states = new ConcurrentHashMap<Integer, NodeState>();
	private Transport transport;
	private Integer id;
	private static Logger LOG = Logger.getLogger(SimulationSystem.class.getName());
	private MessageTransmissionListener transmissionListener;
	private StatusChangeListener statusChangeListener;
	private CyclicBarrier barrier;

	private final ReentrantReadWriteLock messagesLockDuringStatusUpdate = new ReentrantReadWriteLock(true);

	public Node(Integer id, MessageTransmissionListener tListener, StatusChangeListener sListener, CyclicBarrier barrier) {
		this.id = id;
		this.transmissionListener = tListener;
		this.statusChangeListener = sListener;		
		this.barrier = barrier;
		transport = new Transport(transmissionListener);
	}

	public void changeState(NodeState state) {
		messagesLockDuringStatusUpdate.writeLock().lock();
		this.state = state;
		lastStateChangeCorrelation.set(UUID.randomUUID());
		statusChangeListener.onStateChange(id, lastStateChangeCorrelation.get());
		messagesLockDuringStatusUpdate.writeLock().unlock();
	}

	public void switchState() {
		messagesLockDuringStatusUpdate.writeLock().lock();
		switch(state) {
		case OFFLINE:
			state = NodeState.ONLINE;
			break;
		case ONLINE:
			state = NodeState.OFFLINE;
			break;
		}
		lastStateChangeCorrelation.set(UUID.randomUUID());
		statusChangeListener.onStateChange(id, lastStateChangeCorrelation.get());
		messagesLockDuringStatusUpdate.writeLock().unlock();

	}

	public void run() {
		// Count cleaner
		try {
			// Lets wait for all the Drivers and Nodes to reach this point
			barrier.await();
			// Get the time we are starting this buddy message iteration. track the message count
			//			long startTime = SimulatedTime.getInstance().getCurrentMilliSeconds();
			int messageCount = 0;
			// While the thread is not interrupted
			while(!Thread.currentThread().isInterrupted()) {
				for(Integer nodeId:buddies.keySet()) {
					// If the message count reaches 5 then we cannot send any more message for the next remaining seconds till its one min form the start time
					if(messageCount > 4) {
						SimulatedTime.getInstance().increment(MINUTE, false);
						messageCount = 0;
					}
					// We do not support buddy Removal. So its okay to lookup the buddy map while iterating on the lookup map
					Node node = buddies.get(nodeId);
					messagesLockDuringStatusUpdate.readLock().lock();

					// We now can send the message to this buddy. Send a Throw Ball message.
					Message message = new Message(id, nodeId, state, lastStateChangeCorrelation.get());
					LOG.info("Sending state update from "+id+" to "+nodeId);
					transport.sendMessage(node, message);
					messageCount = messageCount + 1;

					messagesLockDuringStatusUpdate.readLock().unlock();
				}
			}
			LOG.info("Node "+id+" has been interrupted");
		} catch(Throwable e) {
			LOG.severe("Node "+id+" died. Reason "+e.toString());
			e.printStackTrace();
		}
	}

	/**
	 * Handle the message the transport layer gives it to the node
	 * @param message - Message to be handled
	 * @return Response Message or <code>null</code> if there is no response
	 */
	public void handleMessage(Message message) {
		int sourceNodeId = message.getFrom();
		states.replace(sourceNodeId, message.getNewState());
		statusChangeListener.onBuddyStateChange(sourceNodeId, message.getCorrelation(), id);
	}

	public int getId(){
		return id;
	}
	public NodeState getState(){
		return state;
	}
	public boolean equals(Object o) {
		if(!(o instanceof Node)) return false;
		Node another = (Node) o;
		return another.id == this.id;
	}
	public int hashCode(){
		return id.hashCode();
	}
	public String toString() {
		return id.toString();
	}

	public void makeBuddy(Node node) {
		buddies.put(node.getId(), node);
		// We may have someone updated the buddy state already
		states.putIfAbsent(node.getId(), NodeState.UNKNOWN);
	}

	public void printBuddyInfo() {
		StringBuilder builder = new StringBuilder();
		builder.append("Node "+id+":{");
		for(Integer buddyId:buddies.keySet()) builder.append(buddyId+",");
		builder.append("}");
		System.out.println(builder.toString());
	}

	public boolean isBuddy(Node node) {
		return buddies.containsKey(node.id);
	}

	public Set<Integer> getBuddies() {
		return buddies.keySet();
	}

}
