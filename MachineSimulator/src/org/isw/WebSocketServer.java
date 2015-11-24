package org.isw;


import java.util.ArrayList;

import fi.iki.elonen.NanoWSD;

public class WebSocketServer extends NanoWSD{
	ArrayList<NanoWebSocket> ws;
	public WebSocketServer(int port) {
		super(port);
		ws = new ArrayList<NanoWebSocket>();
		// TODO Auto-generated constructor stub
	}

	@Override
	protected WebSocket openWebSocket(IHTTPSession arg0) {
		ws.add(new NanoWebSocket(arg0,this));
		return ws.get(ws.size() - 1);
	}

	public ArrayList<NanoWebSocket> getWebSockets(){
		return ws;
	}

	public void removeWebSocket(NanoWebSocket nanoWebSocket) {
		ws.remove(nanoWebSocket);		
	} 

}
