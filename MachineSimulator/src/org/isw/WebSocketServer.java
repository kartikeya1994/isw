package org.isw;


import fi.iki.elonen.NanoWSD;

public class WebSocketServer extends NanoWSD{
	NanoWebSocket ws;
	public WebSocketServer(int port) {
		super(port);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected WebSocket openWebSocket(IHTTPSession arg0) {
		ws = new NanoWebSocket(arg0);
		return ws;
	}

	public WebSocket getWebSocket(){
		return ws;
	} 

}
