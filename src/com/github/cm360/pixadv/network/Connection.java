package com.github.cm360.pixadv.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

import com.github.cm360.pixadv.util.Logger;

public class Connection {

	private final Socket internalSocket;
	private final OutputStream tcpOut;
	private final InputStream tcpIn;
	
	private final List<Packet> queue;
	private Thread receiverThread;
	
	
	public Connection(Socket socket) throws IOException {
		internalSocket = socket;
		tcpOut = internalSocket.getOutputStream();
		tcpIn = internalSocket.getInputStream();
		queue = new LinkedList<Packet>();
		// Start data receiver thread
		receiverThread = new Thread(() -> {
			try {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				int read = 0;
				while (!internalSocket.isClosed() && (read = tcpIn.read()) != -1) {
					// Check if packet payload should be terminated yet
					if ((char) read == '\0') {
						try {
							String[] packetSplit = buffer.toString("UTF-8").split(";", 2);
							Class<?> packetClass = Class.forName(packetSplit[0]);
							// For security, the class must be a type of packet
							if (Packet.class.isAssignableFrom(packetClass)) {
								synchronized (queue) {
									// Create new, empty packet of correct type
									Packet packet = (Packet) packetClass.getConstructor().newInstance();
									// Load packet data from payload
									packet.deserialize(new String(Base64.getDecoder().decode(packetSplit[1]), StandardCharsets.UTF_8));
									// Add packet to queue and notify any waiting threads
									queue.add(packet);
									queue.notifyAll();
									//System.out.printf("Received '%s', data: '%s'\n", packetClass.getCanonicalName(), packetSplit[1]);
								}
							} else {
								Logger.logMessage(Logger.ERROR, "Invalid packet: %s is not a subclass of %s", packetClass, Packet.class);
								close();
							}
						} catch (Exception e) {
							Logger.logException("Invalid packet!", e);
							close();
						}
						buffer = new ByteArrayOutputStream();
					} else {
						buffer.write(read);
					}
				}
			} catch (Exception e) {
				Logger.logException("Error while reading from socket!", e);
				close();
			}
		}, String.format("Connection@%s:%s", socket.getInetAddress().getHostAddress(), socket.getPort()));
		receiverThread.start();
	}
	
	public void close() {
		try {
			internalSocket.close();
		} catch (IOException e) {
			Logger.logException("Error while closing socket!", e);
		}
	}
	
	/**
	 * Send a packet over the internal socket to the opposite endpoint
	 * @param packet The packet to send.
	 * @throws IOException 
	 */
	public void send(Packet packet) throws IOException {
		// Encode packet contents to prevent null characters from being used to create fake packets
		String packetData = Base64.getEncoder().encodeToString(packet.serialize().getBytes(StandardCharsets.UTF_8));
		// Write packet class and data
		tcpOut.write(String.format("%s;%s\0", packet.getClass().getCanonicalName(), packetData).getBytes(StandardCharsets.UTF_8));
		tcpOut.flush();
	}
	
	/**
	 * @param packetType
	 * @return
	 */
	public Packet consume(Class<? extends Packet> packetType) {
		// Check for an already received packet
		for (Packet packet : queue) {
			if (packetType.isAssignableFrom(packet.getClass())) {
				queue.remove(packet);
				return packet;
			}
		}
		return null;
	}
	
	/**
	 * @param packetType
	 * @param timeout
	 * @return
	 */
	public Packet await(Class<? extends Packet> packetType, int timeout) {
		long startTime = System.currentTimeMillis();
		try {
			long elapsed = 0;
			while (elapsed < timeout) {
				synchronized (queue) {
					queue.wait(timeout - elapsed);
					Packet packet = consume(packetType);
					if (packet != null)
						return packet;
					else
						elapsed = System.currentTimeMillis() - startTime;
				}
			}
		} catch (InterruptedException e) {
			Logger.logException("Interrupted!", e);
		}
		return null;
	}

}
