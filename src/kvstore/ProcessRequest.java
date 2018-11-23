package kvstore;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ProcessRequest implements Runnable {

	Socket socket = null;
	private ConcurrentHashMap <String, String> map;
	private ConcurrentHashMap <String, String> replicaMap;

	public ProcessRequest(Socket socket, ConcurrentHashMap <String, String> map, ConcurrentHashMap <String, String> replicaMap) {
		super();
		this.socket = socket;
		this.map = map;
		this.replicaMap = replicaMap;
	}
	
	public void cloningNew(JSONObject obj) {
		long rangeStart = Long.parseLong((String)obj.get("RangeStart"));
		long rangeEnd = Long.parseLong((String)obj.get("RangeEnd"));
		JSONArray array = new JSONArray();
		for (String key : map.keySet()) {
			if(Long.parseLong(key) >= rangeStart && Long.parseLong(key) <= rangeEnd) {
				JSONObject jsonObj = new JSONObject();
				jsonObj.put("key", key);
				jsonObj.put("value", map.get(key));
				array.add(jsonObj);
			}
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("key", key);
			jsonObj.put("value", map.get(key));
			array.add(jsonObj);

		}
		JSONObject responseObj = new JSONObject();
		responseObj.put("msgType", "CloningNew");
		responseObj.put("Data", array);
		DataOutputStream outStream;
		try {
			outStream = new DataOutputStream(socket.getOutputStream());
			outStream.writeUTF(responseObj.toString());
			outStream.flush();
			outStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}
	public void run() {

		System.out.println("Processing requests");
		System.out.println("Client accepted");
		try {
			DataInputStream in = new DataInputStream( new BufferedInputStream(socket.getInputStream())); 

			String line = in.readUTF(); 
			//line = in.readLine();
			System.out.println(line); 
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject)parser.parse(line);
			System.out.println(obj.get("msgType"));
			String messageType = (String)obj.get("msgType");
			if(messageType.equals("CloningNew")) {
				
				cloningNew(obj);

			}

			in.close();
			socket.close();

		}
		catch(Exception e) {

		}
		finally {
			if(socket.isClosed() == false)
				try {
					socket.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}


	}



}
