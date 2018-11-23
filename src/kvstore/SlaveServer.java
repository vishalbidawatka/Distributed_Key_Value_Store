package kvstore;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class SlaveServer {

	private ServerSocket server;
	private Socket socket;
	private String Ip;
	private int Port;
	private RequestHandler requestHandler;
	private ConcurrentHashMap <String, String> map = new ConcurrentHashMap<String, String>();
	private ConcurrentHashMap <String, String> replicaMap = new ConcurrentHashMap<String, String>();

	

	public SlaveServer() {
		map.put("11", "bar");
		map.put("12", "bar1");
	}

	public SlaveServer(String Ip, int Port) {
		super();
		this.Ip = Ip;
		this.Port = Port;
		map.put("11", "bar");
		map.put("12", "bar1");

	}



	public String getIp() {
		return Ip;
	}

	public void setIp(String ip) {
		Ip = ip;
	}

	public int getPort() {
		return Port;
	}

	public void setPort(int port) {
		Port = port;
	}

	public void start() throws IOException {

		requestHandler = new RequestHandler(server, this.Port, map, replicaMap);
		Thread thread = new Thread(requestHandler);
		thread.start();
		//requestHandler.run();

	}

	public void stop() {
		System.out.println("Server stopped !");
		requestHandler.stop();
	}
	public void show() {
		System.out.println("Server is running !");
	}

	public void register(String masterIp, int masterPort) throws Exception {
		try {
			Socket socket = new Socket(masterIp, masterPort); 
			System.out.println("Connected"); 

			JSONObject obj  = new JSONObject();
			obj.put("msgType", "Register");
			obj.put("IP", this.Ip);
			obj.put("PORT", this.Port);
			// takes input from terminal 

			// sends output to the socket 
			DataOutputStream out    = new DataOutputStream(socket.getOutputStream()); 
			out.writeUTF(obj.toString());

			DataInputStream input  = new DataInputStream(socket.getInputStream()); 
			String response = input.readUTF();
			System.out.println(response);
			JSONParser parser = new JSONParser();
			JSONObject respObj = (JSONObject)parser.parse(response);
			if(respObj.get("Complete").equals("No")) {
				String finalResponse = input.readUTF();
				JSONObject finalRespObj = (JSONObject)parser.parse(finalResponse);
				JSONArray array = (JSONArray)finalRespObj.get("Data");
				for(int i = 0;i < array.size(); i++) {
					JSONObject dataObj = (JSONObject)array.get(i);
					String key = (String)dataObj.get("key");
					String value = (String)dataObj.get("value");
					map.put(key, value);
				}
			}



		}

		catch(Exception e) {
			e.printStackTrace();
			//System.out.println("Exception:" + e.printStackTrace());
		}


	}



}
