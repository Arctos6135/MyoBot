package myobot.bridge.myo;

import com.thalmic.myo.Arm;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.Quaternion;
import com.thalmic.myo.WarmupState;
import com.thalmic.myo.XDirection;

public class DataCollector extends DeviceListener {
	
	boolean onArm = false;
	boolean isUnlocked = false;
	Arm arm = Arm.armUnknown;
	Pose currentPose = Pose.unknown;
	
	Quaternion refOrientation = null;
	Quaternion orientationRaw = null;
	Quaternion orientation = null;
	
	double yaw, pitch, roll;
	
	public DataCollector() {
	}
	
	public boolean onArm() {
		return onArm;
	}
	public boolean isUnlocked() {
		return isUnlocked;
	}
	public Pose getPose() {
		return currentPose;
	}
	public Quaternion getOrientationQuat() {
		return orientation;
	}
	public EulerOrientation getOrienationEuler() {
		return new EulerOrientation(yaw, pitch, roll);
	}
	public Quaternion getRawOrientation() {
		return orientationRaw;
	}
	public Quaternion getRefOrientation() {
		return refOrientation.conjugate();
	}
	public void setRefOrientation(Quaternion ref) {
		refOrientation = ref.conjugate();
	}
	public Arm getArm() {
		return arm;
	}
	
	@Override
	public void onUnpair(Myo myo, long timestamp) {
		onDisconnect(myo, timestamp);
	}
	@Override
	public void onDisconnect(Myo myo, long timestamp) {
		onArm = false;
		isUnlocked = false;
	}
	@Override
	public void onArmSync(Myo myo, long timestamp, Arm arm, XDirection xDirection, float rotation, WarmupState warmupState) {
		onArm = true;
		this.arm = arm;
	}
	@Override
	public void onArmUnsync(Myo myo, long timestamp) {
		onArm = false;
	}
	@Override
	public void onLock(Myo myo, long timestamp) {
		isUnlocked = false;
	}
	@Override
	public void onUnlock(Myo myo, long timestamp) {
		isUnlocked = true;
	}
	
	@Override
	public void onPose(Myo myo, long timestamp, Pose pose) {
		currentPose = pose;
	}
	@Override
	public void onOrientationData(Myo myo, long timestamp, Quaternion quat) {
		orientationRaw = quat;
		
		if(refOrientation != null) {
			orientation = refOrientation.multiply(orientationRaw);
		}
		else {
			orientation = quat;
		}
		
		roll = Math.atan2(2.0f * (orientation.w() * orientation.x() + orientation.y() * orientation.z()),
				1.0f - 2.0f * (orientation.x() * orientation.x() + orientation.y() * orientation.y()));
		pitch = Math.asin(Math.max(-1.0f, Math.min(1.0f, 2.0f * (orientation.w() * orientation.y() - orientation.z() * orientation.x()))));
		yaw = Math.atan2(2.0f * (orientation.w() * orientation.z() + orientation.x() * orientation.y()),
				1.0f - 2.0f * (orientation.y() * orientation.y() + orientation.z() * orientation.z()));
	}
	
}
