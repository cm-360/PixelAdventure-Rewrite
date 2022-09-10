package com.github.cm360.pixadv.graphics.picasso;

import java.awt.Image;

public class ChunkImage {

	private Image image;
	private long creationTime;
	
	/**
	 * @param image
	 * @param creationTime
	 */
	public ChunkImage(Image image, long creationTime) {
		this.image = image;
		this.creationTime = creationTime;
	}

	public Image getImage() {
		return image;
	}
	
	public long getCreationTime() {
		return creationTime;
	}

}
