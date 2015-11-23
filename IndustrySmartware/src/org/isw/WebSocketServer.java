package org.isw;

import fi.iki.elonen.NanoWSD;

public class WebSocketServer extends NanoWSD {

	NanoWebSocket nws; 
	public WebSocketServer(int port) {
		super(port);
		
	}

	@Override
	protected WebSocket openWebSocket(IHTTPSession arg0) {
		nws = new NanoWebSocket(arg0);
		return nws;
	}
	
	public WebSocket getWebSocket(){
		return nws;
	}

}
