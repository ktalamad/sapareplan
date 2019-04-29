package edu.asu.sapa.monitor;

class WaitingMessage implements Message {
//	BlockingQueue<Message> queue;
	long threadID;
	
	public WaitingMessage(long threadID) {
		this.threadID = threadID;
	}
}
