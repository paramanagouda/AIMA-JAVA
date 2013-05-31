package aima.core.logic.propositional.parsing.ast;

import java.util.Comparator;

/**
 * A utility routine for comparing two propositional symbols.
 * 
 * @author Ravi Mohan
 * @author Ciaran O'Reilly
 */
public class PropositionSymbolComparator implements Comparator<PropositionSymbol> {

	@Override
	public int compare(PropositionSymbol one, PropositionSymbol two) {
		return one.getSymbol().compareTo(two.getSymbol());
	}
}
