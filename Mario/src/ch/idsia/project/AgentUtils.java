package ch.idsia.project;

import ch.idsia.agents.controllers.BasicMarioAIAgent;
import ch.idsia.benchmark.mario.environments.Environment;

/**
 * This class handles everything to do with calculating state information, 
 * since it was too big, cumbersome, and unrelated to main functionality to stay in DQNAgent
 * @author stephanielaflamme
 *
 */
public abstract class AgentUtils extends BasicMarioAIAgent {
	
	public AgentUtils(String s) {
		super(s);
		// TODO Auto-generated constructor stub
	}

	protected static final int NUM_EL = 10;
	protected static final int NUM_FRAMES_STUCK = 5;
	
	// information on past state to calculate current
	private int lastKillsByStomp;
	private int lastKillsByFire;
	private int lastKillsByShell;
	private int lastMode;
	private float[] lastFloatPos;
	private int notMovedCount;
	
	// the level of detail in observations
	protected int zLevelScene = 1; 
	protected int zLevelEnemies = 1; 
		
	
	/**
	 * Get the observation. Encoded with 39 bits.
	 * Mario mode: 0 - small, 1 - big, 2 - fire
	 * Direction of velocity: 8 directions + stay, 9 possible values
	 * Stuck: 0 or 1
	 * On ground: 0 or 1
	 * Can jump: 0 or 1
	 * Carrying: 0 or 1
	 * Able to shoot: 0 or 1
	 * Collided with creature: 0 or 1
	 * Nearby enemies: 0 or 1 in 8 directions in the 3x3 window (or 4x3 if large mario)
	 * Mid-range enemies: 0 or 1 in 8 directions in the 7x7 window (or 8x7 if large mario)
	 * Far enemies: 0 or 1 in 8 directions in the 11x11 (or 12x11 if large mario)
	 * Enemies killed by stomp: 0 or 1
	 * Enemies killed by fire: 0 or 1
	 * Enemies killed by shell: 0 or 1
	 * Obstacles: 4-bit boolean for obstacles in front of mario
	 * WHAT ABOUT CARRYING AND ABLE TO SHOOT?
	 */
	protected int[] getState(Environment environment) {
		int[] state = new int[15 + 3*8*NUM_EL];
		
		// get data on current environment
		int currentMode = environment.getMarioMode();
		int currentKillsByStomp = environment.getKillsByStomp();
		int currentKillsByFire = environment.getKillsByFire();
		int currentKillsByShell = environment.getKillsByShell();
		float[] currentFloatPos = environment.getMarioFloatPos();
		int[] egoPos = environment.getMarioEgoPos();
		byte[][] obs = environment.getMergedObservationZZ(zLevelScene, zLevelEnemies);
		
		// update information on how long you've been stuck
		notMovedCount = (Math.abs(currentFloatPos[0] - lastFloatPos[0]) < 0.0001 
				&& Math.abs(currentFloatPos[1] - lastFloatPos[1]) < 0.0001) ? notMovedCount+1 : 0;

		
		// mario mode; one bit for small vs not, and another for fire vs not
		state[0] = currentMode == 0 ? 1 : 0; 
		state[1] = currentMode == 2 ? 1 : 0; 
		
		// direction of velocity: up down left right can each be 1 or 0
		state[2] = lastFloatPos[0] - currentFloatPos[0] > 0 ? 1 : 0;
		state[3] = lastFloatPos[0] - currentFloatPos[0] < 0 ? 1 : 0;
		state[4] = lastFloatPos[1] - currentFloatPos[1] > 0 ? 1 : 0;
		state[5] = lastFloatPos[1] - currentFloatPos[1] < 0 ? 1 : 0;
		
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
		
		// update to set 'past' as 'current'
		lastMode = currentMode;
		lastKillsByStomp = currentKillsByStomp;
		lastKillsByFire = currentKillsByFire;
		lastKillsByShell = currentKillsByShell;
		lastFloatPos = currentFloatPos;
		
		return state;
	}

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
	private static void fillObs(int[][][] observations, int[] state, int n) {
		for (int i = 0; i < observations.length; i++) {
			for (int j = 0; j < observations[i].length; j++) {
				for (int k = 0; k < observations[i][j].length; k++) {
					state[n + i + observations[i].length*(j + observations[i][j].length*k)] = observations[i][j][k];
				}
			}
		}
	}
}
