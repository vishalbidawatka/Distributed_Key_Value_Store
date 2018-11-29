package kvstore;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.*;
import java.util.concurrent.locks.Lock;

import javax.sql.rowset.spi.SyncResolver;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



class slaveinfo
{
	String ip;
	String port;
	String id;
	slaveinfo(String i , String p , String id)
	{
		this.ip = i;
		this.port = p;
		this.id = id;
	}


}
class CustomComparator implements Comparator<slaveinfo> {
	@Override
	public int compare(slaveinfo o1, slaveinfo o2) {
		return o1.id.compareTo(o2.id);
	}
}
class requesttype
{
	String msgType;
	String IP;
	String PORT;
}
class messagetofill
{
	String msgType;
	String RangeStart;
	String RangeEnd;

}
class hashing
{
	String genratehash(String key)
	{
		MessageDigest instance = null;
		try {
			instance = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		instance.reset();
		instance.update(key.getBytes());
		byte[] digest = instance.digest();

		long h = 0;
		for (int i = 0; i < 4; i++) {
			h <<= 8;
			h |= ((int) digest[i]) & 0xFF;
		}
		return Long.toString(h);
	}

}
public class cordinatorserver implements Runnable 
{
	Socket csocket;
	Socket persocket;

	public static KVCache cache = new KVCache(1, 1);

	public static ServerSocket clisocket ;
	public static HashMap<String, Integer> listoflockedslaves = new  HashMap<String, Integer>();
	static ArrayList<slaveinfo> listofslaves = new ArrayList<slaveinfo>();
	static ArrayList<String> listofslavesids = new ArrayList<String>();
	cordinatorserver(Socket csocket) {
		this.csocket = csocket;
	}
	public static void main(String args[]) throws Exception { 

		clisocket = new ServerSocket(1234);
		ServerSocket ssock = new ServerSocket(9999);
		Socket tempsocket = null;
		System.out.println("Listening");
		new Thread(new ProcessingRequest( tempsocket)).start();
		while (true) {
			Socket sock = ssock.accept();
			System.out.println("Connected");
			new Thread(new cordinatorserver(sock)).start();

		}
	}

	public static ArrayList<slaveinfo> getListofslaves() {
		return listofslaves;
	}

	public void run() {
		try {
			
			 
			JSONObject p = getjsonfromstring(csocket);
			System.out.println(p);
			if(p.get("msgType").equals("Register"))
			{	
				register(Long.toString( (Long)p.get("PORT")),(String)p.get("IP"));
				//register(p.PORT,p.IP);
			}
			if(p.get("msgType").equals("EXIT"))
			{
				gracefuldeletion(Long.toString( (Long)p.get("PORT")),(String)p.get("IP"), p);
			}
			csocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void gracefuldeletion(String port, String ip , JSONObject p) {
		// TODO Auto-generated method stub
		synchronized (listofslaves) 
		{
			
			String tobehashed = port + ip;
			String h = (new hashing().genratehash(tobehashed));
			slaveinfo slavobj = new slaveinfo(ip,port,h);
			for(int i  = 0 ; i < listofslaves.size() ; i++)
			{
				if(listofslaves.get(i).id.equals(h))
				{

					try {
						System.out.println("calling gracefull deletion");
						deregister(slavobj , i , p);
						listofslaves.remove(i);
						Collections.sort(listofslaves, new CustomComparator());
						return;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}

			
			
		}
		
		
	}
	private void deregister(slaveinfo slavobj, int index , JSONObject p) throws Exception {
		
		
		// TODO Auto-generated method stub

		if(listofslaves.size() > 2)
		{	
			System.out.println("index of current " + index);
			int successorindex = index+1;
			int succofsucc = (successorindex+1)%listofslaves.size();
							

			if(successorindex >= listofslaves.size())
			{
				successorindex = 0;
			}
			int preindex = index-1;
			if(preindex < 0)
			{
				preindex = listofslaves.size()-1;
			}

			listoflockedslaves.put(listofslaves.get(index).id , 1);
			System.out.println("index of current " + successorindex);
			System.out.println("index of current " + preindex);
//			JSONObject merg1 = recievereplicalist2(index,successorindex , preindex,csocket);
//			JSONObject merg2 = recieveoriginal2(successorindex ,index, preindex,csocket);
			
			JSONObject merg3  = new JSONObject();
			JSONObject merg4  = new JSONObject();
			System.out.println("Merge 1 :"+p.toJSONString());
			
			
			
			
			
			
			
			
			merg3.put("msgType", "CopyData");
			merg3.put("CopyReplicaData", p.get("ReplicaData"));
			merg3.put("CopyOriginalData", p.get("OriginalData"));
			Socket s = new Socket(listofslaves.get(successorindex).ip,Integer.parseInt(listofslaves.get(successorindex).port));
			Socket s1 = new Socket(listofslaves.get(succofsucc).ip,Integer.parseInt(listofslaves.get(succofsucc).port));
			merg4.put("msgType","CopyReplica");
			merg4.put("CopyReplicaData",  p.get("OriginalData"));
			sendjsonstring(s1, merg4);
			sendjsonstring(s, merg3);
			JSONObject obj2 = new JSONObject();
			obj2.put("msgType", "ExitAck");
			sendjsonstring(csocket, obj2);
			listoflockedslaves.remove(listofslaves.get(index).id);
		}

		
	}
	public void printcurrentarraylist()
	{
		System.out.println("Current Arraylist contents");

		for(int i = 0 ; i  < listofslaves.size() ; i++)
		{
			System.out.println(listofslaves.get(i).id);
		}
	}

	public void register(String port, String ip )
	{
		synchronized(listofslaves)
		{
			String tobehashed = port + ip;
			String h = (new hashing().genratehash(tobehashed));
			System.out.println(h);
			slaveinfo slavobj = new slaveinfo(ip,port,h);
			for(int i  = 0 ; i < listofslaves.size() ; i++)
			{
				if(listofslaves.get(i).id.equals(h))
				{

					try {
						System.out.println("calling reregister");
						reregister(slavobj , i);
						return;
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
			}
		
			listofslaves.add(slavobj);
			Collections.sort(listofslaves, new CustomComparator());
			int index = listofslaves.indexOf(slavobj);
			sendresponse(port,ip,slavobj.id);
			printcurrentarraylist();
			copyandfillofnewserver(slavobj,index);
			//recieveackfromcurrent(port,ip,slavobj.id)
			//sendacktothesuccessor(port,ip,slavobj.id);
			try {
				csocket.close();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}


	}
	private void reregister(slaveinfo slavobj ,  int index) throws Exception 
	{
		// TODO Auto-generated method stub /////////////////////////////////////////////////////

		JSONObject obj = new JSONObject();
		obj.put("Complete", "RE");

		sendjsonstring(csocket, obj);
		if(listofslaves.size() > 2)
		{	
			System.out.println("index of current " + index);
			int successorindex = index+1;


			if(successorindex >= listofslaves.size())
			{
				successorindex = 0;
			}
			int preindex = index-1;
			if(preindex < 0)
			{
				preindex = listofslaves.size()-1;
			}

			listoflockedslaves.put(listofslaves.get(index).id , 1);
			System.out.println("index of current " + successorindex);
			System.out.println("index of current " + preindex);
			JSONObject merg1 = recievereplicalist(successorindex , preindex,index);
			JSONObject merg2 = recieveoriginal(successorindex , preindex,index);
			JSONObject merg3  = new JSONObject();
			System.out.println("Merge 1 :"+merg1.toJSONString());
			System.out.println("Merge 2 :"+merg2.toJSONString());
			merg3.put("msgType", "ReregisterData");
			merg3.put("OriginalData", merg1.get("ReplicaData"));
			merg3.put("ReplicaData", merg2.get("OriginalData"));
			sendjsonstring(csocket, merg3);
			listoflockedslaves.remove(listofslaves.get(index).id);
		}


	}
	private JSONObject recievereplicalist(int successorindex, int preindex, int index) throws Exception{
		// TODO Auto-generated method stub
		Socket socket = new Socket(listofslaves.get(successorindex).ip,Integer.parseInt(listofslaves.get(successorindex).port));
		JSONObject obj2 = new JSONObject();
		obj2.put("msgType", "GetReplica");
		sendjsonstring(socket, obj2);
		JSONObject obj3 = getjsonfromstring(socket);
		socket.close();
		return obj3;

	}

	private JSONObject recieveoriginal(int successorindex, int preindex, int index) throws Exception{
		// TODO Auto-generated method stub
		Socket socket = new Socket(listofslaves.get(preindex).ip,Integer.parseInt(listofslaves.get(preindex).port));
		JSONObject obj2 = new JSONObject();
		obj2.put("msgType", "GetOriginal");
		sendjsonstring(socket, obj2);
		JSONObject obj3 = getjsonfromstring(socket);
		socket.close();
		return obj3;

	}

	private void recieveackfromcurrent(int index, int succindex) 
	{
		// TODO Auto-generated method stub
		try {

			//			String ip = listofslaves.get(index).ip;
			//			String port = listofslaves.get(index).port;
			//			System.out.println("port of the new slave" + port);
			//			System.out.println("ip of the new slave" +  ip);
			//			Socket socket = new Socket(ip,Integer.parseInt(port));
			DataInputStream iis = new DataInputStream(csocket.getInputStream());
			String mesgrecieved = iis.readUTF();
			System.out.println("Message recived from new slave" + mesgrecieved);
			JSONParser parser = new JSONParser();
			JSONObject obj = (JSONObject)parser.parse(mesgrecieved);
			System.out.println(obj.get("msgType"));
			if(obj.get("msgType").equals("CloneAck") && obj.get("Status").equals("Success"))
			{
				sendacktothesuccessor(succindex , "CloneCommit");
			}
			else
			{
				sendacktothesuccessor(succindex , "CLoneAbort");
			}
		} catch (NumberFormatException | IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}




	}
	private void sendacktothesuccessor( int index , String message) {

		// TODO Auto-generated method stub


		Socket socket;
		try {
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("msgType", message);
			socket = new Socket(listofslaves.get(index).ip,Integer.parseInt(listofslaves.get(index).port));
			DataOutputStream oos = null;
			oos = new DataOutputStream(socket.getOutputStream());
			String jsonString = jsonObj.toJSONString();
			System.out.println(jsonString);
			oos.writeUTF(jsonString);
			DataInputStream iis = new DataInputStream(socket.getInputStream());
			String recievedmsg =iis.readUTF();
			System.out.println("String recieved at the end "+recievedmsg);
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}






	}
	public void copyandfillofnewserver(slaveinfo slavobj,int index)
	{
		if(listofslaves.size() > 2)
		{	
			System.out.println("index of current " + index);
			int successorindex = index+1;


			if(successorindex >= listofslaves.size())
			{
				successorindex = 0;
			}
			int preindex = index-1;
			if(preindex < 0)
			{
				preindex = listofslaves.size()-1;
			}
			listoflockedslaves.put(listofslaves.get(successorindex).id , 1);
			listoflockedslaves.put(listofslaves.get(index).id , 1);
			System.out.println("index of current " + successorindex);
			System.out.println("index of current " + preindex);
			recievedatafromsucc(successorindex , preindex,index);
			listoflockedslaves.remove(listofslaves.get(successorindex).id);
			listoflockedslaves.remove(listofslaves.get(index).id);
		}

	}

	public void recievedatafromsucc(int successorindex , int preindex, int index) {
		// TODO Auto-generated method stub
		slaveinfo succ = listofslaves.get(successorindex);
		JSONObject obj = new JSONObject();
		obj.put("msgType","CloningNew" );
		obj.put("RangeStart", (String)listofslaves.get(preindex).id);
		obj.put("RangeEnd", (String)listofslaves.get(preindex).id);
		String jsonString = obj.toJSONString();
//		messagetofill messobj = new messagetofill();
//		messobj.msgType = "CloningNew";
//		messobj.RangeStart =(listofslaves.get(preindex).id);
//		messobj.RangeEnd = (listofslaves.get(index).id);
//		
//		Gson g = new Gson();
//		String jsonString = g.toJson(messobj);
		System.out.println("Json sent :"+jsonString);
		sendmessangetoclient(succ.ip,succ.port,jsonString , successorindex , index );


	}

	private void sendmessangetoclient(String ip, String port, String jsonString , int succindex , int index) {
		// TODO Auto-generated method stub
		try {
			System.out.println("phase 1");
			System.out.println(Integer.parseInt(port));
			int port1 = Integer.parseInt(port);
			Socket socket = new Socket(ip,Integer.parseInt(port));
			System.out.println("phase 2");
			DataOutputStream oos = null;
			System.out.println("phase 3");
			oos = new DataOutputStream(socket.getOutputStream());
			System.out.println("phase 4");
			System.out.println(jsonString);
			System.out.println("phase 5");
			oos.writeUTF(jsonString);
			System.out.println("phase 6");
			DataInputStream iis = new DataInputStream(socket.getInputStream());
			System.out.println("phase 7");
			String mesgrecieved = iis.readUTF();
			System.out.println("phase 8");
			System.out.println("message recieved : "+mesgrecieved);
			socket.close();
			String newmessage = mesgrecieved.replace("CloningNew", "FillNew");

			DataOutputStream oos1 = null;
			oos1 = new DataOutputStream(csocket.getOutputStream());

			oos1.writeUTF(newmessage);
			System.out.println("message sent: " + newmessage);
			recieveackfromcurrent(index, succindex  );

		}
		catch(Exception e)
		{
			System.out.println(e);
		}
	}
	public void sendresponse(String port, String ip , String id )
	{
		try {

			JSONObject obj = new JSONObject();
			obj.put("ID", id);
			if(listofslaves.size()>2)
			{
				obj.put("Complete","NO");
			}
			else
			{
				obj.put("Complete", "YES");
			}
			String op = obj.toJSONString();
			DataOutputStream oos = null;
			oos = new DataOutputStream(csocket.getOutputStream());
			System.out.println(op);
			oos.writeUTF(op);

		}
		catch(Exception e)
		{
			System.out.println(e);
		}
	}

	JSONObject getjsonfromstring(Socket clientsocket) throws Exception
	{
		DataInputStream ois = new DataInputStream(clientsocket.getInputStream());
		String message = (String) ois.readUTF();
		System.out.println("message recieved from other side" + message);
		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject)parser.parse(message);
		return obj;
	}

	String sendjsonstring(Socket clientsocket , JSONObject obj) throws Exception
	{	
		String s = obj.toJSONString();
		DataOutputStream ois = new DataOutputStream(clientsocket.getOutputStream());
		ois.writeUTF(s);
		System.out.println("message send to other side" + s);
		return s;
	}



}

//client side processing

class ProcessingRequest implements Runnable 
{	

	Socket clientsocket;
	public ProcessingRequest(Socket ssock)
	{

		this.clientsocket = ssock;
	}


	public void run() {
		try {

			while(true)
			{
				System.out.println("Listening to clients from process");
				Socket sock = cordinatorserver.clisocket.accept();
				System.out.println("Connected");
				new Thread(new Processingclients(sock)).start();
				//printcurrentarraylist();
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
class Processingclients implements Runnable
{
	Socket clientsocket;
	public Processingclients(Socket ssock)
	{

		this.clientsocket = ssock;
	}
	public void printcurrentarraylist()
	{
		System.out.println("current arraylist from clients");
		kvstore.cordinatorserver newobj = new kvstore.cordinatorserver(clientsocket);
		ArrayList<slaveinfo> listofslaves2 = cordinatorserver.getListofslaves();
		for(int i = 0 ; i  < listofslaves2.size() ; i++)
		{
			System.out.println(listofslaves2.get(i).id);
		}
	}
	JSONObject getjsonfromstring(Socket clientsocket) throws Exception
	{
		DataInputStream ois = new DataInputStream(clientsocket.getInputStream());
		String message = (String) ois.readUTF();
		System.out.println("message recieved from other side" + message);
		JSONParser parser = new JSONParser();
		JSONObject obj = (JSONObject)parser.parse(message);
		return obj;
	}

	String sendjsonstring(Socket clientsocket , JSONObject obj) throws Exception
	{	
		String s = obj.toJSONString();
		DataOutputStream ois = new DataOutputStream(clientsocket.getOutputStream());
		ois.writeUTF(s);
		System.out.println("message send to other side" + s);
		return s;
	}

	String genratehash(String key)
	{
		MessageDigest instance = null;
		try {
			instance = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		instance.reset();
		instance.update(key.getBytes());
		byte[] digest = instance.digest();

		long h = 0;
		for (int i = 0; i < 4; i++) {
			h <<= 8;
			h |= ((int) digest[i]) & 0xFF;
		}
		return Long.toString(h);
	}
	public void run() {
		try {

			printcurrentarraylist();
			JSONObject obj = getjsonfromstring(clientsocket);
			if(obj.get("msgType").equals("PUT"))
			{
				handleput(obj);
			}
			if(obj.get("msgType").equals("GET"))
			{
				handleget(obj);
			}
			if(obj.get("msgType").equals("DEL"))
			{
				handledel(obj);
			}





		
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	
	private void handledel(JSONObject obj) {
		// TODO Auto-generated method stub
		String hashofkey = genratehash((String) obj.get("Key"));
		slaveinfo tempinfo = new slaveinfo("0.0.0.0", "0000", hashofkey);
		int upperindex = Math.abs(Collections.binarySearch(kvstore.cordinatorserver.listofslaves, tempinfo ,   new CustomComparator() ) ) - 1 ;
		int originalindex = upperindex % kvstore.cordinatorserver.listofslaves.size();
		int replicateindex = (upperindex + 1) % kvstore.cordinatorserver.listofslaves.size();
		while(kvstore.cordinatorserver.listoflockedslaves.containsKey(kvstore.cordinatorserver.listofslaves.get(originalindex).id)) {};
		while(kvstore.cordinatorserver.listoflockedslaves.containsKey(kvstore.cordinatorserver.listofslaves.get(replicateindex).id)) {};
		JSONObject obj2 = new JSONObject();
		obj2.put("msgType", "DEL");
		obj2.put("Key", hashofkey);
		obj2.put("Value", obj.get("Value"));
		obj2.put("ServerType", "Original");
		JSONObject obj1 = new JSONObject();
		obj1.put("msgType", "DEL");
		obj1.put("Key", hashofkey);
		obj1.put("Value", obj.get("Value"));
		obj1.put("ServerType", "Replica");

		int a1 = tpccommunicationdel(kvstore.cordinatorserver.listofslaves.get(originalindex),kvstore.cordinatorserver.listofslaves.get(replicateindex) ,obj2 , obj1);
		//int a2 = tpccommunication(kvstore.cordinatorserver.listofslaves.get(replicateindex), obj1);
		JSONObject objmsg  = new JSONObject();
		if(a1 == 1 )
		{
			kvstore.cordinatorserver.cache.del((String)obj.get("Key"));

			objmsg.put("msgType","DEL");
			objmsg.put("Status","Sucess");
			objmsg.put("Value", "");
			objmsg.put("Key", obj.get("Key"));

		}
		else
		{	
			objmsg.put("msgType","DEL");
			objmsg.put("Status","Failure");
			objmsg.put("Value", "");
			objmsg.put("Key", obj.get("Key"));


		}
		try {
			sendjsonstring(clientsocket, objmsg);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
	private int tpccommunicationdel(slaveinfo slaveinfo, slaveinfo slaveinfo2, JSONObject obj, JSONObject obj2) {
		// TODO Auto-generated method stub
		Socket s1;
		try {
			s1 = new Socket(slaveinfo.ip, Integer.parseInt(slaveinfo.port));
			sendjsonstring(s1, obj);
		}
		catch(Exception e)
		{	
			System.out.println("original server down");
			return 0;
		}
		Socket s2;
		try {
			s2  = new Socket(slaveinfo2.ip, Integer.parseInt(slaveinfo2.port));
			sendjsonstring(s2, obj2);
		}
		catch(Exception e)
		{	
			System.out.println("Replica server down");
			JSONObject obj3;
			try {
				obj3 = getjsonfromstring(s1);
				if(obj3.get("msgType").equals("DelReady"))
				{
					obj.put("msgType", "DelAbort");
					sendjsonstring(s1, obj);
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			return 0;
		}
		try
		{
			JSONObject obj3 = getjsonfromstring(s1);
			if(obj3.get("msgType").equals("DelReady"))
			{
				obj.put("msgType", "DelCommit");
				sendjsonstring(s1, obj);
			}

		}
		catch(Exception e)
		{
			return 0;
		}
		try
		{
			JSONObject obj3 = getjsonfromstring(s2);
			if(obj3.get("msgType").equals("DelReady"))
			{
				obj.put("msgType", "DelCommit");
				sendjsonstring(s2, obj);
			}

		}
		catch(Exception e) 
		{
			return 0;
		}
		try
		{
			JSONObject ackobj = getjsonfromstring(s1);
			if(ackobj.get("msgType").equals("DelAck"))
			{
				JSONObject ackobj2 = getjsonfromstring(s2);
				if(ackobj2.get("msgType").equals("DelAck"))
				{
					return 1;
				}
				else
				{
					return 0;
				}
			}
			else
			{
				return 0;
			}


		}
		catch(Exception e)
		{
			System.out.println(e);
		}
		return 0;

		
	}
	private void handleget(JSONObject obj) 
	{
		// TODO Auto-generated method stub
		String key = (String) obj.get("Key");
		String hashofkey = genratehash(key);
		obj.put("Key", hashofkey);
		String cachresult = kvstore.cordinatorserver.cache.get(key);
		
		if(cachresult != null)
		{
			JSONObject obj2 = new JSONObject();
			obj2.put("msgType", "Key found in CS cache, Value is "+cachresult);
			try {
				sendjsonstring(clientsocket, obj2);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{	
			slaveinfo tempinfo = new slaveinfo("0.0.0.0", "0000", hashofkey);
			int upperindex = Math.abs(Collections.binarySearch(kvstore.cordinatorserver.listofslaves, tempinfo ,   new CustomComparator() ) ) - 1 ;
			int originalindex = upperindex % kvstore.cordinatorserver.listofslaves.size();
			int replicateindex = (upperindex + 1) % kvstore.cordinatorserver.listofslaves.size();
			JSONObject response = new JSONObject();
			Boolean f1 = kvstore.cordinatorserver.listoflockedslaves.containsKey(kvstore.cordinatorserver.listofslaves.get(originalindex).id);
			Boolean f2 = kvstore.cordinatorserver.listoflockedslaves.containsKey(kvstore.cordinatorserver.listofslaves.get(replicateindex).id);
			while ((f1 && f2) )
			{
				f1 = kvstore.cordinatorserver.listoflockedslaves.containsKey(kvstore.cordinatorserver.listofslaves.get(originalindex).id);
				f2 = kvstore.cordinatorserver.listoflockedslaves.containsKey(kvstore.cordinatorserver.listofslaves.get(replicateindex).id);
				
			}
			
			if(!f1)
			{	
				try {
				Socket s1 = new Socket(kvstore.cordinatorserver.listofslaves.get(originalindex).ip, Integer.parseInt(kvstore.cordinatorserver.listofslaves.get(originalindex).port));
				sendjsonstring(s1, obj);
				response = getjsonfromstring(s1);
				if(response.get("Status").equals("Failure"))
				{
					response.put("msgType","GET");
					response.put("Status","Failure");
					response.put("Value", "");
					
				}
				else
				{
					kvstore.cordinatorserver.cache.put(key, (String) response.get("Value"));
					response.put("msgType","GET");
					response.put("Status","Sucess");
					
					
				}
				
				
				try {
					sendjsonstring(clientsocket, response);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return ;
				
				}
				catch(Exception e)
				{
					System.out.println(e);
					
				}
			}
			if(!f2 )
			{

				try {
				Socket s2 = new Socket(kvstore.cordinatorserver.listofslaves.get(replicateindex).ip, Integer.parseInt(kvstore.cordinatorserver.listofslaves.get(replicateindex).port));
				sendjsonstring(s2, obj);
				response = getjsonfromstring(s2);
				if(response.get("Status").equals("Failure"))
				{
					response.put("msgType","GET");
					response.put("Status","Failure");
					response.put("Value", "");
				}
				else
				{
					kvstore.cordinatorserver.cache.put(key, (String) response.get("Value"));
					response.put("msgType","GET");
					response.put("Status","Sucess");
					
				}
				
				
				try {
					sendjsonstring(clientsocket, response);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				return ;
				}
				catch(Exception e)
				{
					System.out.println(e);
					
				}
			}
			
			
			
			
			
			
			
			
		}
		
		
		
	}
	private void handleput(JSONObject obj) {
		// TODO Auto-generated method stub

		String hashofkey = genratehash((String) obj.get("Key"));
		slaveinfo tempinfo = new slaveinfo("0.0.0.0", "0000", hashofkey);
		int upperindex = Math.abs(Collections.binarySearch(kvstore.cordinatorserver.listofslaves, tempinfo ,   new CustomComparator() ) ) - 1 ;
		int originalindex = upperindex % kvstore.cordinatorserver.listofslaves.size();
		int replicateindex = (upperindex + 1) % kvstore.cordinatorserver.listofslaves.size();
		while(kvstore.cordinatorserver.listoflockedslaves.containsKey(kvstore.cordinatorserver.listofslaves.get(originalindex).id)) {};
		while(kvstore.cordinatorserver.listoflockedslaves.containsKey(kvstore.cordinatorserver.listofslaves.get(replicateindex).id)) {};
		JSONObject obj2 = new JSONObject();
		obj2.put("msgType", "PUT");
		obj2.put("Key", hashofkey);
		obj2.put("Value", obj.get("Value"));
		obj2.put("ServerType", "Original");
		JSONObject obj1 = new JSONObject();
		obj1.put("msgType", "PUT");
		obj1.put("Key", hashofkey);
		obj1.put("Value", obj.get("Value"));
		obj1.put("ServerType", "Replica");

		int a1 = tpccommunication(kvstore.cordinatorserver.listofslaves.get(originalindex),kvstore.cordinatorserver.listofslaves.get(replicateindex) ,obj2 , obj1);
		//int a2 = tpccommunication(kvstore.cordinatorserver.listofslaves.get(replicateindex), obj1);
		JSONObject objmsg  = new JSONObject();
		if(a1 == 1 )
		{
			kvstore.cordinatorserver.cache.put((String)obj.get("Key"),(String)obj.get("Value"));

			objmsg.put("msgType","PUT");
			objmsg.put("Status","Sucess");
			objmsg.put("Value", "");
			objmsg.put("Key", obj.get("Key"));

		}
		else
		{	
			objmsg.put("msgType","PUT");
			objmsg.put("Status","Failure");
			objmsg.put("Value", "");
			objmsg.put("Key", obj.get("Key"));
			


		}
		try {
			sendjsonstring(clientsocket, objmsg);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


	}
	private int tpccommunication(slaveinfo slaveinfo, slaveinfo slaveinfo2,JSONObject obj, JSONObject obj2) {
		// TODO Auto-generated method stub
		Socket s1;
		try {
			s1 = new Socket(slaveinfo.ip, Integer.parseInt(slaveinfo.port));
			sendjsonstring(s1, obj);
		}
		catch(Exception e)
		{
			return 0;
		}
		Socket s2;
		try {
			s2  = new Socket(slaveinfo2.ip, Integer.parseInt(slaveinfo2.port));
			sendjsonstring(s2, obj2);
		}
		catch(Exception e)
		{	
			JSONObject obj3;
			try {
				obj3 = getjsonfromstring(s1);
				if(obj3.get("msgType").equals("PutReady"))
				{
					obj.put("msgType", "PutAbort");
					sendjsonstring(s1, obj);
				}
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			return 0;
		}
		try
		{
			JSONObject obj3 = getjsonfromstring(s1);
			if(obj3.get("msgType").equals("PutReady"))
			{
				obj.put("msgType", "PutCommit");
				sendjsonstring(s1, obj);
			}

		}
		catch(Exception e)
		{
			return 0;
		}
		try
		{
			JSONObject obj3 = getjsonfromstring(s2);
			if(obj3.get("msgType").equals("PutReady"))
			{
				obj.put("msgType", "PutCommit");
				sendjsonstring(s2, obj);
			}

		}
		catch(Exception e) 
		{
			return 0;
		}
		try
		{
			JSONObject ackobj = getjsonfromstring(s1);
			if(ackobj.get("msgType").equals("PutAck"))
			{
				JSONObject ackobj2 = getjsonfromstring(s2);
				if(ackobj2.get("msgType").equals("PutAck"))
				{
					return 1;
				}
				else
				{
					return 0;
				}
			}
			else
			{
				return 0;
			}


		}
		catch(Exception e)
		{
			System.out.println(e);
		}
		return 0;

		//		JSONObject obj2 = getjsonfromstring(s);
		//		if(obj2.get("msgType").equals("PutReady"))
		//		{
		//			obj.put("msgType", "PutCommit");
		//			sendjsonstring(s, obj);
		//		}
		//		JSONObject ackobj = getjsonfromstring(s);
		//		if(ackobj.get("msgType").equals("PutAck"))
		//		{
		//			return 1;
		//		}
		//		return 0;




	}

}