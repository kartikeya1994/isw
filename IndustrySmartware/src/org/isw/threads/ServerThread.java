package org.isw.threads;

import java.io.IOException;

import org.isw.WebSocketServer;

import fi.iki.elonen.SimpleWebServer;

public class ServerThread extends Thread{

	WebSocketServer ws;
	int port = 9091;
	public ServerThread(WebSocketServer ws) {
		this.ws = ws;
		this.setDaemon(false);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		 
		 
         try {
			ws.start(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         try {
			sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
            
         runWebServer();
            
	}
	private void runWebServer() {
		 String[] args = {
                "--host",
                "0.0.0.0",
                "--port",
                "9090",
                "--dir",
                "www"
            };
		 
		SimpleWebServer.main(args);
		
	}
}
