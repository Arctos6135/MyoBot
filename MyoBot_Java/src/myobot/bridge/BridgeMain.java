package myobot.bridge;

import myobot.bridge.myo.Myo;
import myobot.bridge.myo.MyoException;

public class BridgeMain {

	public static void main(String[] args) {
		
		try {
			Myo.init();
		}
		catch(MyoException e) {
			System.out.println("Failed to initialize Myo connection.");
			System.exit(0);
		}
	}

}
