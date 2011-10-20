package org.skype.test.simulation.model;

import java.util.Random;

import org.skype.test.simulation.message.Message;
import org.skype.test.simulation.message.MessageTransmissionListener;

public class Transport {
	private MessageTransmissionListener messageTransmissionListener;
	private Random random = new Random();

	public Transport(MessageTransmissionListener tListener) {
		this.messageTransmissionListener = tListener;
	}

	public void sendMessage(Node to, Message message) {
		if(random.nextInt(100) < 6) return;
		to.handleMessage(message);
		messageTransmissionListener.onTransmitMessage(message);
	}

}
