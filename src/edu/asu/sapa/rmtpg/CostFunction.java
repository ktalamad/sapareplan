package edu.asu.sapa.rmtpg;

import java.util.Arrays;

import edu.asu.sapa.utils.Utility;

public class CostFunction<T> {
	private static final int DEFAULT = 30;

	public T o;

	int size;
	float[] times;
	float[] costs;
	int[] supportIDs;
	float[] supportTimes;

	public CostFunction() {
		times = new float[DEFAULT];
		costs = new float[DEFAULT];
		supportIDs = new int[DEFAULT];
		supportTimes = new float[DEFAULT];
		size = 0;
	}

	public CostFunction(T o) {
		this.o = o;

		times = new float[DEFAULT];
		costs = new float[DEFAULT];
		supportIDs = new int[DEFAULT];
		supportTimes = new float[DEFAULT];
		size = 0;
	}

	public boolean achievable(float time) {
		if (size < 1) {
			return false;
		}

		if (times[0] > time) {
			return false;
		}

		return true;
	}

	/* Manage the costs & supporting action */
	public boolean addCost(float time, float c, int aID, float aTime) {
		if (size >= costs.length) {
			int capacity = size + (size >> 1) + 1;
			costs = Arrays.copyOf(costs, capacity);
			times = Arrays.copyOf(times, capacity);
			supportIDs = Arrays.copyOf(supportIDs, capacity);
			supportTimes = Arrays.copyOf(supportTimes, capacity);
		}

		int i;
		for (i = size - 1; i >= 0; --i) {
			if (times[i] < time) {
				if (c >= costs[i]) {
					return false;
				}
				++i;
				if (i == size) {
					times[size] = time;
					costs[size] = c;
					supportIDs[size] = aID;
					supportTimes[size] = aTime;
					++size;
					return true;
				}
				if (c == costs[i]) {
					if (time == times[i]) {
						return false;
					}
					times[i] = time;
					supportIDs[i] = aID;
					supportTimes[size] = aTime;
				} else if (c > costs[i]) {
					if (time == times[i]) {
						return false;
					}
					Utility.insert(costs, i, c);
					Utility.insert(times, i, time);
					Utility.insert(supportIDs, i, aID);
					Utility.insert(supportTimes, i, aTime);
					++size;
				} else if (c < costs[i]) {
					times[i] = time;
					costs[i] = c;
					supportIDs[i] = aID;
					supportTimes[size] = aTime;
					++i;
					int j;
					for (j = i; j < size; ++j) {
						if (c >= costs[j]) {
							break;
						}
					}
					if (j > i) {
						Utility.remove(costs, i, j - i);
						Utility.remove(times, i, j - i);
						Utility.remove(supportIDs, i, j - i);
						Utility.remove(supportTimes, i, j - i);
						size -= j - i;
					}
				}
				return true;
			}
		}
		++i;
		if (i == size) {
			times[size] = time;
			costs[size] = c;
			supportIDs[size] = aID;
			supportTimes[size] = aTime;
			++size;
			return true;
		}
		if (c == costs[i]) {
			if (time == times[i]) {
				return false;
			}
			times[i] = time;
			supportIDs[i] = aID;
			supportTimes[size] = aTime;
		} else if (c > costs[i]) {
			if (time == times[i]) {
				return false;
			}
			Utility.insert(costs, i, c);
			Utility.insert(times, i, time);
			Utility.insert(supportIDs, i, aID);
			Utility.insert(supportTimes, i, aTime);
			++size;
		} else if (c < costs[i]) {
			times[i] = time;
			costs[i] = c;
			supportIDs[i] = aID;
			supportTimes[size] = aTime;
			++i;
			int j;
			for (j = i; j < size; ++j) {
				if (c >= costs[j]) {
					break;
				}
			}
			if (j > i) {
				Utility.remove(costs, i, j - i);
				Utility.remove(times, i, j - i);
				Utility.remove(supportIDs, i, j - i);
				Utility.remove(supportTimes, i, j - i);
				size -= j - i;
			}
		}
		return true;
	}

	public void deactivate() {
		size = 0;
	}

	public float getCost() {
		return size > 0 ? costs[size - 1] : Float.POSITIVE_INFINITY;
	}

	public float getCost(float time) {
		for (int i = size - 1; i >= 0; --i) {
			if (times[i] <= time) {
				return costs[i];
			}
		}

		return Float.POSITIVE_INFINITY;
	}

	/**
	 * \argmin_t cost(t) == cost(tmax)
	 * 
	 * (\forall t getCost(getMinTime(t)) == getCost(t))
	 */
	public float getMinTime(float tmax) {
		for (int i = size - 1; i >= 0; --i) {
			if (times[i] <= tmax)
				return times[i];
		}
		
		System.err.println("\nError from RP Extraction.getMinTime: " +
				"Support time " + tmax + " earlier than reachable time " + times[0]);
		return times[0];
		//return Float.NEGATIVE_INFINITY;
	}

	public int getLastSupportID() {
		if (size <= 0) {
			return -1;
		}
		return supportIDs[size - 1];
	}
	
	public int getLastSupportTime() {
		if (size <= 0) {
			return -1;
		}
		return supportIDs[size - 1];
	}
	
	public int getSupportID(int index) {
		return supportIDs[index];
	}
	
	public int getSupportTime(int index) {
		if (size <= 0) {
			return -1;
		}
		return supportIDs[index];
	}

	public int getSupportIndex(float time) {
		
		if (size <= 0) {
			return -1;
		}
		
		for (int i = size - 1; i >= 0; --i) {
			if (times[i] <= time) {
				return i;
			}
		}
		
		System.err.println("\nError from RP Extraction.getSupport: " +
				"Support time " + time + " earlier than reachable time " + times[0]);

		return 0;
		//return Integer.MIN_VALUE;
	}

	public boolean isIn() {
		return size > 0;
	}

}
