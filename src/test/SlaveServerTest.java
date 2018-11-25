package test;
import java.util.Random;

import kvstore.*;
public class SlaveServerTest {
	public static void main(String []args) {
		SlaveServer slave = new SlaveServer();
		slave.setIp("10.42.0.242");
		Random random = new Random();
		slave.setPort(8081);
		int port = Math.abs(random.nextInt() % 1000);
		slave.setPort(5000 + port);
		System.out.println(port);
		try {
			
			slave.start();			
			
			slave.register("10.1.37.211", 9999);
			//slave.show();
			//Thread.sleep(20000);
			//slave.stop();

		}
		catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}

}
