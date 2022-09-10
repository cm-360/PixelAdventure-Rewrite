package com.github.cm360.pixadv.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import com.github.cm360.pixadv.builtin.pixadv.java.commands.NoClipCommand;
import com.github.cm360.pixadv.builtin.pixadv.java.commands.TeleportCommand;
import com.github.cm360.pixadv.util.Logger;
import com.github.cm360.pixadv.world.storage.universe.Universe;

public class CommandProcessor {

	protected Universe universe;
	protected Map<String, Command> commands;
	
	public CommandProcessor(Universe universe) {
		this.universe = universe;
		this.commands = new HashMap<String, Command>();
		this.registerCommand("tp", new TeleportCommand(universe));
		this.registerCommand("noclip", new NoClipCommand(universe));
	}
	
	public String processCommand(String commandString) {
		String result = "Invalid syntax!";
		Pattern argPattern = Pattern.compile("(?:(?<=\")[^\"]+(?=\"))|(?:[^\\s\"]+)");
		try (Scanner argumentScanner = new Scanner(commandString)) {
			List<String> argumentStrings = argumentScanner.tokens().toList();
			// Lookup command
			String commandName = argumentStrings.get(0);
			Command command = commands.get(commandName);
			if (command != null) {
				// Parse argument objects
				Object[] arguments;
				if (argumentStrings.size() > 1)
					arguments = parse(argumentStrings.subList(1, argumentStrings.size())).toArray();
				else
					arguments = new Object[0];
				// Find matching syntaxes
				for (Syntax syntax : command.getSyntaxes()) {
					if (syntax.compatibleWith(arguments)) {
						result = syntax.invoke(arguments);
					}
				}
			} else {
				result = "Command not found: '%s'!".formatted(commandName);
			}
		} catch (Exception e) {
			Logger.logException("An exception occurred while processing a command! Command: '%s'", e, commandString);
			result = e.getClass().getName() + ": " + e.getLocalizedMessage();
		}
		Logger.logMessage(Logger.DEBUG, result);
		return result;
	}
	
	public List<Object> parse(List<String> arguments) {
		List<Object> argObjects = new ArrayList<Object>();
		for (String arg : arguments) {
			if (arg.matches("-?\\d+")) {
				argObjects.add(Integer.parseInt(arg));
			} else if (arg.matches("-?\\d+(?:\\.\\d+)?")) {
				argObjects.add(Double.parseDouble(arg));
			} else {
				argObjects.add(arg);
			}
		}
		return argObjects;
	}
	
	public void registerCommand(String name, Command command) {
		commands.put(name, command);
	}
	
	public Command unregisterCommand(String name) {
		return commands.remove(name);
	}
	
	public boolean unregisterCommand(Command command) {
		return commands.values().remove(command);
	}

}
