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
	private ServerData data;
	private Thread thread = null;
//	private ConcurrentHashMap <String, String> map = new ConcurrentHashMap<String, String>();
//	private ConcurrentHashMap <String, String> replicaMap = new ConcurrentHashMap<String, String>();

	

	public SlaveServer() {
//		map.put("11", "bar");
//		map.put("12", "bar1");
		this.data = new ServerData();
		this.data.setMap(new ConcurrentHashMap<String, String>());
		this.data.setReplicaMap(new ConcurrentHashMap<String, String>());

	}

	public SlaveServer(String Ip, int Port) {
		super();
		this.Ip = Ip;
		this.Port = Port;
//		map.put("11", "bar");
//		map.put("12", "bar1");

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

		requestHandler = new RequestHandler(server, this.Port, this.data);
		thread = new Thread(requestHandler);
		thread.start();
	}

	public void stop(String masterIp,int masterPort) throws Exception {
		Socket socket = new Socket(masterIp, masterPort);
		JSONObject response = new JSONObject();
		response.put("msgType", "EXIT");
		
		JSONArray array = new JSONArray();
		for (String key : this.data.map.keySet()) {
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("Key", key);
			jsonObj.put("Value", this.data.map.get(key));
			array.add(jsonObj);
		}
		response.put("OriginalData", array);
		
		array = new JSONArray();
		for (String key : this.data.replicaMap.keySet()) {
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("Key", key);
			jsonObj.put("Value", this.data.replicaMap.get(key));
			array.add(jsonObj);
		}
		response.put("ReplicaData", array);
		
		response.put("IP", this.Ip);
		response.put("PORT", this.Port);
		
		DataOutputStream out    = new DataOutputStream(socket.getOutputStream());
		out.writeUTF(response.toString());
		DataInputStream in = new DataInputStream(socket.getInputStream());
		JSONObject ack = (JSONObject)new JSONParser().parse( (String)in.readUTF() );
		if(ack.get("msgType").equals("ExitAck")) {
			System.out.println(ack);
			requestHandler.stop();
			if(in != null)
				in.close();
			if(socket != null && !socket.isClosed())
				socket.close();
			if(server != null && !server.isClosed())
				server.close();
			System.out.println("Server stopped !");
			
		}else {
			System.out.println("Unable to Stop Server !");
		}
		try {
			System.exit(0);

		}
		catch(Exception e) {
			
		}
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
			
			if(respObj.get("Complete").equals("NO")) {
				String finalResponse = input.readUTF();
				JSONObject finalRespObj = (JSONObject)parser.parse(finalResponse);
//				System.out.println(finalRespObj);
				JSONArray array = (JSONArray)finalRespObj.get("Data");
//				System.out.println(array);
				for(int i = 0;i < array.size(); i++) {
					JSONObject dataObj = (JSONObject)array.get(i);
//					System.out.println(dataObj);
					String key = (String)dataObj.get("Key");
					String value = (String)dataObj.get("Value");
//					System.out.println(key);
//					System.out.println(value);
					this.data.map.put(key, value);
				}
				
				JSONArray replicaArray = (JSONArray)finalRespObj.get("ReplicationData");
				for(int i = 0; i < replicaArray.size(); i++) {
					JSONObject dataObj = (JSONObject)replicaArray.get(i);
					String key = (String)dataObj.get("Key");
					String value = (String)dataObj.get("Value");
					this.data.replicaMap.put(key, value);
				}
				JSONObject ackObj = new JSONObject();
				ackObj.put("msgType", "CloneAck");
				ackObj.put("Status", "Success");
				out.writeUTF(ackObj.toString());
				System.out.println(socket.getPort());
				System.out.println(ackObj.toString());
				System.out.println("Message sent!");
						
			}
			else if(respObj.get("Complete").equals("RE")) {
				String finalResponse = input.readUTF();
				JSONObject finalRespObj = (JSONObject)parser.parse(finalResponse);
				
				JSONArray array = (JSONArray)finalRespObj.get("ReplicaData");
				for(int i = 0;i < array.size(); i++) {
					JSONObject dataObj = (JSONObject)array.get(i);
					String key = (String)dataObj.get("Key");
					String value = (String)dataObj.get("Value");
					this.data.map.put(key, value);
				}
				
				JSONArray replicaArray = (JSONArray)finalRespObj.get("OriginalData");
				for(int i = 0; i < replicaArray.size(); i++) {
					JSONObject dataObj = (JSONObject)replicaArray.get(i);
					String key = (String)dataObj.get("Key");
					String value = (String)dataObj.get("Value");
					this.data.replicaMap.put(key, value);
				}
			}
		}

		catch(Exception e) {
			System.out.println("Exception !");
			e.printStackTrace();
			//System.out.println("Exception:" + e.printStackTrace());
		}


	}



}
