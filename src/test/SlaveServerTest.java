package test;
import kvstore.*;
public class SlaveServerTest {
	public static void main(String []args) {
		SlaveServer slave = new SlaveServer();
		slave.setIp("127.0.0.1");
		slave.setPort(8081);
		try {
			slave.start();
			//slave.register("10.1.37.157", 9999);
			slave.show();
			//Thread.sleep(20000);
			//slave.stop();

		}
		catch(Exception e) {
			System.out.println(e.getMessage());
		}
	}

}
