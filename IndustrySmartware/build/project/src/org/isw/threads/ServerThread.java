package org.isw.threads;

import java.io.IOException;

import org.isw.WebSocketServer;

import fi.iki.elonen.SimpleWebServer;

public class ServerThread extends Thread{

	WebSocketServer ws;
	int port = 9091;
	public ServerThread(WebSocketServer ws) {
		this.ws = ws;
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		 String[] args = {
                 "--host",
                 "127.0.0.1",
                 "--port",
                 "9090",
                 "--dir",
                 "www"
             };
		 
		 ws = new WebSocketServer(port);
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
             SimpleWebServer.main(args);
             
            
	}

}
