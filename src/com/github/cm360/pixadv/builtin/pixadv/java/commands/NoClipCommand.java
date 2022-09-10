package com.github.cm360.pixadv.builtin.pixadv.java.commands;

import java.util.UUID;

import com.github.cm360.pixadv.commands.Command;
import com.github.cm360.pixadv.commands.Syntax;
import com.github.cm360.pixadv.world.storage.universe.Universe;
import com.github.cm360.pixadv.world.storage.world.World;
import com.github.cm360.pixadv.world.types.entities.Entity;

public class NoClipCommand extends Command {

	protected Universe universe;
	
	public NoClipCommand(Universe universe) {
		super();
		this.universe = universe;
		// Toggles noclip
		addSyntax(new Syntax(args -> {
			World world = universe.getCurrentWorld();
			UUID playerId = universe.getPlayerId();
			if (playerId != null) {
				Entity player = world.getEntity(playerId);
				if (player != null) {
					synchronized (player) {
						boolean noclip = !player.canNoClip();
						player.setNoClip(noclip);
						return "Toggled noclip %s.".formatted(noclip);
					}
				} else {
					return "The player could not be found!";
				}	
			} else {
				return "This command is only available when executed by the client!";
			}
		}));
		// Sets noclip to a certain value
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "noclip";
	}

}
