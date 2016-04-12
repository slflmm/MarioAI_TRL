package ch.idsia.project;

//import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.benchmark.tasks.MarioCustomSystemOfValues;
import ch.idsia.evolution.MLP;
import ch.idsia.tools.MarioAIOptions; 
//import ch.idsia.benchmark.mario.environments.MarioEnvironment;

/**
 * Created by IntelliJ IDEA.
 * User: julian
 * Date: May 5, 2009
 * Time: 12:46:43 PM
 */

/**
 * The <code>Play</code> class shows how simple is to run a MarioAI Benchmark.
 * It shows how to set up some parameters, create a task,
 * use the CmdLineParameters class to set up options from command line if any.
 * Defaults are used otherwise.
 *
 * @author Julian Togelius, Sergey Karakovskiy
 * @version 1.0, May 5, 2009
 */

public final class Testing
{
/**
 * <p>An entry point of the class.</p>
 *
 * @param args input parameters for customization of the benchmark.
 * @see ch.idsia.scenarios.oldscenarios.MainRun
 * @see ch.idsia.tools.MarioAIOptions
 * @see ch.idsia.benchmark.mario.simulation.SimulationOptions
 * @since MarioAI-0.1
 */

public static void main(String[] args)
{
    MarioAIOptions marioAIOptions = new MarioAIOptions(args);
    marioAIOptions.setLevelType(0);
    marioAIOptions.setLevelDifficulty(0);
    
    MLP mlp = new MLP(255,64,12);
    DQNAgent agent = new DQNAgent(mlp, 0.9);
    marioAIOptions.setAgent(agent);
    
    ReplayMemory rm = new ReplayMemory(10000, 255);
    final DQLTask basicTask = new DQLTask(marioAIOptions, rm);
    
    marioAIOptions.setVisualization(true);
    final MarioCustomSystemOfValues m = new MarioCustomSystemOfValues();
    basicTask.doEpisodes(1, true, 5);
    
//    marioAIOptions.setVisualization(true);
//    basicTask.runSingleEpisode(1,false);
    
    System.out.println("\nEvaluationInfo: \n" + basicTask.getEnvironment().getEvaluationInfoAsString());
    System.out.println("\nCustom : \n" + basicTask.getEnvironment().getEvaluationInfo().computeWeightedFitness(m));
    System.exit(0);
}
}
