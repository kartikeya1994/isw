package org.isw;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.json.simple.JSONObject;

public class MachineLogger{
	static InetAddress ip;
	static Socket socket;
	static ObjectOutputStream out;
	public static void init(InetAddress ip){
		Logger.init(ip);
	}
	public static void connect() throws IOException{
		Logger.connect(2);
	}
	
	public static void log(int status, String logMessage) throws IOException {
		Logger.log(status, logMessage);
		
		JSONObject obj = new JSONObject();
		obj.put("type", "log");
		obj.put("log", logMessage);
		obj.put("status_code", String.valueOf(status));
		for(NanoWebSocket nws : Machine.wsd.getWebSockets())
			 nws.send(obj.toJSONString());
	}
	
	public static void log(MachineResultPacket mrp) {
		Logger.log(mrp);
	}
	public static void timeLog(long time) throws IOException {
		JSONObject obj = new JSONObject();
		obj.put("type", "time_stamp");
		obj.put("time", String.valueOf(time));
		for(NanoWebSocket nws : Machine.wsd.getWebSockets())
			 nws.send(obj.toJSONString());
		
	}
}
