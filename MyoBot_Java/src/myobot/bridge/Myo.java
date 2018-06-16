package myobot.bridge;

public class Myo {
	static {
		System.loadLibrary("myobot_jni_64");
	}
	
	public static native void hello();
}
