package test;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;

import org.json.simple.JSONObject;
public class TestSlave {
	public static long hashTo64bit(String string) {
        long h = 1125899906842597L;
        int len = string.length();

        for (int i = 0; i < len; i++) {
            h = (31 * h) + string.charAt(i);
        }
        return Math.abs(h);
    }
	public static void main(String []args) {
		try {
			
			Socket socket = new Socket("127.0.0.1", 5651);
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
