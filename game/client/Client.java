package game.client;

import java.net.InetAddress;

public class Client {

	public Player player;
	public InetAddress ipAddress;
	public int port;
	
	public Client(Player player, InetAddress ipAddress, int port) {
		this.player = player;
		this.ipAddress = ipAddress;
		this.port = port;
	}
	
}
