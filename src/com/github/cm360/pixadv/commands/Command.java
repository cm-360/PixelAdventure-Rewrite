package com.github.cm360.pixadv.commands;

import java.util.HashSet;
import java.util.Set;

/**
 * The Class Command
 */
public abstract class Command {

	/** A set of syntaxes currently in this command */
	protected Set<Syntax> syntaxes;
	
	/**
	 * Instantiates a new Command object
	 */
	public Command() {
		syntaxes = new HashSet<Syntax>();
	}
	
	/**
	 * Gets the name this command can be executed with
	 *
	 * @return The name of this command
	 */
	public abstract String getName();
	
	/**
	 * Adds a syntax to this command
	 *
	 * @param syntax The syntax to add
	 * @return true, if successful
	 */
	public boolean addSyntax(Syntax syntax) {
		return syntaxes.add(syntax);
	}
	
	/**
	 * Removes a syntax from this command
	 *
	 * @param syntax The syntax to remove
	 * @return true, if successful
	 */
	public boolean removeSyntax(Syntax syntax) {
		return syntaxes.remove(syntax);
	}
	
	/**
	 * Gets the syntaxes this command offers
	 *
	 * @return A set of the available syntaxes
	 */
	public Set<Syntax> getSyntaxes() {
		return Set.copyOf(syntaxes);
	}

}
