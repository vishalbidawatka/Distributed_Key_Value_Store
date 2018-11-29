package test;
import java.util.Random;
import java.util.Scanner;

import kvstore.*;
public class SlaveServerTest {
	public static void main(String []args) {
		SlaveServer slave = new SlaveServer();
		slave.setIp("10.1.37.168");
		Random random = new Random();
//		slave.setPort(8081);
		int port = Math.abs(random.nextInt() % 1000);
//		slave.setPort(5000 + port);
		slave.setPort(5281);
		System.out.println(5281);
		try {
			
			slave.start();			
			
			slave.register("10.1.37.17", 9999);
			//slave.show();
			//Thread.sleep(20000);
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
			System.out.println(e.getMessage());
		}
	}

}
