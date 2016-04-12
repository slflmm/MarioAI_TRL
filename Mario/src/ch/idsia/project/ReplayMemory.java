package ch.idsia.project;

import java.util.Random;

/**
 * 
 * @author stephanielaflamme
 *
 */
public class ReplayMemory {

	private int capacity;
	private int counter;
	private boolean full;
	
	private double[][] states;
	private double[] rewards;
	private int[] actions;
	private boolean[] terminals;
	private Random rng;
	
	public ReplayMemory(int N, int stateSize) {
		capacity = N;
		counter = 0;
		full = false;
		states = new double[capacity+1][stateSize]; // +1 because 'next state' is in there too
		rewards = new double[capacity];
		actions = new int[capacity];
		terminals = new boolean[capacity];
//		states[counter] = initial_state;
		rng = new Random();
	}
	
	/**
	 * 
	 * @param r
	 * @param a
	 * @param sp
	 */
	public void addTransition(double r, int a, double[] sp, boolean t) {
		states[counter+1] = sp;
		rewards[counter] = r;
		actions[counter] = a;
		terminals[counter] = t;
		counter++;
		if (counter >= capacity) {
			counter = 0;
			full = true;
		}
	}
	
	public void setFirstState(double[] s) {
		states[counter] = s;
	}
	
	public int getCount() {
		return counter;
	}
	
	/**
	 * Returns an array of indices for the minibatch
	 * @param size
	 * @return
	 */
	public int[] sampleMinibatchIdx(int size) {
		int[] idx = new int[size];
		for (int i = 0; i < size; i++) {
			int max = full ? capacity : counter;
			idx[i] = rng.nextInt(max); 
		}
		return idx;
	}
	
	/**
	 * Returns s for transition i
	 * @param i
	 * @return
	 */
	public double[] getS(int i) {
		return states[i];
	}
	
	public double[][] getStates(int[] idx) {
		double[][] sBatch = new double[idx.length][states[0].length];
		for (int i = 0; i < idx.length; i++) {
			sBatch[i] = states[i];
		}
		return sBatch;
	}
	
	/**
	 * Returns s' (next state) for transition i
	 * @param i
	 * @return
	 */
	public double[] getSPrime(int i) {
		return states[i+1];
	}
	
	public double[][] getStatesPrime(int[] idx) {
		double[][] sBatch = new double[idx.length][states[0].length];
		for (int i = 0; i < idx.length; i++) {
			sBatch[i] = states[i+1];
		}
		return sBatch;
	}
	
	/**
	 * Returns reward for transition i
	 * @param i
	 * @return
	 */
	public double getR(int i) {
		return rewards[i];
	}
	
	public double[] getRewards(int[] idx) {
		double[] rBatch = new double[idx.length];
		for (int i = 0; i < idx.length; i++) {
			rBatch[i] = rewards[i];
		}
		return rBatch;
	}
	
	/**
	 * Returns action for transition i
	 * @param i
	 * @return
	 */
	public int getA(int i) {
		return actions[i];
	}
	
	public int[] getActions(int[] idx) {
		int[] aBatch = new int[idx.length];
		for (int i = 0; i < idx.length; i++) {
			aBatch[i] = actions[i];
		}
		return aBatch;
	}
	
	/**
	 * Returns terminal boolean for transition i
	 * @param i
	 * @return
	 */
	public boolean getT(int i) {
		return terminals[i];
	}
	
	public boolean[] getTerminals(int[] idx) {
		boolean[] tBatch = new boolean[idx.length];
		for (int i = 0; i < idx.length; i++) {
			tBatch[i] = terminals[i];
		}
		return tBatch;
	}
	
}
