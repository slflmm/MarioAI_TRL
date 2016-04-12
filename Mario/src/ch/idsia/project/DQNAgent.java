package ch.idsia.project;


import java.util.Random;

import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.evolution.MLP;

public class DQNAgent extends UtilAgent implements Agent {
	
	static private final String name = "DQNAgent";

	private MLP mlp;
	private double gamma;
	private double epsilon = 0.9999;
	private Random rng;

	public DQNAgent(MLP mlp, double gamma) {
		super(name);
		this.mlp = mlp;
		this.gamma = gamma;
		rng = new Random();
	}
	
	/**
	 * Given a minibatch of transitions, performs gradient descent
	 * @param states
	 * @param rewards
	 * @param actions
	 */
	public void train(double[][] states, double[] rewards, int[] actions, double[][] nextStates, boolean[] terminal, int batchSize) {

		// calculate target values for the actions
		double[][] y = new double[batchSize][12];
		for (int j = 0; j < batchSize; j++) {
			double[] Qsa = mlp.propagate(states[j]); // get the current Q(s,a) values for that state
			for (int i = 0; i < 12; i++) {
				y[j][i] = Qsa[i]; // if it's not the action we did, leave it the same
			}
			// modify the Q-value for the action we did 
			if (terminal[j] == true) y[j][actions[j]] = rewards[j]; 
			else {
				y[j][actions[j]] = rewards[j] + gamma*max(mlp.propagate(nextStates[j]));
			}
		}
		// next perform back propagation on target - current Q 
		for (int j = 0; j < batchSize; j++) {
			mlp.propagate(states[j]); // this is a pain...
			mlp.backPropagate(y[j]);
		}
	}

	/**
	 * Returns action based on current policy.
	 * eps --> true means we are using epsilon-greedy
	 */
	public int getAction(double[] state, boolean eps) {
		int a;
		if (eps && Math.random() < epsilon) {
			a = rng.nextInt(12);
			epsilon*=0.9999;
//			System.out.println(epsilon);
		}
		else a = argmax(mlp.propagate(state));
		return a;
	}


	/**
	 * Builds state vector from the environment.
	 */
	public double[] getState(Environment environment) {
		return super.getState(environment);
	}
	

	/**
	 * Calculate reward based on advancement.
	 */
	public double getReward(Environment environment) {
		return super.getReward(environment);
	}


	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
	

	

	

	


}
