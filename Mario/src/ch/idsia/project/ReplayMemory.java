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
	private double[][] states;
	private double[] rewards;
	private int[] actions;
	private Random rng;
	
	public ReplayMemory(int N, int stateSize, double[] initial_state) {
		capacity = N;
		counter = 0;
		states = new double[capacity+1][stateSize]; // +1 because 'next state' is in there too
		rewards = new double[capacity];
		actions = new int[capacity];
		states[counter] = initial_state;
		rng = new Random();
	}
	
	/**
	 * 
	 * @param r
	 * @param a
	 * @param sp
	 */
	public void addTransition(double r, int a, double[] sp) {
		states[counter+1] = sp;
		rewards[counter] = r;
		actions[counter] = a;
		counter++;
		if (counter >= capacity) counter=0;
	}
	
	/**
	 * Returns an array of indices for the minibatch
	 * @param size
	 * @return
	 */
	public int[] sampleMinibatchIdx(int size) {
		int[] idx = new int[size];
		for (int i = 0; i < size; i++) {
			idx[i] = rng.nextInt(capacity); 
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
	
	/**
	 * Returns s' (next state) for transition i
	 * @param i
	 * @return
	 */
	public double[] getSPrime(int i) {
		return states[i+1];
	}
	
	/**
	 * Returns reward for transition i
	 * @param i
	 * @return
	 */
	public double getR(int i) {
		return rewards[i];
	}
	
	/**
	 * Returns action for transition i
	 * @param i
	 * @return
	 */
	public int getA(int i) {
		return actions[i];
	}
	
}
