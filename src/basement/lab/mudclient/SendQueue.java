package basement.lab.mudclient;

import java.util.LinkedList;

public class SendQueue {
	private LinkedList<Object> queueList;

	SendQueue() {
		queueList = new LinkedList<Object>();
	}

	public synchronized Object dequeue() {
		while (isEmpty()) {
			try {
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		Object obj = queueList.removeFirst();
		notify();
		return obj;
	}

	public synchronized void enqueue(Object element) {
		queueList.addLast(element);
		notify();
	}

	public boolean isEmpty() {
		return queueList.size() == 0;
	}

}
