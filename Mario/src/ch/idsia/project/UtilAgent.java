package ch.idsia.project;

import java.util.HashMap;
import java.util.Map;

import ch.idsia.agents.controllers.BasicMarioAIAgent;
import ch.idsia.benchmark.mario.environments.Environment;

/**
 * This class handles everything to do with calculating state, reward & action from the MarioAI interface, 
 * since it was too big, cumbersome, and unrelated to main functionality to stay in DQNAgent
 * @author stephanielaflamme
 *
 */
public abstract class UtilAgent extends BasicMarioAIAgent {


	protected static final int NUM_EL = 10;
	protected static final int NUM_FRAMES_STUCK = 5;


	// information on past state to calculate current
	protected int lastMode;
	protected int lastKillsByStomp;
	protected int lastKillsByFire;
	protected int lastKillsByShell;
	protected int lastKillsTotal;
	protected float lastFloatPosX;
	protected float lastFloatPosY;
	protected float lastGroundedPosX;
	protected float lastGroundedPosY;

	// temporary variables for 'current'
	protected int currentMode;
	protected int currentKillsByStomp;
	protected int currentKillsByFire;
	protected int currentKillsByShell;
	protected int currentKillsTotal;
	protected float currentFloatPosX;
	protected float currentFloatPosY;
	protected float currentGroundedPosX;
	protected float currentGroundedPosY;

	protected int notMovedCount;

	// the level of detail in observations
	protected int zLevelScene = 1; 
	protected int zLevelEnemies = 1; 

	private Map<Integer, int[]> twelveToSixMap;


	public UtilAgent(String s) {
		super(s);
		initializeActionMap();
	}


	/**
	 * Update information about previous state.
	 * Must only be called after transition has been stored!
	 */
	protected void setPrevious(Environment environment) {
		lastFloatPosX = currentFloatPosX;
		lastFloatPosY = currentFloatPosY;
		lastKillsTotal = currentKillsTotal;
		lastMode = currentMode;
		lastKillsByStomp = currentKillsByStomp;
		lastKillsByFire = currentKillsByFire;
		lastKillsByShell = currentKillsByShell;
		if (environment.isMarioOnGround()) {
			lastGroundedPosX = environment.getMarioFloatPos()[0];
			lastGroundedPosY = environment.getMarioFloatPos()[1];
		}

	}

	protected void setCurrents(Environment environment) {
		// get data on current environment
		currentMode = environment.getMarioMode();
		currentKillsByStomp = environment.getKillsByStomp();
		currentKillsByFire = environment.getKillsByFire();
		currentKillsByShell = environment.getKillsByShell();
		currentKillsTotal = environment.getKillsTotal();
		//		System.arraycopy(environment.getMarioFloatPos(), 0, currentFloatPos, 0, 2);
		currentFloatPosX = environment.getMarioFloatPos()[0];
		currentFloatPosY = environment.getMarioFloatPos()[1];
	}


	/**
	 * Calculates reward. 
	 * Moving forward, landing on a higher platform, and killing enemies is positive.
	 * Moving backward, being hit by enemies, or being stuck is negative.
	 */
	protected double getReward(Environment environment) {

		double advancement = (lastFloatPosX < currentFloatPosX) ? 0.5 : -0.5;
		double elevation = environment.isMarioOnGround() && currentFloatPosY < lastGroundedPosY ? 0.5 : 0;
		double kills = currentKillsTotal > lastKillsTotal ? 0.5 : 0;
		double collided = lastMode > currentMode || environment.getMarioStatus() == 0 ? -0.5 : 0;
		double stuck = notMovedCount > NUM_FRAMES_STUCK ? -0.5 : 0; 

		double r = advancement + elevation + kills + collided + stuck;
		return Math.max(-1, Math.min(1, r));
	}


	/**
	 * Get the observation. Encoded with 255 bits.
	 * Mario mode: 0 - small, 1 - big, 2 - fire
	 * Direction of velocity: 8 directions + stay, 9 possible values
	 * Stuck: 0 or 1
	 * On ground: 0 or 1
	 * Can jump: 0 or 1
	 * Carrying: 0 or 1
	 * Able to shoot: 0 or 1
	 * Collided with creature: 0 or 1
	 * Enemies killed by stomp: 0 or 1
	 * Enemies killed by fire: 0 or 1
	 * Enemies killed by shell: 0 or 1
	 * Nearby enemies + level elements: 0 or 1 in 8 directions in the 3x3 window (or 4x3 if large mario)
	 * Mid-range enemies + level elements: 0 or 1 in 8 directions in the 7x7 window (or 8x7 if large mario)
	 * Far enemies + level elements: 0 or 1 in 8 directions in the 11x11 (or 12x11 if large mario)
	 */
	protected double[] getState(Environment environment) {
		double[] state = new double[15 + 3*8*NUM_EL];

		int[] egoPos = environment.getMarioEgoPos();
		byte[][] obs = environment.getMergedObservationZZ(zLevelScene, zLevelEnemies);

		//		if (currentFloatPos==null) currentFloatPos = environment.getMarioFloatPos();
		//		if (lastFloatPos==null) lastFloatPos = environment.getMarioFloatPos();

		// update information on how long you've been stuck
		notMovedCount = (Math.abs(currentFloatPosX - lastFloatPosX) < 0.0001 
				&& Math.abs(currentFloatPosY - lastFloatPosY) < 0.0001) ? notMovedCount+1 : 0;


		// mario mode; one bit for small vs not, and another for fire vs not
		state[0] = currentMode == 0 ? 1 : 0; 
		state[1] = currentMode == 2 ? 1 : 0; 

		// direction of velocity: up down left right can each be 1 or 0
		state[2] = lastFloatPosX - currentFloatPosX > 0 ? 1 : 0;
		state[3] = lastFloatPosX - currentFloatPosX < 0 ? 1 : 0;
		state[4] = lastFloatPosY - currentFloatPosY > 0 ? 1 : 0;
		state[5] = lastFloatPosY - currentFloatPosY < 0 ? 1 : 0;

		// whether the agent is stuck (no movement over several frames)
		state[6] = notMovedCount > NUM_FRAMES_STUCK ? 1 : 0; 

		// some info on what Mario can and cannot do
		state[7] = environment.isMarioOnGround() ? 1 : 0;
		state[8] = environment.isMarioAbleToJump() ? 1 : 0;
		state[9] = environment.isMarioCarrying() ? 1 : 0;
		state[10] = environment.isMarioAbleToShoot() ? 1 : 0;

		// whether you've collided with an enemy in the current frame (mode decreased, or you've died)
		state[11] = lastMode > currentMode || environment.getMarioStatus() == 0 ? 1 : 0;

		// information on whether the agent made any kills in the current frame, and what kind
		state[12] = currentKillsByStomp - lastKillsByStomp >= 1 ? 1 : 0;
		state[13] = currentKillsByFire - lastKillsByFire >= 1 ? 1 : 0;
		state[14] = currentKillsByShell - lastKillsByShell >= 1 ? 1 : 0;

		// enemy + scene location information -- (8*3 = 24) bits for each type
		int[][][] observations = getDirections(obs, egoPos, currentMode == 0);
		fillObs(observations, state, 15);

		// and now state is complete
		return state;
	}

	/**
	 * Returns the observations of elements in 8 directions at 3 ranges
	 * @param obs
	 * @param egoPos
	 * @param isShort
	 * @return
	 */
	protected int[][][] getDirections(byte[][] obs, int[] egoPos, boolean isShort) {
		// numElements is the number of observations we care about
		// 3 for the 3 ranges (flatten it later)
		// 8 for the 8 directions
		int[][][] directions = new int[NUM_EL][3][8]; 

		int x0 = egoPos[0];
		int y0 = egoPos[1];

		int yOff = isShort ? 0 : 1;

		// Start with the short range

		// up and down are directions 0 and 1
		directions[getEl(obs[y0+1][x0])][0][0] = 1; // down
		directions[getEl(obs[y0-yOff-1][x0])][0][1] = 1; // up

		// left and right are directions 2 and 3
		directions[getEl(obs[y0][x0-1])][0][2] = 1;
		directions[getEl(obs[y0][x0+1])][0][3] = 1;
		if (!isShort) {
			// then we need to look at y0-1 also
			directions[getEl(obs[y0-1][x0-1])][0][2] = 1; // i.e. look up a bit
			directions[getEl(obs[y0-1][x0+1])][0][3] = 1;
		}

		// diagonals are directions 4, 5, 6, 7
		directions[getEl(obs[y0+1][x0-1])][0][4] = 1;
		directions[getEl(obs[y0+1][x0+1])][0][5] = 1;
		directions[getEl(obs[y0-yOff-1][x0-1])][0][6] = 1;
		directions[getEl(obs[y0-yOff-1][x0+1])][0][7] = 1;

		// Next we do the mid range

		// up and down form a 2x3 rectangle, and left and right a 3x2 (or 4x2)
		for (int j = 0; j <= 1; j++) {
			// up and down
			for (int i = -1; i <= 1; i++) {
				directions[getEl(obs[y0-yOff-2-j][x0+i])][1][0] = 1;
				directions[getEl(obs[y0+2+j][x0+i])][1][1] = 1;
			}
			// left and right
			for (int i = -1; i <= 1 + yOff; i++) {
				directions[getEl(obs[y0-i][x0-2-j])][1][2] = 1;
				directions[getEl(obs[y0-i][x0+2+j])][1][3] = 1;
			}
		}
		// now the diagonals form 2x2 squares
		for (int i = 0; i <= 1; i++) {
			for (int j = 0; j <= 1; j++) {
				directions[getEl(obs[y0+2+j][x0-2-i])][1][4] = 1;
				directions[getEl(obs[y0+2+j][x0+2+i])][1][5] = 1;
				directions[getEl(obs[y0-2-yOff-j][x0-2-i])][1][6] = 1;
				directions[getEl(obs[y0-2-yOff-j][x0+2+i])][1][7] = 1;
			}
		}

		// Finally, the long range

		// up and down form a 2x5 rectangle, and left and right 5x2 (or 6x2)
		for (int j = 0; j <= 1; j++) {
			// up and down
			for (int i = -2; i <= 2; i++) {
				directions[getEl(obs[y0-yOff-4-j][x0+i])][2][0] = 1;
				directions[getEl(obs[y0+4+j][x0+i])][2][1] = 1;
			}
			// left and right
			for (int i = -2; i <= 2 + yOff; i++) {
				directions[getEl(obs[y0-i][x0-4-j])][2][2] = 1;
				directions[getEl(obs[y0-i][x0+4+j])][2][3] = 1;
			}
		}
		// and finally the diagonals form little L-shapes; 2x2 + 1x2 borders
		for (int i = 0; i <= 1; i++) {
			for (int j = 0; j <= 1; j++) {
				// take care of the 2x2 squares
				directions[getEl(obs[y0+4+j][x0-4-i])][2][4] = 1;
				directions[getEl(obs[y0+4+j][x0+4+i])][2][5] = 1;
				directions[getEl(obs[y0-4-yOff-j][x0-4-i])][2][6] = 1;
				directions[getEl(obs[y0-4-yOff-j][x0+4+i])][2][7] = 1;
			}
			// handle the 2x1 borders
			directions[getEl(obs[y0+3][x0-4-i])][2][4] = 1;
			directions[getEl(obs[y0+4+i][x0-3])][2][4] = 1;
			directions[getEl(obs[y0+3][x0+4+i])][2][5] = 1;
			directions[getEl(obs[y0+4+i][x0+3])][2][5] = 1;
			directions[getEl(obs[y0-3-yOff][x0-4-i])][2][6] = 1;
			directions[getEl(obs[y0-4-yOff-i][x0-3])][2][6] = 1;
			directions[getEl(obs[y0-3-yOff][x0+4+i])][2][7] = 1;
			directions[getEl(obs[y0-4-yOff-i][x0+3])][2][7] = 1;
		}

		return directions;

	}

	/**
	 * Maps an element from GeneralizerLevelScene and GeneralizerEnemyScene to an int from 0 to 10
	 * We consider "can't pass through", border hill, brick, flower pot/cannon, 
	 * fire flower, goomba, spiky, mushroom, and fireball
	 */
	private static int getEl(int el) {
		switch(el) {
		case -60: return 1; // can't pass through
		case -62: return 2; // border hill
		case -24: return 3; // brick
		case -85: return 4; // flower pot / cannon
		case 3: return 5; // fire flower
		case 80: return 6; // goomba
		case 93: return 7; // spiky
		case 2: return 8; // mushroom
		case 25: return 9; // fireball
		default: return 0; // anything else (including empty space)
		}
	}

	/**
	 * Fill the 'state' array with values from 'observations' starting at idx 'n'
	 * @param observations
	 * @param state
	 * @param k
	 */
	private static void fillObs(int[][][] observations, double[] state, int n) {
		for (int i = 0; i < observations.length; i++) {
			for (int j = 0; j < observations[i].length; j++) {
				for (int k = 0; k < observations[i][j].length; k++) {
					state[n + i + observations[i].length*(j + observations[i][j].length*k)] = observations[i][j][k];
				}
			}
		}
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
	private void initializeActionMap() {
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


	/**
	 * Maps the neural net's 12 one-hot actions to the 6 boolean keys.
	 * In order, the keys represent 0=LEFT, 1=RIGHT, 2=DOWN, 3=JUMP, 4=SPEED, and 5=UP (see Mario.java)
	 * The agent only considers 12 actions from the space of 64: 
	 * {LEFT, RIGHT, STAY} x {JUMP, NOTJUMP} x {SPEED(FIRE), NOSPEED}.
	 * @param twelve
	 * @return
	 */
	public boolean[] twelveToSixActions(int[] twelve) {
		boolean[] six = new boolean[6];
		int action = 0;
		for (int i = 0; i < twelve.length; i++) {
			if (twelve[i] > 0) {
				action = i;
				break;
			}
		}
		int[] idx = twelveToSixMap.get(action);
		for (int i : idx) {
			six[i] = true;
		}
		return six;
	}

	protected int argmax(double[] array) {
		int idx = 0;
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < array.length; i++) {
			if (array[i] > max) {
				idx = i;
				max = array[i];
			}
		}
		return idx;
	}

	protected double max(double[] array) {
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < array.length; i++) {
			if (array[i] > max) {
				max = array[i];
			}
		}
		return max;
	}
}
