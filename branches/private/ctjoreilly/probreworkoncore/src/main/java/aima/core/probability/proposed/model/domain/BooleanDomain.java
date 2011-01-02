package aima.core.probability.proposed.model.domain;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class BooleanDomain extends AbstractFiniteDomain {

	private static Set<Boolean> _possibleValues = null;
	static {
		// Keep consistent order
		_possibleValues = new LinkedHashSet<Boolean>();
		_possibleValues.add(Boolean.TRUE);
		_possibleValues.add(Boolean.FALSE);
		// Ensure cannot be modified
		_possibleValues = Collections.unmodifiableSet(_possibleValues);
	}

	public BooleanDomain() {

	}

	// 
	// START-Domain

	public int size() {
		return 2;
	}

	public boolean isOrdered() {
		return false;
	}

	// END-Domain
	//

	//
	// START-DiscreteDomain
	public Set<Boolean> getPossibleValues() {
		return _possibleValues;
	}

	// END-DiscreteDomain
	//

	@Override
	public boolean equals(Object o) {
		return o instanceof BooleanDomain;
	}

	@Override
	public int hashCode() {
		return _possibleValues.hashCode();
	}
}