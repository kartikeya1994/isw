package org.isw;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Enumeration;

import org.json.simple.JSONObject;

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
		System.out.println("Sending machinelist to browser");
		Enumeration<InetAddress> ips = Main.machineList.getIPs();
		while(ips.hasMoreElements()){
			JSONObject obj = new JSONObject();
			obj.put("type","machine");
			obj.put("ip", ips.nextElement().getHostAddress());
			
			try {
				send(obj.toJSONString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	@Override
	protected void onPong(WebSocketFrame arg0) {
		// TODO Auto-generated method stub
		
	}

}
