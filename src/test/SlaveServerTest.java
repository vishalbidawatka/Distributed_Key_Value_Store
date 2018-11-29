package test;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.Scanner;

import kvstore.*;
public class SlaveServerTest {
	public static void main(String []args) {
		SlaveServer slave = new SlaveServer();
		String IP="localhost";
		try {
			IP = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		slave.setIp(IP);
		Random random = new Random();
		int port = Math.abs(random.nextInt() % 1000);
//		slave.setPort(5000 + port);
		slave.setPort(15000);
		System.out.println(15000);
		try {
			
			slave.start();			
			
			slave.register("10.1.37.17", 9999);
			while(true) {
				Scanner sc = new Scanner(System.in);
				String req = sc.next();
				if (req.equalsIgnoreCase("exit")) {
					slave.stop("10.1.37.17", 9999);
					break;
				}
				else
					continue;
			}
//			System.out.println("RENTERED");
		}
		catch(Exception e) {
			//System.out.println(e.getMessage());
		}
	}

}
