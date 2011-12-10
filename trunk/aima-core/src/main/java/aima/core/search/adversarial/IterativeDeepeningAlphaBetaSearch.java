package aima.core.search.adversarial;

import java.util.ArrayList;
import java.util.List;

import aima.core.search.framework.Metrics;

/**
 * Implements an iterative deepening Minimax search with alpha-beta pruning and
 * action ordering. Maximal computation time is specified in seconds. The
 * algorithm is implemented as template method and can be configured and tuned
 * by subclassing.
 * 
 * @author Ruediger Lunde
 * 
 * @param <STATE>
 *            Type which is used for states in the game.
 * @param <ACTION>
 *            Type which is used for actions in the game.
 * @param <PLAYER>
 *            Type which is used for players in the game.
 */
public class IterativeDeepeningAlphaBetaSearch<STATE, ACTION, PLAYER>
		implements AdversarialSearch<STATE, ACTION> {

	protected Game<STATE, ACTION, PLAYER> game;
	protected double utilMax;
	protected double utilMin;
	private int currDepthLimit;
	private boolean maxDepthReached;
	private long maxTime;
	private boolean logEnabled;

	private int expandedNodes;
	private int maxDepth;

	/** Creates a new search object for a given game. */
	public static <STATE, ACTION, PLAYER> IterativeDeepeningAlphaBetaSearch<STATE, ACTION, PLAYER> createFor(
			Game<STATE, ACTION, PLAYER> game, double utilMin, double utilMax,
			int time) {
		return new IterativeDeepeningAlphaBetaSearch<STATE, ACTION, PLAYER>(
				game, utilMin, utilMax, time);
	}

	public IterativeDeepeningAlphaBetaSearch(Game<STATE, ACTION, PLAYER> game,
			double utilMin, double utilMax, int time) {
		this.game = game;
		this.utilMin = utilMin;
		this.utilMax = utilMax;
		this.maxTime = time * 1000; // internal: ms instead of s
	}

	public void setLogEnabled(boolean b) {
		logEnabled = b;
	}

	/**
	 * Template method controlling the search. In this implementation, action
	 * ordering is only performed on top-level.
	 */
	public ACTION makeDecision(STATE state) {
		List<ACTION> results = null;
		double resultValue = Double.NEGATIVE_INFINITY;
		StringBuffer logText = null;
		expandedNodes = 0;
		maxDepth = 0;
		currDepthLimit = 0;
		long startTime = System.currentTimeMillis();
		boolean exit = false;
		do {
			currDepthLimit++;
			maxDepthReached = false;
			List<ACTION> newResults = new ArrayList<ACTION>();
			double newResultValue = Double.NEGATIVE_INFINITY;
			double secondBestValue = Double.NEGATIVE_INFINITY;
			if (logEnabled)
				logText = new StringBuffer("depth " + currDepthLimit + ": ");
			for (ACTION action : orderActions(state, game.getActions(state))) {
				if (results != null
						&& System.currentTimeMillis() > startTime + maxTime) {
					exit = true;
					break;
				}
				double value = minValue(game.getResult(state, action),
						game.getPlayer(state), Double.NEGATIVE_INFINITY,
						Double.POSITIVE_INFINITY, 1);
				if (logEnabled)
					logText.append(action + "->" + value + " ");
				if (value >= newResultValue) {
					if (value > newResultValue) {
						secondBestValue = newResultValue;
						newResultValue = value;
						newResults.clear();
					}
					newResults.add(action);
				}
			}
			if (logEnabled)
				System.out.println(logText);
			if (!exit || isSignificantlyBetter(newResultValue, resultValue)) {
				results = newResults;
				resultValue = newResultValue;
			}
			if (!exit && results.size() == 1
					&& this.isSignificantlyBetter(resultValue, secondBestValue))
				break;
		} while (!exit && maxDepthReached && !hasSafeWinner(resultValue));
		return results.get(0);
	}

	public double maxValue(STATE state, PLAYER player, double alpha,
			double beta, int depth) { // returns an utility value
		expandedNodes++;
		maxDepth = Math.max(maxDepth, depth);
		if (game.isTerminal(state) || depth >= currDepthLimit) {
			return eval(state, player, depth);
		} else {
			double value = Double.NEGATIVE_INFINITY;
			for (ACTION action : game.getActions(state)) {
				// for (ACTION action : orderActions(state,
				// game.getActions(state))) {
				value = Math.max(
						value,
						minValue(game.getResult(state, action), player, alpha,
								beta, depth + 1));
				if (value >= beta)
					return value;
				alpha = Math.max(alpha, value);
			}
			return value;
		}
	}

	public double minValue(STATE state, PLAYER player, double alpha,
			double beta, int depth) { // returns an utility
		expandedNodes++;
		maxDepth = Math.max(maxDepth, depth);
		if (game.isTerminal(state) || depth >= currDepthLimit) {
			return eval(state, player, depth);
		} else {
			double value = Double.POSITIVE_INFINITY;
			for (ACTION action : game.getActions(state)) {
				// for (ACTION action : orderActions(state,
				// game.getActions(state))) {
				value = Math.min(
						value,
						maxValue(game.getResult(state, action), player, alpha,
								beta, depth + 1));
				if (value <= alpha)
					return value;
				beta = Math.min(beta, value);
			}
			return value;
		}
	}

	/** Returns some statistic data from the last search. */
	@Override
	public Metrics getMetrics() {
		Metrics result = new Metrics();
		result.set("expandedNodes", expandedNodes);
		result.set("maxDepth", maxDepth);
		return result;
	}

	/**
	 * Primitive operation which is used to stop iterative deepening search in
	 * situations where a clear best action exists. This implementation returns
	 * always false.
	 */
	protected boolean isSignificantlyBetter(double newUtility, double utility) {
		return false;
	}

	/**
	 * Primitive operation which is used to stop iterative deepening search in
	 * situations where a safe winner has been identified. This implementation
	 * returns true if the given value (for the currently preferred action
	 * result) is the highest or lowest utility value possible.
	 */
	protected boolean hasSafeWinner(double resultUtility) {
		return resultUtility <= utilMin || resultUtility >= utilMax;
	}

	/**
	 * Primitive operation, which estimates the value for (not necessarily
	 * terminal) states. This implementation returns the utility value for
	 * terminal states and <code>(utilMin + utilMax) / 2</code> for non-terminal
	 * states.
	 */
	protected double eval(STATE state, PLAYER player, int depth) {
		if (game.isTerminal(state)) {
			return game.getUtility(state, player);
		} else {
			maxDepthReached = true;
			return (utilMin + utilMax) / 2;
		}
	}

	/**
	 * Primitive operation for action ordering. This implementation preserves
	 * the original order (provided by the game).
	 */
	public List<ACTION> orderActions(STATE state, List<ACTION> actions) {
		return actions;
	}
}
