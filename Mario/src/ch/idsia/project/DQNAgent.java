package ch.idsia.project;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import ch.idsia.agents.Agent;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.evolution.MLP;

public class DQNAgent extends AgentUtils implements Agent {
	
	private static Map<Integer, int[]> twelveToSixMap;
	
	
	private MLP mlp;
	static private final String name = "DQNAgent";
	

	public DQNAgent(MLP mlp) {
		// TODO Whatever you end up needing here
		super(name);
		this.mlp = mlp;
		initializeActionMap();
	}

	/**
	 * Returns action based on current policy, as an array of 6 booleans on the Mario keys.
	 */
	@Override
	public boolean[] getAction() {
		// TODO integrate with neural network and save to replay memory
		
		int[] output = new int[12]; // assume this is the output of the MLP
		return twelveToSixActions(output);
	}


	@Override
	public void integrateObservation(Environment environment) {
		// TODO Store the state array in replay memory
		int[] state = getState(environment);
		
	}
	


	@Override
	public void giveIntermediateReward(float intermediateReward) {
		// TODO Store sequence of rewards and states separately to avoid duplication in states if storing transitions
		
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}
	
	
	/**
	 * Maps the neural net's 12 one-hot actions to the 6 boolean keys.
	 * In order, the keys represent 0=LEFT, 1=RIGHT, 2=DOWN, 3=JUMP, 4=SPEED, and 5=UP (see Mario.java)
	 * The agent only considers 12 actions from the space of 64: 
	 * {LEFT, RIGHT, STAY} x {JUMP, NOTJUMP} x {SPEED(FIRE), NOSPEED}.
	 * @param twelve
	 * @return
	 */
	private static boolean[] twelveToSixActions(int[] twelve) {
		boolean[] six = new boolean[6];
		Integer action = Arrays.asList(twelve).indexOf(1);
		int[] idx = twelveToSixMap.get(action);
		for (int i : idx) {
			six[i] = true;
		}
		return six;
	}
	
	/**
	 * Mapping from one-hot encoding of 12 states to true indices in Mario 
	 * 0= stay, no jump, no speed (none active)
	 * 1= left, no jump, no speed (0)
	 * 2= right, no jump, no speed (1)
	 * 3= stay, jump, no speed (3)
	 * 4= left, jump, no speed (0,3)
	 * 5= right, jump, no speed (1,3)
	 * 6= stay, no jump, speed (4)
	 * 7= left, no jump, speed (0,4)
	 * 8= right, no jump, speed (1,4)
	 * 9= stay, jump, speed (3,4)
	 * 10= left, jump, speed (0,3,4)
	 * 11= right, jump, speed (1,3,4)
	 */
	private static void initializeActionMap() {
		twelveToSixMap = new HashMap<Integer, int[]>(12);
		twelveToSixMap.put(0, new int[]{});
		twelveToSixMap.put(1, new int[]{0});
		twelveToSixMap.put(2, new int[]{1});
		twelveToSixMap.put(3, new int[]{3});
		twelveToSixMap.put(4, new int[]{0,3});
		twelveToSixMap.put(5, new int[]{1,3});
		twelveToSixMap.put(6, new int[]{4});
		twelveToSixMap.put(7, new int[]{0,4});
		twelveToSixMap.put(8, new int[]{1,4});
		twelveToSixMap.put(9, new int[]{3,4});
		twelveToSixMap.put(10, new int[]{0,3,4});
		twelveToSixMap.put(11, new int[]{1,3,4});
	}
	

	


}
