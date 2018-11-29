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
//		Socket socket1;
		try {
			socket = new Socket("10.1.37.17", 1234);
			DataOutputStream op = new DataOutputStream(socket.getOutputStream());
			JSONObject obj = new JSONObject();
			
//			obj.put("msgType", "GET");
//			obj.put("Key", "GHI");
//			op.writeUTF(obj.toString());
//			DataInputStream ip = new DataInputStream(socket.getInputStream());
//			System.out.println(ip.readUTF().toString());

			obj.put("msgType", "DEL");
			obj.put("Key", "ABC");
			op.writeUTF(obj.toString());
			DataInputStream ip = new DataInputStream(socket.getInputStream());
			System.out.println(ip.readUTF().toString());
			
//			obj.put("msgType", "PUT");
//			obj.put("Key", "GHI");
//			obj.put("Value", "Bhavin");
//			op.writeUTF(obj.toString());
//			DataInputStream ip = new DataInputStream(socket.getInputStream());
//			System.out.println(ip.readUTF().toString());
//			obj = new JSONObject();
//			obj.put("msgType", "Commit");
//			op.writeUTF(obj.toString());

			op.close();
			socket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
