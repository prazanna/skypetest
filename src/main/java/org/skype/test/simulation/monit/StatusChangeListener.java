package org.skype.test.simulation.monit;

import java.util.UUID;


public interface StatusChangeListener {
	void onStateChange(Integer node, UUID correlation);
	void onBuddyStateChange(int sourceNodeId, UUID correlation, int node);
}
