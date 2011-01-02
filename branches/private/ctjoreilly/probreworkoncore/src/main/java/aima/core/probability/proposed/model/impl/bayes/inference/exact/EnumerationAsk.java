package aima.core.probability.proposed.model.impl.bayes.inference.exact;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import aima.core.probability.proposed.model.Distribution;
import aima.core.probability.proposed.model.RandomVariable;
import aima.core.probability.proposed.model.domain.FiniteDomain;
import aima.core.probability.proposed.model.impl.bayes.BayesianNetwork;
import aima.core.probability.proposed.model.impl.bayes.FiniteNode;
import aima.core.probability.proposed.model.impl.bayes.Node;
import aima.core.probability.proposed.model.proposition.AssignmentProposition;
import aima.core.util.Util;

/**
 * Artificial Intelligence A Modern Approach (3rd Edition): Figure 14.9, page
 * 525.
 * 
 * <pre>
 * function ENUMERATION-ASK(X, e, bn) returns a distribution over X
 *   inputs: X, the query variable
 *           e, observed values for variables E
 *           bn, a Bayes net with variables {X} &cup; E &cup; Y /* Y = hidden variables //
 *           
 *   Q(X) <- a distribution over X, initially empty
 *   for each value x<sub>i</sub> of X do
 *       Q(x<sub>i</sub>) <- ENUMERATE-ALL(bn.VARS, e<sub>x<sub>i</sub></sub>)
 *          where e<sub>x<sub>i</sub></sub> is e extended with X = x<sub>i</sub>
 *   return NORMALIZE(Q(X))
 *   
 * ---------------------------------------------------------------------------------------------------
 * 
 * function ENUMERATE-ALL(vars, e) returns a real number
 *   if EMPTY?(vars) then return 1.0
 *   Y <- FIRST(vars)
 *   if Y has value y in e
 *       then return P(y | parents(Y)) * ENUMERATE-ALL(REST(vars), e)
 *       else return &sum;<sub>y</sub> P(y | parents(Y)) * ENUMERATE-ALL(REST(vars), e<sub>y</sub>)
 *           where e<sub>y</sub> is e extended with Y = y
 * </pre>
 * 
 * Figure 14.9 The enumeration algorithm for answering queries on Bayesian
 * networks. <br>
 * <br>
 * Note: The implementation has been extended to handle queries with multiple
 * variables. <br>
 * 
 * @author Ciaran O'Reilly
 */
public class EnumerationAsk {

	public EnumerationAsk() {

	}

	// function ENUMERATION-ASK(X, e, bn) returns a distribution over X
	/**
	 * The ENUMERATION-ASK algorithm in Figure 14.9 evaluates expression trees
	 * (Figure 14.8) using depth-first recursion.
	 * 
	 * @param X
	 *            the query variables.
	 * @param observedEvidence
	 *            observed values for variables E.
	 * @param bn
	 *            a Bayes net with variables {X} &cup; E &cup; Y /* Y = hidden
	 *            variables //
	 * @return a distribution over the query variables.
	 */
	public Distribution enumerationAsk(final RandomVariable[] X,
			final AssignmentProposition[] observedEvidence,
			final BayesianNetwork bn) {

		// Q(X) <- a distribution over X, initially empty
		final Distribution Q = new Distribution(X);
		final ObservedEvidence e = new ObservedEvidence(X, observedEvidence, bn);
		// for each value x<sub>i</sub> of X do
		Distribution.Iterator di = new Distribution.Iterator() {
			int cnt = 0;

			/**
			 * <pre>
			 * Q(x<sub>i</sub>) <- ENUMERATE-ALL(bn.VARS, e<sub>x<sub>i</sub></sub>)
			 *   where e<sub>x<sub>i</sub></sub> is e extended with X = x<sub>i</sub>
			 * </pre>
			 */
			public void iterate(Map<RandomVariable, Object> possibleWorld,
					double probability) {
				for (int i = 0; i < X.length; i++) {
					e.setExtendedValue(X[i], possibleWorld.get(X[i]));
				}
				Q.setValue(cnt, enumerateAll(bn
						.getVariablesInTopologicalOrder(), e));
				cnt++;
			}

			public Object getPostIterateValue() {
				return null; // N/A
			}
		};
		Q.iterateDistribution(di);

		// return NORMALIZE(Q(X))
		return Q.normalize();
	}

	//
	// PROTECTED METHODS
	//
	// function ENUMERATE-ALL(vars, e) returns a real number
	protected double enumerateAll(List<RandomVariable> vars, ObservedEvidence e) {
		// if EMPTY?(vars) then return 1.0
		if (0 == vars.size()) {
			return 1;
		}
		// Y <- FIRST(vars)
		RandomVariable Y = Util.first(vars);
		// if Y has value y in e
		if (e.containsValue(Y)) {
			// then return P(y | parents(Y)) * ENUMERATE-ALL(REST(vars), e)
			return e.posteriorForParents(Y) * enumerateAll(Util.rest(vars), e);
		}
		/**
		 * <pre>
		 *  else return &sum;<sub>y</sub> P(y | parents(Y)) * ENUMERATE-ALL(REST(vars), e<sub>y</sub>)
		 *       where e<sub>y</sub> is e extended with Y = y
		 * </pre>
		 */
		double sum = 0;
		for (Object y : ((FiniteDomain) Y.getDomain()).getPossibleValues()) {
			e.setExtendedValue(Y, y);
			sum += e.posteriorForParents(Y) * enumerateAll(Util.rest(vars), e);
		}

		return sum;
	}

	protected class ObservedEvidence {
		private BayesianNetwork bn = null;
		private AssignmentProposition[] extendedValues = null;
		private RandomVariable[] var = null;
		private Map<RandomVariable, Integer> varIdxs = new HashMap<RandomVariable, Integer>();

		public ObservedEvidence(RandomVariable[] queryVariables,
				AssignmentProposition[] e, BayesianNetwork bn) {
			this.bn = bn;

			int maxSize = bn.getVariablesInTopologicalOrder().size();
			extendedValues = new AssignmentProposition[maxSize];
			var = new RandomVariable[maxSize];
			// query variables go first
			int idx = 0;
			for (int i = 0; i < queryVariables.length; i++) {
				var[idx] = queryVariables[i];
				varIdxs.put(var[idx], idx);
				idx++;
			}
			// initial evidence variables go next
			for (int i = 0; i < e.length; i++) {
				var[idx] = e[i].getRandomVariable();
				varIdxs.put(var[idx], idx);
				extendedValues[idx] = e[i];
				idx++;
			}
			// the remaining slots are left open for the hidden variables
			for (RandomVariable rv : bn.getVariablesInTopologicalOrder()) {
				if (!varIdxs.containsKey(rv)) {
					var[idx] = rv;
					varIdxs.put(var[idx], idx);
					idx++;
				}
			}
		}

		public void setExtendedValue(RandomVariable rv, Object value) {
			extendedValues[varIdxs.get(rv)] = new AssignmentProposition(rv,
					value);
		}

		public boolean containsValue(RandomVariable rv) {
			return null != extendedValues[varIdxs.get(rv)];
		}

		public double posteriorForParents(RandomVariable rv) {
			Node n = bn.getNode(rv);
			if (!(n instanceof FiniteNode)) {
				throw new IllegalArgumentException(
						"Enumeration-Ask only works with finite Nodes.");
			}
			FiniteNode fn = (FiniteNode) n;
			AssignmentProposition[] aps = new AssignmentProposition[1+fn
					.getParents().size()];
			int idx = 0;
			for (Node pn : n.getParents()) {
				aps[idx] = extendedValues[varIdxs.get(pn.getRandomVariable())];
				idx++;
			}
			aps[idx] = extendedValues[varIdxs.get(rv)];

			return fn.getCPT().probabilityFor(aps);
		}
	}

	//
	// PRIVATE METHODS
	//
}