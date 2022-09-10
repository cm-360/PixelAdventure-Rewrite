package com.github.cm360.pixadv.graphics.swing.frames;

import java.awt.BorderLayout;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.border.EmptyBorder;

import com.github.cm360.pixadv.graphics.swing.components.GameCanvas;

public class ClientFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private GameCanvas gameCanvas;
	
	public ClientFrame(GameCanvas gamePanel) {
		this.gameCanvas = gamePanel;
		setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/assets/textures/gui/icon.png")));
		setTitle("A Pixel Adventure");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 800, 480);
		gamePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		gamePanel.setLayout(new BorderLayout(0, 0));
		setContentPane(gamePanel);
	}
	
	@Override
	public GameCanvas getContentPane() {
		return getGameCanvas();
	}
	
	public GameCanvas getGameCanvas() {
		return gameCanvas;
	}
	
	@Override
	public void dispose() {
		gameCanvas.getClient().closeUniverse();
		disposeNoClose();
		System.exit(0);
	}
	
	public void disposeNoClose() {
		super.dispose();
	}

}
