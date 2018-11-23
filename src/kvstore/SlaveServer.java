package kvstore;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.json.simple.JSONObject;

public class SlaveServer {
	
	private ServerSocket server;
	private Socket socket;
	private String Ip;
	private int Port;
	private RequestHandler requestHandler;
	
	public SlaveServer() {
		
	}
	
	public SlaveServer(String Ip, int Port) {
		super();
		this.Ip = Ip;
		this.Port = Port;
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
		
		requestHandler = new RequestHandler(server, this.Port);
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
            
			
			
		}
	
		catch(Exception e) {
			e.printStackTrace();
			//System.out.println("Exception:" + e.printStackTrace());
		}
		
		
	}
	
	

}
