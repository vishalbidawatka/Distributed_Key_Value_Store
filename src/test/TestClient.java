package test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.simple.JSONObject;

public class TestClient {
	public static void main(String []args) {
		Socket socket;
		Socket socket1;
		try {
//			socket1 = new Socket("127.0.0.1", 5883);
//			DataOutputStream op1 = new DataOutputStream(socket1.getOutputStream());
//			JSONObject obj1 = new JSONObject();
//			obj1.put("msgType", "GET");
//			long h1 = TestSlave.hash("foo1");
//			obj1.put("Key", Long.toString(h1));
//			obj1.put("ServerType", "Original");
//
//			op1.writeUTF(obj1.toString());
//			DataInputStream ip1 = new DataInputStream(socket1.getInputStream());
//			System.out.println(ip1.readUTF().toString());
//			
//			op1.close();
//			socket1.close();
			
			socket = new Socket("10.1.37.211", 1234);
			DataOutputStream op = new DataOutputStream(socket.getOutputStream());
			JSONObject obj = new JSONObject();
			obj.put("msgType", "PUT");
			//long h = TestSlave.hash("foo1");
			//obj.put("Key", Long.toString(h));
			obj.put("Key", "foo2");
			obj.put("Value", "bar1");
			obj.put("ServerType", "Original");

			op.writeUTF(obj.toString());
			DataInputStream ip = new DataInputStream(socket.getInputStream());
			System.out.println(ip.readUTF().toString());
			obj = new JSONObject();
			obj.put("msgType", "Commit");
			op.writeUTF(obj.toString());
			System.out.println(ip.readUTF().toString());

			op.close();
			socket.close();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
