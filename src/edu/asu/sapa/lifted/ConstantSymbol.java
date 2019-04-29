package edu.asu.sapa.lifted;

// interacts with the Bindable interface.

public class ConstantSymbol<K> extends Symbol<K> {

	public ConstantSymbol(K name) {
		super(name);
	}

}
