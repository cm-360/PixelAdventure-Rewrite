package com.github.cm360.pixadv.util.tree;

import java.util.HashMap;
import java.util.Map;

public class HashTree<K, V> implements Tree<K, V> {

	private Map<K, Leaf> data;
	
	public HashTree() {
		data = new HashMap<K, Leaf>();
	}
	
	@Override
	public Leaf<?> getLeaf(String path) {
		// TODO Auto-generated method stub
		return null;
	}

}
