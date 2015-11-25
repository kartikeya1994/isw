package org.isw;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoWSD.WebSocket;
import fi.iki.elonen.NanoWSD.WebSocketFrame;
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;

public class NanoWebSocket extends WebSocket{

	public NanoWebSocket(IHTTPSession handshakeRequest) {
		super(handshakeRequest);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onClose(CloseCode arg0, String arg1, boolean arg2) {
		System.out.println("Connection closed");
		
	}

	@Override
	protected void onException(IOException arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onMessage(WebSocketFrame arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onOpen() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onPong(WebSocketFrame arg0) {
		// TODO Auto-generated method stub
		
	}

}
