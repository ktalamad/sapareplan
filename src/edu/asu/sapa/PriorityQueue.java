package edu.asu.sapa;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class PriorityQueue<E extends Comparable<? super E>> implements
		Cloneable, java.util.Queue<Priority<E>> {
	protected static final int DEFAULT = 10;
	protected static final int GROW_BY = 20;
	protected static final int RESERVE = 2;

	protected int size = 0;
	protected Priority<E>[] events;
	protected Class<Priority<E>> typePriorityE;

	protected PriorityQueue() {
		// subclasses have to initialize events;
	}

	public PriorityQueue(Priority<E> e) {
		this.typePriorityE = (Class<Priority<E>>) e.getClass();
		events = (Priority<E>[])Array.newInstance(typePriorityE, DEFAULT);
		events[0] = e;
	}

	public PriorityQueue(Class<Priority<E>> c) {
		this.typePriorityE = c;
		events =(Priority<E>[]) Array.newInstance(c, DEFAULT);
	}

	public PriorityQueue(Class<Priority<E>> c, int capacity) {
		if (capacity < DEFAULT) {
			capacity = DEFAULT;
		}
		this.typePriorityE = c;
		events = (Priority<E>[])Array.newInstance(c, capacity);
	}

	public PriorityQueue(PriorityQueue<E> queue) {
		size = queue.size;
//		int capacity = Math.min(size + 5, queue.events.length);
		typePriorityE = queue.typePriorityE;
		events = (Priority<E>[])Array.newInstance(typePriorityE, size+RESERVE);
		for (int i = 0; i < size; ++i) {
			events[i] = queue.events[i].clone();
		}
	}

	public boolean add(E event, float p) {
		if (size >= events.length) {
			growArray();
		}
		Priority<E> t;
		if (events[size] == null) {
			t = new Priority<E>();
		} else {
			t = events[size];
		}
		t.set(event, p);
		int i = size++;
		int parent;
		for (; (parent = (i - 1) >> 1) >= 0 && t.compareTo(events[parent]) < 0; i = parent) {
			events[i] = events[parent];
		}
		events[i] = t;
		return true;
	}

	public boolean add(Priority<E> te) {
		if (size >= events.length) {
			growArray();
		}
		int i = size++;
		int parent;
		for (; (parent = (i - 1) >> 1) >= 0 && te.compareTo(events[parent]) < 0; i = parent) {
			events[i] = events[parent];
		}
		events[i] = te;
		return true;
	}

	public boolean addAll(Collection<? extends Priority<E>> c) {
		if (c == this)
			throw new IllegalArgumentException();
		int csize = c.size();
		if (size + csize > events.length)
			events = Arrays.copyOf(events, size + csize);
		Iterator<? extends Priority<E>> e = c.iterator();
		if (!e.hasNext())
			return false;
		Priority<E> te;
		int i, parent;
		do {
			te = e.next();
			i = size++;
			for (; (parent = (i - 1) >> 1) >= 0 && te.compareTo(events[parent]) < 0; i = parent) {
				events[i] = events[parent];
			}
			events[i] = te;
		} while (e.hasNext());

		return true;
	}

	public void clear() {
		// recycle memory aggressively
		size = 0;
	}

	@Override
	public Object clone() {
		PriorityQueue<E> o = null;
		try {
			o = (PriorityQueue<E>) super.clone();
		} catch (CloneNotSupportedException e) {
			// assert false;
		}
		o.events = (Priority<E>[])Array.newInstance(typePriorityE, o.size+RESERVE);
		for (int i = 0; i < size; ++i) {
			o.events[i] = events[i].clone();
		}
		return o;
	}

	public boolean contains(Object te) {
		for (int i = 0; i < size; ++i) {
			if (events[i].equals(te))
				return true;
		}
		return false;
	}

	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o))
				return false;
		}
		return true;
	}

	// no range checking
	public Priority<E> delete() {
		int i = 0;
		int child;
		// recycle memory aggressively
		Priority<E> down_event;
		down_event = events[--size];
		events[size] = events[i];
		// events[i] = down_event; // should be unnecessary
		for (; (child = (i << 1) + 1) < size; i = child) {
			if (child + 1 < size
					&& events[child + 1].compareTo(events[child]) < 0) {
				++child;
			}
			if (events[child].compareTo(down_event) < 0) {
				events[i] = events[child];
			} else {
				break;
			}
		}
		events[i] = down_event;
		return events[size];
	}

	public Priority<E> element() {
		if (size <= 0)
			throw new NoSuchElementException();
		return events[0];
	}

	// slow growth.
	protected void growArray() {
		events = Arrays.copyOf(events, size + GROW_BY);
		//System.out.println(".");
	}

	public boolean isEmpty() {
		if (size == 0)
			return true;
		return false;
	}

	public PriorityQueueIterator iterator() {
		return new PriorityQueueIterator();
	}

	// this queue is its own memory pool
	public Priority<E> newElement() {
		if (size >= events.length) {
			growArray();
		}
		Priority<E> t;
		if (events[size] == null) {
			t = new Priority<E>();
		} else {
			t = events[size];
		}
		return t;
	}

	public boolean offer(Priority<E> te) {
		if (size >= events.length)
			return false;
		int i = size++;
		int parent;
		for (; (parent = (i - 1) >> 1) >= 0 && te.compareTo(events[parent]) < 0; i = parent) {
			events[i] = events[parent];
		}
		events[i] = te;
		return true;
	}

	// events can't be null, but it could (theoretically) have length 0
	// but more importantly, events[0] might not be null if
	// size <= 0
	public Priority<E> peek() {
		if (size <= 0)
			return null;
		return events[0];
	}

	public Priority<E> poll() {
		if (size <= 0)
			return null;
		int i = 0;
		int child;
		// recycle memory aggressively
		Priority<E> down_event;
		down_event = events[--size];
		events[size] = events[i];
		// events[i] = down_event; // should be unnecessary
		for (; (child = (i << 1) + 1) < size; i = child) {
			if (child + 1 < size
					&& events[child + 1].compareTo(events[child]) < 0) {
				++child;
			}
			if (events[child].compareTo(down_event) < 0) {
				events[i] = events[child];
			} else {
				break;
			}
		}
		events[i] = down_event;
		return events[size];
	}

	// no growth, and no range checking.
	// will throw indexoutofbounds, nullpointer, etc.
	public boolean put(Priority<E> te) {
		int i = size++;
		int parent;
		for (; (parent = (i - 1) >> 1) >= 0 && te.compareTo(events[parent]) < 0; i = parent) {
			events[i] = events[parent];
		}
		events[i] = te;
		return true;
	}

	public Priority<E> remove() {
		if (size <= 0)
			throw new NoSuchElementException();
		int i = 0;
		int child;
		// recycle memory aggressively
		Priority<E> down_event;
		down_event = events[--size];
		events[size] = events[i];
		// events[i] = down_event; // should be unnecessary
		for (; (child = (i << 1) + 1) < size; i = child) {
			if (child + 1 < size
					&& events[child + 1].compareTo(events[child]) < 0) {
				++child;
			}
			if (events[child].compareTo(down_event) < 0) {
				events[i] = events[child];
			} else {
				break;
			}
		}
		events[i] = down_event;
		return events[size];
	}

	public boolean remove(Object o) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	public int size() {
		return size;
	}

	public Object[] toArray() {
		Priority<E>[] a = null;
		a = (Priority<E>[])java.lang.reflect.Array.newInstance(typePriorityE, size);
		for (int i = 0; i < size; ++i) {
			a[i] = events[i].clone();
		}
		return a;
	}

	/**
	 * Maintains Heap-order
	 * 
	 * @see java.util.Collection#toArray(T[])
	 */
	public <T> T[] toArray(T[] a) {
		if (a.length < size) {
			a = Arrays.copyOf(a, size);
		}
		if (a.length > size) {
			a[size] = null;
		}
		for (int i = 0; i < size; ++i) {
			a[i] = (T) events[i].clone();
		}
		return a;
	}

	@Override
	public String toString() {
		StringBuilder buf = new StringBuilder(size * 10);
		for (int i = 0; i < size; ++i) {
			buf.append(events[i]).append('\n');
		}
		return buf.toString();
	}

	class PriorityQueueIterator implements Iterator<Priority<E>> {
		int i = 0;

		public boolean hasNext() {
			return i < size;
		}

		public Priority<E> next() {
			try {
				return events[i++];
			} catch (Exception e) {
				throw new NoSuchElementException(e.getMessage());
			}
		}

		public void remove() {
			if (i == 0)
				throw new IllegalStateException();
			if (i == 1) {
				remove();
				i = 0;
			}
			throw new UnsupportedOperationException();
		}

	}
}