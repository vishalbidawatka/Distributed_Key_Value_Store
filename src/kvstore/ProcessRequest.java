package kvstore;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ProcessRequest implements Runnable {

	Socket socket = null;
	private ServerData serverData;
	public ProcessRequest(Socket socket, ServerData serverData) {
		super();
		this.socket = socket;
		this.serverData = serverData;
	}
	
	public void cloningNew(JSONObject obj) {
		System.out.println("Inside clonining new method");
		try {
			long rangeStart = Long.parseLong((String)obj.get("RangeStart"));
			long rangeEnd = Long.parseLong((String)obj.get("RangeEnd"));
			JSONArray array = new JSONArray();
			for (String key : this.serverData.map.keySet()) {
				if(Long.parseLong(key) >= rangeStart && Long.parseLong(key) <= rangeEnd) {
					JSONObject jsonObj = new JSONObject();
					jsonObj.put("Key", key);
					jsonObj.put("Value", this.serverData.map.get(key));
					this.serverData.tempMap.put(key, this.serverData.map.get(key));
					array.add(jsonObj);
				}
			}

			JSONObject responseObj = new JSONObject();
			responseObj.put("msgType", "CloningNew");
			responseObj.put("Data", array);
			JSONArray replicaArray = new JSONArray();
			for(String key : this.serverData.replicaMap.keySet()) {
				JSONObject repObj = new JSONObject();
				repObj.put("Key", key);
				repObj.put("Value", this.serverData.replicaMap.get(key));
				this.serverData.tempReplicaMap.put(key, this.serverData.replicaMap.get(key));
				replicaArray.add(repObj);
			}

			responseObj.put("ReplicationData", replicaArray);
			DataOutputStream outStream;

			outStream = new DataOutputStream(socket.getOutputStream());
			outStream.writeUTF(responseObj.toString());
			System.out.println(responseObj.toString());
			System.out.println("Data sent");
			outStream.flush();
			outStream.close();
		} catch (IOException e) {
			System.out.println("Exception in sending data !");
			e.printStackTrace();
		}



	}

	public void cloneCommit() {
		this.serverData.map = this.serverData.tempMap;
		this.serverData.replicaMap = this.serverData.tempReplicaMap;
		this.serverData.tempMap = new ConcurrentHashMap<String,String>();
		this.serverData.tempReplicaMap = new ConcurrentHashMap<String,String>();
		DataOutputStream outStream;
		try {

			JSONObject responseObj = new JSONObject();
			responseObj.put("msgType", "CloneCommitAck");
			responseObj.put("Status", "Success");
			outStream = new DataOutputStream(socket.getOutputStream());
			outStream.writeUTF(responseObj.toString());
			System.out.println("Clone Ack Sent !");
			outStream.flush();
			outStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void cloneAbort() {
		this.serverData.tempMap = new ConcurrentHashMap<String,String>();
		this.serverData.tempReplicaMap = new ConcurrentHashMap<String,String>();
	}

	public void handleGet(JSONObject obj) {
		String key = (String)obj.get("Key");
		String value = null;

		this.serverData.readLock.lock();
		if(value == null && this.serverData.map.containsKey(key))
			value = this.serverData.map.get(key);
		else if(value == null && this.serverData.replicaMap.containsKey(key))
			value = this.serverData.replicaMap.get(key);
		DataOutputStream outStream = null;
		try {
			JSONObject responseObj = new JSONObject();
			responseObj.put("msgType", "GetReply");
			if(value != null) {
				responseObj.put("Status", "Success");
				responseObj.put("Value", value);
			}
			else {
				responseObj.put("Status", "Failure");
				responseObj.put("Value", "");
			}			

			outStream = new DataOutputStream(socket.getOutputStream());
			outStream.writeUTF(responseObj.toString());
			outStream.flush();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if(outStream != null ) {
				try {
					outStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.serverData.readLock.unlock();
		}
	}

	public void handlePut(JSONObject obj, DataInputStream in) {
		DataOutputStream outStream = null;
		String key = (String) obj.get("Key");
		String value = (String) obj.get("Value");
		String serverType = (String) obj.get("ServerType");

		this.serverData.writeLock.lock();		
		
		try {
			JSONObject responseObj = new JSONObject();
			responseObj.put("msgType", "PutReady");
			outStream = new DataOutputStream(socket.getOutputStream());
			outStream.writeUTF(responseObj.toString());
			System.out.println("PUT sending response: " + responseObj.toString());
			String response = in.readUTF();
			try {
				responseObj = (JSONObject)(new JSONParser().parse(response));
				String operation = (String)responseObj.get("msgType");
				if(operation.equals("PutCommit")) {
					if(serverType.equals("Original"))
						this.serverData.map.put(key, value);
					else if(serverType.equals("Replica"))
						this.serverData.replicaMap.put(key, value);
					responseObj = new JSONObject();
					responseObj.put("msgType", "PutAck");
					responseObj.put("Status", "Success");
					outStream.writeUTF(responseObj.toString());
					System.out.println("Sending: " +responseObj.toString());
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			outStream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if(outStream != null ) {
				try {
					outStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.serverData.writeLock.unlock();
		}
	}
	
	public void handleDel(JSONObject obj, DataInputStream in) {
		DataOutputStream outStream = null;
		String key = (String) obj.get("Key");
		String serverType = (String) obj.get("ServerType");

		this.serverData.writeLock.lock();
		try {
			JSONObject responseObj = new JSONObject();
			responseObj.put("msgType", "DelReady");
			outStream = new DataOutputStream(socket.getOutputStream());
			outStream.writeUTF(responseObj.toString());
			System.out.println("DEL sending response: " + responseObj.toString());
			String response = in.readUTF();
			try {
				responseObj = (JSONObject)(new JSONParser().parse(response));
				String operation = (String)responseObj.get("msgType");
				if(operation.equals("DelCommit")) {
					if(serverType.equals("Original"))
						this.serverData.map.remove(key);
					else if(serverType.equals("Replica"))
						this.serverData.replicaMap.remove(key);
					responseObj = new JSONObject();
					responseObj.put("msgType", "DelAck");
					responseObj.put("Status", "Success");
					outStream.writeUTF(responseObj.toString());
					System.out.println("Sending: " +responseObj.toString());
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			outStream.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if(outStream != null ) {
				try {
					outStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			this.serverData.writeLock.unlock();
		}
	}

/********************************************************************************************/
	public void getOriginal(){
		JSONArray array = new JSONArray();
		for (String key : this.serverData.map.keySet()) {
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("Key", key);
			jsonObj.put("Value", this.serverData.map.get(key));
			array.add(jsonObj);
		}
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("OriginalData", array);
		try {
			DataOutputStream outstream = new DataOutputStream(socket.getOutputStream());
			outstream.writeUTF(jsonResponse.toString());
			outstream.flush();
			outstream.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getReplica() {
		JSONArray array = new JSONArray();
		for (String key : this.serverData.replicaMap.keySet()) {
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("Key", key);
			jsonObj.put("Value", this.serverData.replicaMap.get(key));
			array.add(jsonObj);
		}
		JSONObject jsonResponse = new JSONObject();
		jsonResponse.put("ReplicaData", array);
		try {
			DataOutputStream outstream = new DataOutputStream(socket.getOutputStream());
			outstream.writeUTF(jsonResponse.toString());
			outstream.flush();
			outstream.close();
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void copyLists(JSONObject obj) {
		JSONArray firstMsg = (JSONArray)obj.get("CopyOriginalData");
		JSONArray secondMsg = (JSONArray)obj.get("CopyReplicaData");
		copyOriginal(firstMsg);
		copyReplica(secondMsg);
	}
	
	public void copyOriginal(JSONArray array) {
		for(int i = 0;i < array.size(); i++) {
			JSONObject dataObj = (JSONObject)array.get(i);
			String key = (String)dataObj.get("Key");
			String value = (String)dataObj.get("Value");
			this.serverData.map.put(key, value);
		} 
	}

	public void copyReplica(JSONArray array) {
		for(int i = 0;i < array.size(); i++) {
			JSONObject dataObj = (JSONObject)array.get(i);
			String key = (String)dataObj.get("Key");
			String value = (String)dataObj.get("Value");
			this.serverData.replicaMap.put(key, value);
		}
	}

/********************************************************************************************/
	public void run() {

		System.out.println("Processing requests");
		System.out.println("Client accepted");
		try {
			DataInputStream in = new DataInputStream( new BufferedInputStream(socket.getInputStream())); 

			String line = in.readUTF(); 
			//line = in.readLine();
			System.out.println("Request from Co-ordination server:");
			System.out.println(line); 
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject)parser.parse(line);
			System.out.println(obj.get("msgType"));
			String messageType = (String)obj.get("msgType");
			if(messageType.equals("CloningNew")) {				
				cloningNew(obj);
			}
			else if(messageType.equals("CloneCommit")) {
				cloneCommit();
			}
			else if(messageType.equals("CloneAbort")) {
				cloneAbort();
			}
			else if(messageType.equals("GET")) {
				handleGet(obj);
			}
			else if(messageType.equals("PUT")) {
				handlePut(obj, in);
			}
			else if(messageType.equals("DEL")) {
				handleDel(obj, in);
			}
			else if(messageType.equals("GetOriginal")) {
				getOriginal();
			}
			else if(messageType.equals("GetReplica")) {
				getReplica();
			}
			else if(messageType.equals("CopyData")) {
				copyLists(obj);
			}
			else if(messageType.equals("CopyReplica")) {
				JSONArray array = (JSONArray)obj.get("CopyReplicaData");
				copyReplica(array);
			}
			in.close();
			socket.close();
		}
		catch(Exception e) {
			e.printStackTrace();
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
