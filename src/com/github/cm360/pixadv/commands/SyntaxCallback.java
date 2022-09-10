package com.github.cm360.pixadv.commands;

@FunctionalInterface
public interface SyntaxCallback {

	public String invoke(Object... arguments);

}
