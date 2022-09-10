package com.github.cm360.pixadv.network.endpoints;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import com.github.cm360.pixadv.network.Connection;
import com.github.cm360.pixadv.network.packets.authentication.LoginAttemptPacket;
import com.github.cm360.pixadv.network.packets.authentication.LoginResponsePacket;
import com.github.cm360.pixadv.network.packets.universe.UniverseInfoPacket;
import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.util.HashUtil;
import com.github.cm360.pixadv.util.Logger;
import com.github.cm360.pixadv.world.storage.universe.LocalUniverse;
import com.github.cm360.pixadv.world.storage.universe.Universe;

public class Server {

	private Registry registry;
	
	private Universe universe;
	
	private ServerSocket serverSocketTcp;
	private Map<String, String> loginInfo;
	private static boolean checkAuth = true;
	private Map<String, Connection> players;
	
	
	public Server(Registry registry, File universeDirectory, String address, int port) throws IOException {
		this(registry, new LocalUniverse(registry, universeDirectory), address, port);
	}
	
	public Server(Registry registry, Universe universe, String address, int port) throws IOException {
		this.registry = registry;
		this.universe = universe;
		loginInfo = new HashMap<String, String>();
		loginInfo.put("Player1", HashUtil.getSHA256Hash("password1"));
		// Bind server to specified address, or all if blank
		String bindAddress = address.isEmpty() ? "0.0.0.0" : address;
		serverSocketTcp = new ServerSocket();
		serverSocketTcp.bind(new InetSocketAddress(bindAddress, port));
		Logger.logMessage(Logger.INFO, "Server hosted on '%s:%d'", bindAddress, serverSocketTcp.getLocalPort());
		// Start server thread
		new Thread(null, () -> {
			while (!serverSocketTcp.isClosed()) {
				try {
					// Accept connection from client
					Socket clientSocketTcp = serverSocketTcp.accept();
					String connectionAddress = "%s:%s".formatted(
							clientSocketTcp.getInetAddress().getHostAddress(),
							clientSocketTcp.getPort());
					Logger.logMessage(Logger.INFO, "Received connection from '%s'", connectionAddress);
					Connection clientConnection = new Connection(clientSocketTcp);
					// Authenticate client if needed
					if (authenticate(clientConnection, connectionAddress)) {
						// Send universe info to client
						clientConnection.send(new UniverseInfoPacket(
								registry.getSignature(),
								universe.getName(),
								universe.getWorldNames()));
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, "ServerMain").start();
	}
	
	
	public Registry getRegistry() {
		return registry;
	}
	
	public Universe getUniverse() {
		return universe;
	}
	
	
	private boolean authenticate(Connection clientConnection, String connectionAddress) throws IOException {
		// Perform handshake with client
		LoginAttemptPacket loginPacket = (LoginAttemptPacket) clientConnection.await(LoginAttemptPacket.class, 10000);
		if (loginPacket == null) {
			// Client did not send login fast enough
			Logger.logMessage(Logger.INFO, "Login timeout for '%s'", connectionAddress);
			clientConnection.send(new LoginResponsePacket(false, "Login timeout"));
			clientConnection.close();
		} else {
			String correctHash = loginInfo.get(loginPacket.getUsername());
			if ((!checkAuth && !players.containsKey(loginPacket.getUsername()))
					|| HashUtil.getSHA256Hash(correctHash + loginPacket.getSalt()).equals(loginPacket.getToken())) {
				Logger.logMessage(Logger.DEBUG, "Successful handshake with '%s'", connectionAddress);
				clientConnection.send(new LoginResponsePacket(true, "Accepted"));
				return true;
			} else {
				Logger.logMessage(Logger.INFO, "Failed handshake with '%s'", connectionAddress);
				clientConnection.send(new LoginResponsePacket(false, "Invalid credentials!"));
				clientConnection.close();
			}
		}
		return false;
	}

}
