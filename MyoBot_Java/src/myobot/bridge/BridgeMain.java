package myobot.bridge;

import myobot.bridge.myo.EulerOrientation;
import myobot.bridge.myo.Myo;
import myobot.bridge.myo.MyoException;

public class BridgeMain {

	public static void main(String[] args) {
		
		try {
			Myo myo = new Myo();
			myo.init();
			myo.startHubThread(100);
			for(int i = 0; i < 100; i ++) {
				EulerOrientation o = myo.getOrientation();
				System.out.println(o.getYaw() + ", " + o.getPitch() + ", " + o.getRoll());
				Thread.sleep(100);
			}
			myo.cleanup();
		}
		catch(MyoException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

}
