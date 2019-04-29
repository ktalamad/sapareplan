/****************************************************
    Utility.java: Contains supporting functions to
    help doing dirty jobs for other classes

    Author: Minh B. Do - Arizona State University
 ****************************************************/
package edu.asu.sapa.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

import edu.asu.sapa.ground.FluentDB;
import edu.asu.sapa.ground.Operator;
import edu.asu.sapa.ground.update.Set;

public class Utility {

	ArrayList<Operator> groundActions;

	float[] maxValue;
	float[] maxResCost;
	float[] maxDuration;
	float[] resLevel;
	float[] deficit;

	int numFunc;

	/** Function to set the ground actions */
	public void initialize(ArrayList<Operator> gActions, FluentDB mres, int count) {
		groundActions = gActions;
		this.numFunc = count;
		maxValue = new float[numFunc];
		maxResCost = new float[numFunc];
		maxDuration = new float[numFunc];
		resLevel = new float[numFunc];
		deficit = new float[numFunc];
		if (numFunc > 0)
			maxResPreprocess(mres);
	}

	private void maxResPreprocess(FluentDB mresDB) {
		Operator action;
		Set ms;

		int fID;
		int oper;
		float value;
		float dur, cost;

		Arrays.fill(maxValue, Float.NEGATIVE_INFINITY);
		Arrays.fill(maxResCost, Float.NEGATIVE_INFINITY);
		Arrays.fill(maxDuration, Float.NEGATIVE_INFINITY);

		// Go through all ground actions that can increase that func values.
		for (int i = 1; i < groundActions.size(); i++) {
			action = groundActions.get(i);

			if (action.setList.length < 1 || !action.isPossible)
				continue;

			dur = action.getDuration(mresDB);
			cost = action.getCost(mresDB, dur);

			for (int j = 0; j < action.setList.length; j++) {
				ms = action.setList[j];

				oper = ms.op;
				fID = ms.getLeftSide();
				value = ms.getRightSide().value(mresDB, dur);

				if ((oper == 0 || oper == 1) && (value > maxValue[fID])) {
					maxValue[fID] = value;
					maxDuration[fID] = dur;
					maxResCost[fID] = cost;
				} else if ((oper == 2) && (value < maxValue[fID])) {
					maxValue[fID] = -value;
					maxDuration[fID] = dur;
					maxResCost[fID] = cost;
				}
			}
		}
	}

	/*
	 * Function to adjust the relaxed plan according to the resource usage of
	 * actions int the domains and the amount of resource available at the
	 * inititial state. Note: propOption = True then costs of the reajustment
	 * will be propositional to the (non-integer) costs of the adjustment
	 * actions need to be added to the relaxed plan. False: first count the
	 * *integer* number of actions need to be added.
	 */
	public float resourceAdjustment(int[] solution, int sSize, FluentDB mresDB,
			boolean propOption) {
		float dur, adjustment = 0;

		Set ms;
		int oper, resID;
		float value;
		Operator aAction;

		for (int i = 0; i < numFunc; i++) {
			resLevel[i] = mresDB.get(i);
		}

		Arrays.fill(deficit, 0f);

		/* Go through the actions in the solution */
		for (int i = 0; i < sSize; i++) {
			aAction = groundActions.get(solution[i]);
			if (aAction.setList.length < 1)
				continue;

			for (int j = 0; j < aAction.setList.length; j++) {
				ms = aAction.setList[j];

				dur = aAction.getDuration(mresDB);

				oper = ms.op;
				resID = ms.getLeftSide();
				value = ms.getRightSide().value(mresDB, dur);

				switch (oper) {
				case 0:
					if (resLevel[resID] < 0)
						deficit[resID] += -resLevel[resID];
					resLevel[resID] = value;
					break;
				case 1:
					resLevel[resID] += value;
					break;
				case 2:
					resLevel[resID] -= value;
					break;
				case 3:
					resLevel[resID] *= value;
					break;
				case 4:
					resLevel[resID] /= value;
					break;
				}
			}
		}

		for (int i = 0; i < numFunc; i++)
			if (resLevel[i] < 0)
				deficit[i] += -resLevel[i];
			else if (deficit[i] > 0) {
				if (propOption) {
					adjustment += (deficit[i] / maxValue[i]) * maxResCost[i];
				} else {
					adjustment += Math.floor(deficit[i] / maxValue[i])
							* maxResCost[i];
				}
			}

		return adjustment;
	}
	
	public static void insert(boolean[] a, int index, boolean v) {
		for (int i = a.length - 1; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(byte[] a, int index, byte v) {
		for (int i = a.length - 1; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(char[] a, int index, char v) {
		for (int i = a.length - 1; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(double[] a, int index, double v) {
		for (int i = a.length - 1; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(float[] a, int index, float v) {
		for (int i = a.length - 1; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(int[] a, int index, int v) {
		for (int i = a.length - 1; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(long[] a, int index, long v) {
		for (int i = a.length - 1; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(Object[] a, int index, Object v) {
		for (int i = a.length - 1; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(short[] a, int index, short v) {
		for (int i = a.length - 1; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(boolean[] a, int index, boolean v, int num) {
		for (int i = num; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(byte[] a, int index, byte v, int num) {
		for (int i = num; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(char[] a, int index, char v, int num) {
		for (int i = num; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(double[] a, int index, double v, int num) {
		for (int i = num; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(float[] a, int index, float v, int num) {
		for (int i = num; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(int[] a, int index, int v, int num) {
		for (int i = num; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(long[] a, int index, long v, int num) {
		for (int i = num; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(Object[] a, int index, Object v, int num) {
		for (int i = num; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}

	public static void insert(short[] a, int index, short v, int num) {
		for (int i = num; i > index; --i) {
			a[i] = a[i - 1];
		}
		a[index] = v;
	}
	
	public static void remove(boolean[] a, int index) {
		for (; index < a.length - 1; index++) {
			a[index] = a[index + 1];
		}
	}

	public static void remove(boolean[] a, int index, int length) {
		for (; index < a.length - length; index++) {
			a[index] = a[index + length];
		}
	}

	public static void remove(byte[] a, int index) {
		for (; index < a.length - 1; index++) {
			a[index] = a[index + 1];
		}
	}

	public static void remove(byte[] a, int index, int length) {
		for (; index < a.length - length; index++) {
			a[index] = a[index + length];
		}
	}

	public static void remove(char[] a, int index) {
		for (; index < a.length - 1; index++) {
			a[index] = a[index + 1];
		}
	}

	public static void remove(char[] a, int index, int length) {
		for (; index < a.length - length; index++) {
			a[index] = a[index + length];
		}
	}

	public static void remove(double[] a, int index) {
		for (; index < a.length - 1; index++) {
			a[index] = a[index + 1];
		}
	}

	public static void remove(double[] a, int index, int length) {
		for (; index < a.length - length; index++) {
			a[index] = a[index + length];
		}
	}

	public static void remove(float[] a, int index) {
		for (; index < a.length - 1; index++) {
			a[index] = a[index + 1];
		}
	}

	// Filling

	public static void remove(float[] a, int index, int length) {
		for (; index < a.length - length; index++) {
			a[index] = a[index + length];
		}
	}

	public static void remove(int[] a, int index) {
		for (; index < a.length - 1; index++) {
			a[index] = a[index + 1];
		}
	}

	public static void remove(int[] a, int index, int length) {
		for (; index < a.length - length; index++) {
			a[index] = a[index + length];
		}
	}

	public static void remove(long[] a, int index) {
		for (; index < a.length - 1; index++) {
			a[index] = a[index + 1];
		}
	}

	public static void remove(long[] a, int index, int length) {
		for (; index < a.length - length; index++) {
			a[index] = a[index + length];
		}
	}

	public static void remove(Object[] a, int index) {
		for (; index < a.length - 1; index++) {
			a[index] = a[index + 1];
		}
	}

	public static void remove(Object[] a, int index, int length) {
		for (; index < a.length - length; index++) {
			a[index] = a[index + length];
		}
	}

	public static void remove(short[] a, int index) {
		for (; index < a.length - 1; index++) {
			a[index] = a[index + 1];
		}
	}

	public static void remove(short[] a, int index, int length) {
		for (; index < a.length - length; index++) {
			a[index] = a[index + length];
		}
	}
	
	public static boolean[] grow(boolean[] a) {
		boolean[] t = new boolean[a.length + (a.length >> 1) + 1];
		System.arraycopy(a, 0, t, 0, a.length);

		return t;
	}

	public static float[] grow(float[] a) {
		float[] t = new float[a.length + (a.length >> 1) + 1];
		System.arraycopy(a, 0, t, 0, a.length);

		return t;
	}

	public static int[] grow(int[] a) {
		int[] t = new int[a.length + (a.length >> 1) + 1];
		System.arraycopy(a, 0, t, 0, a.length);

		return t;
	}

	public static <T> T[] grow(T[] a) {
		T[] t = (T[])Array.newInstance((Class<T>) a.getClass().getComponentType(), a.length
				+ (a.length >> 1) + 1);
		System.arraycopy(a, 0, t, 0, a.length);

		return t;
	}

	public static int indexOf(boolean[] a, boolean e) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(byte[] a, byte e) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	// Searching

	public static int indexOf(char[] a, char e) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(double[] a, double e) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(float[] a, float e) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(int[] a, int e) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(long[] a, long e) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(Object[] a, Object e) {
		if (e == null) {
			for (int i = 0; i < a.length; i++) {
				if (a[i] == null) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < a.length; i++) {
				if (a[i] != null && a[i].equals(e)) {
					return i;
				}
			}
		}
		return -1;
	}

	public static int indexOf(short[] a, short e) {
		for (int i = 0; i < a.length; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(boolean[] a, boolean e, int size) {
		for (int i = 0; i < size; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(byte[] a, byte e, int size) {
		for (int i = 0; i < size; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	// Searching

	public static int indexOf(char[] a, char e, int size) {
		for (int i = 0; i < size; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(double[] a, double e, int size) {
		for (int i = 0; i < size; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(float[] a, float e, int size) {
		for (int i = 0; i < size; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(int[] a, int e, int size) {
		for (int i = 0; i < size; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(long[] a, long e, int size) {
		for (int i = 0; i < size; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}

	public static int indexOf(Object[] a, Object e, int size) {
		if (e == null) {
			for (int i = 0; i < size; i++) {
				if (a[i] == null) {
					return i;
				}
			}
		} else {
			for (int i = 0; i < size; i++) {
				if (a[i] != null && a[i].equals(e)) {
					return i;
				}
			}
		}
		return -1;
	}

	public static int indexOf(short[] a, short e, int size) {
		for (int i = 0; i < size; i++) {
			if (a[i] == e) {
				return i;
			}
		}
		return -1;
	}


}
