package org.isw;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;

public class InitConfig implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -4861100990433675980L;
	public int shiftDuration;
	public int simuLationCount;
	public int scaleFactor;
	public Component[] compList;
	public void send(InetAddress ip, int port) throws IOException {
		Socket socket = new Socket(ip, port);
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(this);
		oos.close();
		socket.close();
		
	}

}
