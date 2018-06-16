package myobot.bridge.myo;

public class Myo {
	static {
		System.loadLibrary("myobot_jni");
	}
	
	private boolean initialized = false;
	public boolean isInitialized() {
		return initialized;
	}
	private void checkInit() {
		if(!initialized) {
			throw new MyoException("Myo connection not established");
		}
	}
	
	private native boolean __initialize();
	private long _myoPointer = 0;
	private long _collectorPointer = 0;
	private long _hubPointer = 0;
	public void init() {
		initialized = __initialize();
		if(!initialized) {
			throw new MyoException("Failed to initialize Myo connection");
		}
	}
	
	private native void __runHub(int millis);
	private class HubThread extends Thread {
		
		int millis;
		volatile boolean exit = false;
		
		public HubThread(int millis) {
			this.millis = millis;
		}
		
		@Override
		public void run() {
			while(!exit && !interrupted()) {
				__runHub(millis);
			}
		}
	}
	private HubThread hubThread = null;
	public void startHubThread(int millis) {
		checkInit();
		hubThread = new HubThread(millis);
		hubThread.start();
	}
	public void stopHubThread() {
		if(hubThread != null) {
			hubThread.exit = true;
			hubThread.interrupt();
			hubThread = null;
		}
	}
	
	
	private native boolean __lock();
	public void lock() {
		checkInit();
		if(!__lock()) {
			throw new MyoException("Failed to lock Myo");
		}
	}
	
	private native boolean __unlock();
	public void unlock() {
		checkInit();
		if(!__unlock()) {
			throw new MyoException("Failed to unlock Myo");
		}
	}
	
	private native boolean __isLocked();
	public boolean isLocked() {
		checkInit();
		return __isLocked();
	}
	public boolean isUnlocked() {
		return !isLocked();
	}
	
	private native boolean __isOnArm();
	public boolean isOnArm() {
		checkInit();
		return __isOnArm();
	}
	
	public static final int ARM_LEFT = 0;
	public static final int ARM_RIGHT = 1;
	public static final int ARM_UNKNOWN = 2;
	private native int __getArm();
	public int getArm() {
		checkInit();
		return __getArm();
	}
	
	private native void __updateRef();
	public void updateReferenceOrientation() {
		checkInit();
		__updateRef();
	}
	
	private double result_yaw, result_pitch, result_roll;
	private native void __getOrientation();
	public EulerOrientation getOrientation() {
		checkInit();
		__getOrientation();
		return new EulerOrientation(result_yaw, result_pitch, result_roll);
	}
	
	public void cleanup() {
		if(initialized) {
			lock();
		}
		stopHubThread();
	}
}
