package test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.*;
import java.util.concurrent.locks.Lock;

import com.google.gson.*;


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
public class cordinatorserver implements Runnable 
{
	Socket csocket;
	Socket persocket;

	static ArrayList<slaveinfo> listofslaves = new ArrayList<slaveinfo>();
	cordinatorserver(Socket csocket) {
		this.csocket = csocket;
	}
	public static void main(String args[]) throws Exception { 
		ServerSocket ssock = new ServerSocket(9999);
		System.out.println("Listening");

		while (true) {
			Socket sock = ssock.accept();
			System.out.println("Connected");
			new Thread(new cordinatorserver(sock)).start();
		}
	}
	public void run() {
		try {
			DataInputStream ois = new DataInputStream(csocket.getInputStream());
			String message = (String) ois.readUTF();
			System.out.println(message);
			Gson g = new Gson(); 
			requesttype p = g.fromJson(message, requesttype.class);
			System.out.println(p.msgType);
			if(p.msgType.equals("Register"))
			{
				register(p.PORT,p.IP);
			}
			csocket.close();
		} catch (Exception e) {
			System.out.println(e);
		}
	}
	synchronized public void register(String port, String ip)
	{
		String tobehashed = port + ip;
		long h = 1125899906842597L;
		int len = tobehashed.length();

		for (int i = 0; i < len; i++) {
			h = (31 * h) + tobehashed.charAt(i);
		}
		if(h<0) { h = -h; };
		System.out.println(Long.toString(h));
		slaveinfo slavobj = new slaveinfo(ip,port,Long.toString(h));
		



		listofslaves.add(slavobj);
		Collections.sort(listofslaves, new CustomComparator());
		sendresponse(port,ip,slavobj.id);

	}
	public void sendresponse(String port, String ip , String id )
	{
		try {

			DataOutputStream oos = null;
			oos = new DataOutputStream(csocket.getOutputStream());
			System.out.println(id);
			oos.writeUTF(id);
			csocket.close();
		}
		catch(Exception e)
		{
			System.out.println(e);
		}


	}
}
