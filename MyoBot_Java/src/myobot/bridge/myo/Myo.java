package myobot.bridge.myo;

/**
 * A JNI class for dealing with Myos.
 * @author Tyler Tian
 *
 */
public class Myo {
	static {
		System.loadLibrary("myobot_jni");
	}
	
	private boolean initialized = false;
	/**
	 * Checks if the Myo is initialized. Attempting to call any method besides {@link #init()} will cause a
	 * {@link myobot.bridge.myo.MyoException MyoException} to be thrown.
	 * @return Whether the Myo is initialized
	 * @see #init()
	 */
	public boolean isInitialized() {
		return initialized;
	}
	private void checkInit() {
		if(!initialized) {
			throw new MyoException("Myo connection not established");
		}
	}
	
	private native boolean __initialize();
	//Raw pointers to the Myo, Hub, and Data Collector
	private long _myoHandle = 0;
	private long _collectorHandle = 0;
	private long _hubHandle = 0;
	/**
	 * Initializes the Myo.
	 * @throws MyoException If the initialization fails
	 */
	public void init() {
		initialized = __initialize();
		if(!initialized) {
			throw new MyoException("Failed to initialize Myo connection");
		}
	}
	
	private native void __runHub(int millis);
	private native void __runHubOnce(int millis);
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
	/**
	 * Tests for whether the Hub thread is running.
	 * @return Whether the Hub running thread is running
	 * @see #startHubThread(int)
	 * @see #stopHubThread()
	 */
	public boolean isHubThreadRunning() {
		return !(hubThread == null);
	}
	/**
	 * Starts the Hub running thread. The Myo Hub needs to be run constantly to obtain data;
	 * this method starts a new thread that repeatedly runs the hub for the specified duration.
	 * Alternatively, you can also directly call the {@link #runHub(int)} method in a loop.
	 * This method will do nothing if the Hub thread is already started.
	 * @param millis The duration to run the Hub for, in milliseconds
	 * @throws MyoException If the Myo is not initialized
	 * @see #runHub(int)
	 * @see #stopHubThread()
	 */
	public void startHubThread(int millis) {
		if(!isHubThreadRunning()) {
			checkInit();
			hubThread = new HubThread(millis);
			hubThread.start();
		}
	}
	/**
	 * Stops the Hub running thread. This method will do nothing if the thread was already stopped.
	 * @see #startHubThread(int)
	 */
	public void stopHubThread() {
		if(hubThread != null) {
			hubThread.exit = true;
			hubThread.interrupt();
			hubThread = null;
		}
	}
	/**
	 * Runs the Myo Hub for the specified duration. The Myo Hub needs to be run constantly to obtain data;
	 * so if the Hub thread is not running via {@link #startHubThread(int)}, this method must be called repeatedly in a loop.
	 * @param millis The amount of time to run the Hub for; the method will block for the duration
	 * @throws MyoException If the Myo is not initialized
	 * @see #startHubThread(int)
	 */
	public void runHub(int millis) {
		checkInit();
		__runHub(millis);
	}
	/**
	 * Runs the Myo Hub until a single event occurs, or until timeout. 
	 * The Myo Hub needs to be run constantly to obtain data; so if the Hub thread is not running via 
	 * {@link #startHubThread(int)}, this method must be called repeatedly in a loop.
	 * @param millis The timeout
	 * @throws MyoException If the Myo is not initialized
	 * @see #runHub(int)
	 */
	public void runHubOnce(int millis) {
		checkInit();
		__runHubOnce(millis);
	}
	
	private native boolean __lock();
	/**
	 * Locks the Myo.
	 * @throws MyoException If the Myo is not initialized, or locking failed
	 */
	public void lock() {
		checkInit();
		if(!__lock()) {
			throw new MyoException("Failed to lock Myo");
		}
	}
	
	/**
	 * This unlock type unlocks the Myo until it is told to lock again.
	 */
	public static final int UNLOCK_HOLD = 0;
	/**
	 * This unlock type unlocks the Myo for a short period of time, then locks it again automatically.
	 * Useful for allowing pose transitions.
	 */
	public static final int UNLOCK_TIMED = 1;
	private native boolean __unlock(int type);
	/**
	 * Unlocks the Myo.
	 * @param type The unlock type, {@link #UNLOCK_HOLD} or {@link #UNLOCK_TIMED}
	 * @throws MyoException If the Myo is not initialized, or unlocking failed
	 */
	public void unlock(int type) {
		checkInit();
		if(!__unlock(type)) {
			throw new MyoException("Failed to unlock Myo");
		}
	}
	/**
	 * Unlocks the Myo. Equivalent to calling {@link #unlock(int)} with {@link #UNLOCK_HOLD}
	 * @throws MyoException If the Myo is not initialized, or unlocking failed
	 */
	public void unlock() {
		unlock(UNLOCK_HOLD);
	}
	
	private native boolean __isLocked();
	/**
	 * Test for whether the Myo is locked.
	 * @return Whether the Myo is locked
	 * @throws MyoException If the Myo is not initialized
	 */
	public boolean isLocked() {
		checkInit();
		return __isLocked();
	}
	/**
	 * Test for whether the Myo is unlocked.
	 * @return Whether the Myo is unlocked
	 * @throws MyoException If the Myo is not initialized
	 */
	public boolean isUnlocked() {
		return !isLocked();
	}
	
	public static final int LOCKING_POLICY_NONE = 0;
	public static final int LOCKING_POLICY_STANDARD = 1;
	private native void __setLockingPolicy(int policy);
	public void setLockingPolicy(int policy) {
		if(policy != LOCKING_POLICY_NONE && policy != LOCKING_POLICY_STANDARD) {
			throw new IllegalArgumentException("Invalid locking policy");
		}
		checkInit();
		__setLockingPolicy(policy);
	}
	
	public static final int VIBRATION_SHORT = 0;
	public static final int VIBRATION_MEDIUM = 1;
	public static final int VIBRATION_LONG = 2;
	private native void __vibrate(int type);
	public void vibrate(int type) {
		if(type != VIBRATION_SHORT && type != VIBRATION_MEDIUM && type != VIBRATION_LONG) {
			throw new IllegalArgumentException("Invalid vibration type");
		}
		checkInit();
		__vibrate(type);
	}
	
	private native void __notifyUserAction();
	/**
	 * Notifies the user that an event has been caused due to their action.
	 * This will cause a short vibration in the Myo.
	 * @throws MyoException If the Myo is not initialized
	 */
	public void notifyUserAction() {
		checkInit();
		__notifyUserAction();
	}
	
	private native boolean __isOnArm();
	/**
	 * Test for whether the Myo is on the arm of a person.
	 * @return Whether the Myo is on an arm
	 * @throws MyoException If the Myo is not initialized
	 */
	public boolean isOnArm() {
		checkInit();
		return __isOnArm();
	}
	
	/**
	 * Left arm.
	 */
	public static final int ARM_LEFT = 0;
	/**
	 * Right arm.
	 */
	public static final int ARM_RIGHT = 1;
	/**
	 * Unknown arm, if the Myo is not on an arm.
	 */
	public static final int ARM_UNKNOWN = 2;
	private native int __getArm();
	/**
	 * Retrieves the arm the Myo is on. The return values can be one of the following:
	 * {@link #ARM_LEFT}, {@link #ARM_RIGHT}, or {@link #ARM_UNKNOWN}.
	 * @return The arm the Myo is on
	 * @throws MyoException If the Myo is not initialized
	 */
	public int getArm() {
		checkInit();
		return __getArm();
	}
	
	private native void __updateRef();
	/**
	 * Updates the reference orientation to the current orientation of the Myo.
	 * Any future orientation data will be relative to the reference.
	 * @throws MyoException If the Myo is not initialized
	 */
	public void updateReferenceOrientation() {
		checkInit();
		__updateRef();
	}
	
	private double result_yaw, result_pitch, result_roll;
	private native void __getOrientation();
	/**
	 * Gets the Euler angle orientation of the Myo.
	 * @return The orientation of the Myo
	 * @throws MyoException If the Myo is not initialized
	 */
	public EulerOrientation getOrientation() {
		checkInit();
		__getOrientation();
		return new EulerOrientation(result_yaw, result_pitch, result_roll);
	}
	
	public static final int POSE_REST = 0;
	public static final int POSE_FIST = 1;
	public static final int POSE_SPREADFINGERS = 2;
	public static final int POSE_WAVEIN = 3;
	public static final int POSE_WAVEOUT = 4;
	public static final int POSE_DOUBLETAP = 5;
	public static final int POSE_UNKNOWN = 6;
	private native int __getPose();
	/**
	 * Gets the current Pose of the user. This can be one of the following:
	 * <ul>
	 * 	<li>{@link #POSE_REST}</li>
	 * 	<li>{@link #POSE_FIST}</li>
	 * 	<li>{@link #POSE_SPREADFINGERS}</li>
	 * 	<li>{@link #POSE_WAVEIN}</li>
	 * 	<li>{@link #POSE_WAVEOUT}</li>
	 * 	<li>{@link #POSE_DOUBLETAP}</li>
	 * 	<li>{@link #POSE_UNKNOWN}</li>
	 * </ul>
	 * @return The pose of the user.
	 * @throws MyoException If the Myo is not initialized
	 */
	public int getPose() {
		checkInit();
		return __getPose();
	}
	
	/**
	 * Cleans up the Myo.
	 * @throws MyoException If the Myo is not initialized
	 */
	public void cleanup() {
		checkInit();
		if(initialized) {
			lock();
		}
		stopHubThread();
	}
}
