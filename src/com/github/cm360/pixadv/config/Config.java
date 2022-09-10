package com.github.cm360.pixadv.config;

import com.github.cm360.pixadv.util.tree.HashTree;
import com.github.cm360.pixadv.util.tree.Tree;

public class Config {

	private Tree<String, Object> config;
	
	public Config() {
		config = new HashTree<String, Object>();
	}
}
