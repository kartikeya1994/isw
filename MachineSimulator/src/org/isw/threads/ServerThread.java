package org.isw.threads;

import java.io.IOException;

import org.isw.WebSocketServer;

public class ServerThread extends Thread{
	WebSocketServer wsd;
	public ServerThread(WebSocketServer wsd){
		this.wsd = wsd;
	}
	
	@Override
	public void run() {
         try {
			wsd.start(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
