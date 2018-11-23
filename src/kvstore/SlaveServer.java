package kvstore;
import java.io.IOException;
import java.net.*;
import java.util.*;

import org.json.simple.JSONObject;
public class SlaveServer {
	
	private Socket socket;
	private String Ip;
	private int Port;
	
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

	public void register() throws Exception {
		try {
			Socket socket = new Socket(Ip, Port);
			socket.setReuseAddress(true);
			JSONObject obj = new JSONObject();
		}
	
		catch(Exception e) {
			System.out.println("Exception:" + e.getMessage() );
		}
		
		
	}
	
	

}
