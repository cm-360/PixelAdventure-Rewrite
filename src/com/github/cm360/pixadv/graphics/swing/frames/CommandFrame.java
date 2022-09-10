package com.github.cm360.pixadv.graphics.swing.frames;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import com.github.cm360.pixadv.network.endpoints.Client;
import com.github.cm360.pixadv.world.storage.universe.Universe;

public class CommandFrame extends JFrame {

	private static final long serialVersionUID = -395930867543608662L;

	private JPanel contentPane;
	private JTextField commandField;
	private JTextPane historyPane;
	
	private Client client;
	

	/**
	 * Create the frame.
	 */
	public CommandFrame(Client client) {
		this.client = client;
		init();
	}
		
	public void init() {
		setTitle("Commands");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JScrollPane historyScroll = new JScrollPane();
		historyScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		contentPane.add(historyScroll, BorderLayout.CENTER);
		
		historyPane = new JTextPane();
		historyPane.setEditable(false);
		historyScroll.setViewportView(historyPane);
		
		commandField = new JTextField();
		commandField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				String command = commandField.getText();
				if (!command.isBlank()) {
					Universe universe = client.getCurrentUniverse();
					if (universe != null) {
						try {
							// Add command to history
							addHistory("> " + command);
							String result = universe.getCommandProcessor().processCommand(commandField.getText());
							if (!result.isBlank()) {
								commandField.setText("");
								addHistory(result);
							}
						} catch (Exception e) {
							// Add exception info to history
							addHistory("  Exception caught while executing command!");
							addHistory("  " + e.getClass().getName() + ": " + e.getMessage());
							// Print stacktrace
							System.out.printf("Exception caught while executing command '%s'!\n", command);
							e.printStackTrace();
						}
					}
				}
			}
		});
		contentPane.add(commandField, BorderLayout.SOUTH);
		commandField.setColumns(10);
	}
	
	// Utility method
	private void addHistory(String line) {
		historyPane.setText(String.format("%s%s\n", historyPane.getText(), line));
	}

}
