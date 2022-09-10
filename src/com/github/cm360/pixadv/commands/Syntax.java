package com.github.cm360.pixadv.commands;

import java.util.List;

public class Syntax {

	protected SyntaxCallback callback;
	protected List<Class<?>> argumentTypes;
	
	public Syntax(SyntaxCallback callback, Class<?>... argumentTypes) {
		this.callback = callback;
		this.argumentTypes = List.of(argumentTypes);
	}
	
	public boolean compatibleWith(Object... arguments) {
		if (arguments.length < argumentTypes.size()) {
			return false;
		} else {
			int i;
			// Check for incompatible argument types
			for (i = 0; i < argumentTypes.size(); i++)
				if (!argumentTypes.get(i).isAssignableFrom(arguments[i].getClass()))
					return false;
			return true;
		}
	}
	
	public String invoke(Object... arguments) {
		return callback.invoke(arguments);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getName() + "[");
		String.join(",", argumentTypes.stream().map(c -> c.getName()).toList());
		return sb.toString() + "]";
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

}
