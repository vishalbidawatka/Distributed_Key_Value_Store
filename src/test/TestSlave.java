package test;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.json.simple.JSONObject;
public class TestSlave {
    
    public static long hash(String key) {
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
        return h;
    }
   
	public static void main(String []args) {
		try {
			System.out.println(hash("foo1"));
			System.out.println(hash(Integer.toString(8081)));
			System.out.println(hash(Integer.toString(8082)));

			Socket socket = new Socket("127.0.0.1", 5180);
			DataOutputStream op = new DataOutputStream(socket.getOutputStream());
			JSONObject obj = new JSONObject();
			obj.put("msgType", "CloningNew");
			obj.put("RangeStart", "10");
			obj.put("RangeEnd", "20");


			obj.put("IP", "191.168.1.1");
			op.writeUTF(obj.toString());
			DataInputStream ip = new DataInputStream(socket.getInputStream());
			System.out.println(ip.readUTF().toString());
			op.writeUTF("Over");
			op.close();
			socket.close();
			//
			 
			 
			//System.out.println(hashTo64bit("kotak"));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}

}
