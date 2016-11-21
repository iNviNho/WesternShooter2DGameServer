package server;

import java.awt.Canvas;
import java.awt.Dimension;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import game.client.Client;
import game.client.Player;
import server.packet.Packet;
import server.packet.Packet00Login;
import server.packet.Packet01Move;

public class Server extends Canvas implements Runnable {
	
	private static final long serialVersionUID = 1L;
	
	public DatagramSocket socket;
	public int port;
	
	public boolean running = false;
	public Thread thread;
	public JFrame frame;
	
	public List<Client> clients = new ArrayList<Client>();
	
	public static void main(String[] args) {
		Server server = new Server();
		server.start();
	}
	
	public Server() {

		this.setResolution();
		this.askAndSetPort();
		
		try {
			this.socket = new DatagramSocket(this.port);
			System.out.println("Server was succesfully started");
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
	}
	
	public void run() {
		
		byte[] data = new byte[1024];
		
		while (true) {
			DatagramPacket packet = new DatagramPacket(data, data.length);
			try {
				socket.receive(packet);
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.parsePacket(packet);
		}
	}
	
	public void sendData(byte[] data, InetAddress ipAddress, int port) {
		
		DatagramPacket packet = new DatagramPacket(data, data.length, ipAddress, port);
		try {
			socket.send(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void parsePacket(DatagramPacket packet) {
		
		byte[] data = packet.getData();
		String message = new String(data).trim();
		int packetId = this.getPacketIdFromPacket(message);
		
		switch (packetId) {
		default:
		// now we have a new player
		case 00:
			// first we construct login packet from given message
			Packet00Login loginPacket = new Packet00Login(packet.getData());
			this.newPlayer(loginPacket, packet.getAddress(), packet.getPort());
			break;
		case 01:
			Packet01Move movePacket = new Packet01Move(packet.getData());
			this.movePlayer(movePacket);
			break;
		}
	}	
	
	private void movePlayer(Packet01Move movePacket) {
		
		for (Client c : this.clients) {
			
			if (c.player.username.equals(movePacket.username)) {
				
				c.player.x = movePacket.x;
				c.player.y = movePacket.y;
				
				Packet01Move newMovePacket = new Packet01Move(c.player.username, c.player.x, c.player.y);
				this.sendToAllClients(newMovePacket.getDataForSending(), c.player.username);
				break;
			}
		}
		
	}
	
	private void newPlayer(Packet00Login loginPacket, InetAddress ipAddress, int port) {
		
		// we create new player
		Player player = new Player(loginPacket.getUsername(), loginPacket.getX(), loginPacket.getY());
		// we create new client
		Client client = new Client(player, ipAddress, port);
		// we add client to server
		this.clients.add(client);
		
		Packet00Login packet = new Packet00Login(player.username, player.x, player.y);
		this.sendToAllClients(packet.getDataForSending(), player.username);
	}
	
	private void sendToAllClients(byte[] data, String exceptClient) {
		// we send to all clients info that we have a new client/player	
		for (Client c : this.clients) {
			if (!c.player.username.equals(exceptClient)) {
				this.sendData(data, c.ipAddress, c.port);
			}
			
		}
	}

	private int getPacketIdFromPacket(String message) {
		int packetId = Integer.parseInt(message.substring(0, 2));
		return packetId;
	}
	
	public void setResolution() {
		
		JFrame frame = new JFrame();
		
		Dimension size = new Dimension(300, 300);
		this.setPreferredSize(size);
		
		frame.setResizable(false);
		frame.add(this);
		frame.pack();
		frame.setLocationRelativeTo(null);	
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		
		this.frame = frame;
	}
	
	public synchronized void start() {
		this.running = true;
		this.thread = new Thread(this, "Display");
		this.thread.start();
	}

	public synchronized void stop() {
		this.running = false;
		try {
			this.thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void askAndSetPort() {
		
		boolean isInteger = false;
		while(!isInteger) {
			
			String port = JOptionPane.showInputDialog("What port would you like to use?", "1234");
			
			if (isInteger(port)) {
				this.port = Integer.parseInt(port);
				isInteger = true;
			} else {
				System.out.println("Port must be integer, please try again ...");
			}
		}
		
	}
	
	private boolean isInteger(String s) {
		int radix = 10;
	    if(s.isEmpty()) return false;
	    for(int i = 0; i < s.length(); i++) {
	        if(i == 0 && s.charAt(i) == '-') {
	            if(s.length() == 1) return false;
	            else continue;
	        }
	        if(Character.digit(s.charAt(i),radix) < 0) return false;
	    }
	    return true;
	}

}
