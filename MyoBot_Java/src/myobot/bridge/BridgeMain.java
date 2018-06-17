package myobot.bridge;

import myobot.bridge.myo.Myo;
import myobot.bridge.myo.MyoException;

public class BridgeMain {

	public static void main(String[] args) {
		
		Myo myo = new Myo();
		try {
			
		}
		catch(MyoException e) {
			e.printStackTrace();
		} 
		finally {
			if(myo.isInitialized()) {
				myo.cleanup();
			}
		}
	}

}
