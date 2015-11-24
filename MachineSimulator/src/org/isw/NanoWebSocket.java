package org.isw;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoWSD.WebSocket;
import fi.iki.elonen.NanoWSD.WebSocketFrame;
import fi.iki.elonen.NanoWSD.WebSocketFrame.CloseCode;

public class NanoWebSocket extends WebSocket{
	WebSocketServer wsd;
	public NanoWebSocket(IHTTPSession handshakeRequest, WebSocketServer wsd) {
		super(handshakeRequest);
		this.wsd = wsd;
		
	}

	@Override
	protected void onClose(CloseCode arg0, String arg1, boolean arg2) {
		wsd.removeWebSocket(this);
		
	}

	@Override
	protected void onException(IOException arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void onMessage(WebSocketFrame arg0) {
		if(arg0.getTextPayload().equals("failure")){
			synchronized(Machine.lock){
				Machine.failureEvent = true;
			}
		}
		
	}

	@Override
	protected void onOpen() {
		System.out.println("Connected");
		
	}

	@Override
	protected void onPong(WebSocketFrame arg0) {
		// TODO Auto-generated method stub
		
	}

}
