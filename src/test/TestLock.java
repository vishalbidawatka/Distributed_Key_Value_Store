package test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;

public class TestLock {
	public static int j = 0;
	public static void main(String []args) {
		try {
			ConcurrentHashMap<Integer,Integer> map = new ConcurrentHashMap<Integer, Integer>();
			for(int i = 0; i < 10; i++) {
				Thread t = new Thread() {
					@Override
					public void run() {
						super.run();	
						if(j%2 == 0) {
							map.put(j, j);
							System.out.println(j + " : "+map.get(j));
						}else
							System.out.println(map.get(j-1));
					}
				};
				t.start();
				j=i;
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
