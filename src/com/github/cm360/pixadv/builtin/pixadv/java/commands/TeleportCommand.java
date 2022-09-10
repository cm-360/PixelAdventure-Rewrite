package com.github.cm360.pixadv.builtin.pixadv.java.commands;

import java.util.UUID;

import com.github.cm360.pixadv.commands.Command;
import com.github.cm360.pixadv.commands.Syntax;
import com.github.cm360.pixadv.world.storage.universe.Universe;
import com.github.cm360.pixadv.world.storage.world.World;
import com.github.cm360.pixadv.world.types.entities.Entity;

public class TeleportCommand extends Command {

	protected Universe universe;
	
	public TeleportCommand(Universe universe) {
		super();
		this.universe = universe;
		// Teleports self to position
		addSyntax(new Syntax(args -> {
			World world = universe.getCurrentWorld();
			UUID playerId = universe.getPlayerId();
			if (playerId != null) {
				Entity player = world.getEntity(playerId);
				if (player != null) {
					synchronized (player) {
						double x = ((Number) args[0]).doubleValue();
						double y = ((Number) args[1]).doubleValue();
						player.setX(x);
						player.setY(y);
						return "Teleported %s to %f %f".formatted(player, x, y);
					}
				} else {
					return "The player could not be found!";
				}	
			} else {
				return "This command is only available when executed by the client!";
			}
		}, Number.class, Number.class));
	}
	
	public boolean teleport(Entity entity, World fromWorld, World toWorld, double x, double y) {
		return true;
	}
	
	@Override
	public String getName() {
		return "tp";
	}

}
