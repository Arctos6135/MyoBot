package myobot.bridge.myo;

public class Myo {
	static {
		System.loadLibrary("myobot_jni_64");
	}
	
	private static boolean initialized = false;
	public static boolean isInitialized() {
		return initialized;
	}
	private static native void __initialize();
	public static void init() {
		__initialize();
		if(!initialized) {
			throw new MyoException("Failed to initialize Myo connection");
		}
	}
	
	private static native void __lock();
	public static void lock() {
		if(!initialized) {
			throw new MyoException("Connection not initialized");
		}
		__lock();
	}
	
	private static native void __unlock();
	public static void unlock() {
		if(!initialized) {
			throw new MyoException("Connection not initialized");
		}
		__unlock();
	}
	
	private static native void __updateRef();
	public static void updateReferenceOrientation() {
		if(!initialized) {
			throw new MyoException("Connection not initialized");
		}
		__updateRef();
	}
	
	private static double result_yaw, result_pitch, result_roll;
	private static native void __getOrientation();
	public static EulerOrientation getOrientation() {
		if(!initialized) {
			throw new MyoException("Connection not initialized");
		}
		__getOrientation();
		return new EulerOrientation(result_yaw, result_pitch, result_roll);
	}
}
