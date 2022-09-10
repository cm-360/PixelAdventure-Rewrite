package com.github.cm360.pixadv.sound.beethoven;

import java.io.ByteArrayInputStream;
import java.util.HashSet;
import java.util.Set;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;

import com.github.cm360.pixadv.network.endpoints.Client;
import com.github.cm360.pixadv.registry.Identifier;
import com.github.cm360.pixadv.util.Logger;

public class Beethoven {

	private final Client client;
	
	private Set<Clip> sounds;

	// Constructor
	public Beethoven(Client client) {
		this.client = client;
		this.sounds = new HashSet<Clip>();
	}
	
	public void playSound(Identifier soundId) {
		try {
			Clip clip = AudioSystem.getClip();
			clip.open(AudioSystem.getAudioInputStream(new ByteArrayInputStream(client.getRegistry().getSound(soundId).array())));
			clip.addLineListener(new LineListener() {
				@Override
				public void update(LineEvent event) {
					if (event.getType().equals(LineEvent.Type.STOP)) {
						sounds.remove(clip);
						clip.close();
					}
				}
			});
			clip.start();
			sounds.add(clip);
		} catch (Exception e) {
			Logger.logException("Exception while playing sound '%s'!", e, soundId);
		}
	}

}
