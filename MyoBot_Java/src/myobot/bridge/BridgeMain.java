package myobot.bridge;

import myobot.bridge.myo.Myo;
import myobot.bridge.myo.MyoException;

public class BridgeMain {

	public static void main(String[] args) {
		
		try {
			Myo myo = new Myo();
			myo.init();
			myo.unlock();
			Thread.sleep(1000);
			myo.lock();
		}
		catch(MyoException e) {
			e.printStackTrace();
		} 
		catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
