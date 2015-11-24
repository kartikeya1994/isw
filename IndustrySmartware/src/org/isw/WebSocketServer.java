package org.isw;

import java.util.ArrayList;

import fi.iki.elonen.NanoWSD;

public class WebSocketServer extends NanoWSD {

	ArrayList <NanoWebSocket> nws; 
	public WebSocketServer(int port) {
		super(port);
		nws = new ArrayList<NanoWebSocket>();
	}

	@Override
	protected WebSocket openWebSocket(IHTTPSession arg0) {
		nws.add(new NanoWebSocket(arg0,this));
		return nws.get(nws.size() - 1);
	}
	
	public ArrayList<NanoWebSocket> getWebSockets(){
		return nws;
	}
	public void removeWebSocket(NanoWebSocket nanoWebSocket) {
		nws.remove(nanoWebSocket);		
	} 

}
