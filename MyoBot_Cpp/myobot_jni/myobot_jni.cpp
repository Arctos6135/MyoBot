#include <jni.h>
#include "myobot_bridge_Myo.h"
#include <iostream>
#include <myo/myo.hpp>

using namespace std;
using namespace myo;

class SingleMyoDataCollector : public myo::DeviceListener {
public:
	bool onArm;
	myo::Arm arm;
	bool isUnlocked;
	myo::Pose currentPose;
	bool active;

	//Reference orientation data
	//Each new orientation will be changed so that it is in relation to the reference orientation
	//This makes sure that our orientations are in relation to the user, not some arbitary start orientation.
	float refRoll, refPitch, refYaw;
	//Note: refOrientation keeps the inverse quaternion of the reference orientation. This is so that we can
	//simply multiply by it instead of having to calculate the inverse every time.
	myo::Quaternion<float> refOrientation;
	//Orientation data
	float roll, pitch, yaw;
	myo::Quaternion<float> orientation;
	//Raw orientation data; this quaternion is not in relation to the reference.
	myo::Quaternion<float> orientationRaw;

	//If set to true, Euler angles will be inverted
	//Since the direction of the roll, pitch and yaw depend on the Myo's x direction, this may be set
	//if the armband is worn backwards.
	bool invertAngles = false;

	SingleMyoDataCollector() : active(true), onArm(false), isUnlocked(false), currentPose(),
		roll(0), pitch(0), yaw(0), refRoll(0), refPitch(0), refYaw(0) {
	}

	void onUnpair(myo::Myo* myo, uint64_t timestamp) override {
		onArm = false;
		isUnlocked = false;
		active = false;
	}

	//Collect pose data
	void onPose(myo::Myo* myo, uint64_t timestamp, myo::Pose pose) override {
		currentPose = pose;
	}

	//Collect orientation data
	void onOrientationData(myo::Myo* myo, uint64_t timestamp, const myo::Quaternion<float>& quat) override {

		orientationRaw = myo::Quaternion<float>(quat);

		//Multiply by the inverse of the reference orientation to change perspective
		orientation = refOrientation * quat;

		//Calculate roll, pitch and yaw
		//Code taken directly from the example
		roll = atan2(2.0f * (orientation.w() * orientation.x() + orientation.y() * orientation.z()),
			1.0f - 2.0f * (orientation.x() * orientation.x() + orientation.y() * orientation.y()));
		pitch = asin(max(-1.0f, min(1.0f, 2.0f * (orientation.w() * orientation.y() - orientation.z() * orientation.x()))));
		yaw = atan2(2.0f * (orientation.w() * orientation.z() + orientation.x() * orientation.y()),
			1.0f - 2.0f * (orientation.y() * orientation.y() + orientation.z() * orientation.z()));

		if (invertAngles) {
			roll *= -1;
			pitch *= -1;
			yaw *= -1;
		}
	}

	void onArmSync(myo::Myo* myo, uint64_t timestamp, myo::Arm arm, myo::XDirection xDirection, float rotation, myo::WarmupState warmupState) override {
		onArm = true;
		this->arm = arm;
	}

	void onArmUnsync(myo::Myo* myo, uint64_t timestamp) override {
		onArm = false;
	}

	void onUnlock(myo::Myo* myo, uint64_t timestamp) override {
		isUnlocked = true;
	}

	void onLock(myo::Myo* myo, uint64_t timestamp) override {
		isUnlocked = false;
	}

	//Sets/updates the reference orientation.
	void setRefOrientation(const myo::Quaternion<float>& ref) {
		myo::Quaternion<float> inverse = ref.conjugate();
		//Assign the ref quaternion to the inverse
		refOrientation = inverse;

		//The ref roll, pitch and yaw are not inverted however
		refRoll = atan2(2.0f * (ref.w() * ref.x() + ref.y() * ref.z()),
			1.0f - 2.0f * (ref.x() * ref.x() + ref.y() * ref.y()));
		refPitch = asin(max(-1.0f, min(1.0f, 2.0f * (ref.w() * ref.y() - ref.z() * ref.x()))));
		refYaw = atan2(2.0f * (ref.w() * ref.z() + ref.x() * ref.y()),
			1.0f - 2.0f * (ref.y() * ref.y() + ref.z() * ref.z()));

		if (invertAngles) {
			refRoll *= -1;
			refPitch *= -1;
			refYaw *= -1;
		}
	}
};

#define HANDLE_MYO "_myoHandle"
#define HANDLE_HUB "_hubHandle"
#define HANDLE_COLLECTOR "_collectorHandle"
typedef SingleMyoDataCollector Collector;

template <typename T>
T* getHandle(JNIEnv *env, jobject obj, const char *name) {
	jfieldID fid = env->GetFieldID(env->GetObjectClass(obj), name, "J");
	jlong l = env->GetLongField(obj, fid);
	return reinterpret_cast<T*>(l);
}

JNIEXPORT jboolean JNICALL Java_myobot_bridge_myo_Myo__1_1initialize(JNIEnv *env, jobject obj) {

	try {
		static Hub hub("org.usfirst.frc.team6135.MyoBot");
		Myo *myo = hub.waitForMyo(10000);
		if (!myo) {
			return false;
		}
		static SingleMyoDataCollector collector;
		hub.addListener(&collector);

		jclass myoClass = env->GetObjectClass(obj);
		jfieldID fidMyoPointer = env->GetFieldID(myoClass, HANDLE_MYO, "J");
		jfieldID fidHubPointer = env->GetFieldID(myoClass, HANDLE_HUB, "J");
		jfieldID fidCollectorPointer = env->GetFieldID(myoClass, HANDLE_COLLECTOR, "J");
		env->SetLongField(obj, fidMyoPointer, (jlong)myo);
		env->SetLongField(obj, fidHubPointer, (jlong)&hub);
		env->SetLongField(obj, fidCollectorPointer, (jlong)&collector);
	}
	catch (...) {
		return false;
	}
	return true;
}

JNIEXPORT void JNICALL Java_myobot_bridge_myo_Myo__1_1runHub(JNIEnv *env, jobject obj, jint millis) {
	Hub *hub = getHandle<Hub>(env, obj, HANDLE_HUB);
	if (hub) {
		hub->run(millis);
	}
}

JNIEXPORT void JNICALL Java_myobot_bridge_myo_Myo__1_1runHubOnce(JNIEnv *env, jobject obj, jint millis) {
	Hub *hub = getHandle<Hub>(env, obj, HANDLE_HUB);
	if (hub) {
		hub->runOnce(millis);
	}
}

JNIEXPORT jboolean JNICALL Java_myobot_bridge_myo_Myo__1_1lock(JNIEnv *env, jobject obj) {
	Myo *myo = getHandle<Myo>(env, obj, HANDLE_MYO);
	if (!myo) {
		return false;
	}
	myo->lock();
	return true;
}

JNIEXPORT jboolean JNICALL Java_myobot_bridge_myo_Myo__1_1unlock(JNIEnv *env, jobject obj, jint type) {
	Myo *myo = getHandle<Myo>(env, obj, HANDLE_MYO);
	if (!myo) {
		return false;
	}
	myo->unlock(type == myobot_bridge_myo_Myo_UNLOCK_HOLD ? Myo::unlockHold : Myo::unlockTimed);
	return true;
}

JNIEXPORT jboolean JNICALL Java_myobot_bridge_myo_Myo__1_1isLocked(JNIEnv *env, jobject obj) {
	return !getHandle<Collector>(env, obj, HANDLE_COLLECTOR)->isUnlocked;
}

JNIEXPORT void JNICALL Java_myobot_bridge_myo_Myo__1_1setLockingPolicy(JNIEnv *env, jobject obj, jint policy) {
	Hub *hub = getHandle<Hub>(env, obj, HANDLE_HUB);
	if (hub) {
		hub->setLockingPolicy(policy == myobot_bridge_myo_Myo_LOCKING_POLICY_NONE ? Hub::lockingPolicyNone : Hub::lockingPolicyStandard);
	}
}

JNIEXPORT void JNICALL Java_myobot_bridge_myo_Myo__1_1vibrate(JNIEnv *env, jobject obj, jint type) {
	Myo *myo = getHandle<Myo>(env, obj, HANDLE_MYO);
	if (myo) {
		switch (type) {
		case myobot_bridge_myo_Myo_VIBRATION_LONG:
			myo->vibrate(Myo::vibrationLong);
			break;
		case myobot_bridge_myo_Myo_VIBRATION_MEDIUM:
			myo->vibrate(Myo::vibrationMedium);
			break;
		case myobot_bridge_myo_Myo_VIBRATION_SHORT:
			myo->vibrate(Myo::vibrationShort);
			break;
		default:break;
		}
	}
}
JNIEXPORT void JNICALL Java_myobot_bridge_myo_Myo__1_1notifyUserAction(JNIEnv *env, jobject obj) {
	getHandle<Myo>(env, obj, HANDLE_MYO)->notifyUserAction();
}

JNIEXPORT jboolean JNICALL Java_myobot_bridge_myo_Myo__1_1isOnArm(JNIEnv *env, jobject obj) {
	return getHandle<Collector>(env, obj, HANDLE_COLLECTOR)->onArm;
}

JNIEXPORT jint JNICALL Java_myobot_bridge_myo_Myo__1_1getArm(JNIEnv *env, jobject obj) {
	Collector *collector = getHandle<Collector>(env, obj, HANDLE_COLLECTOR);
	if (!collector->onArm)
		return myobot_bridge_myo_Myo_ARM_UNKNOWN;
	switch (collector->arm) {
	case Arm::armLeft:
		return myobot_bridge_myo_Myo_ARM_LEFT;
	case Arm::armRight:
		return myobot_bridge_myo_Myo_ARM_RIGHT;
	case Arm::armUnknown:
		return myobot_bridge_myo_Myo_ARM_UNKNOWN;
	default: return 2;
	}
}

JNIEXPORT void JNICALL Java_myobot_bridge_myo_Myo__1_1updateRef(JNIEnv *env, jobject obj) {
	SingleMyoDataCollector *collector = getHandle<Collector>(env, obj, HANDLE_COLLECTOR);
	collector->setRefOrientation(collector->orientationRaw);
}

JNIEXPORT void JNICALL Java_myobot_bridge_myo_Myo__1_1getOrientation(JNIEnv *env, jobject obj) {
	SingleMyoDataCollector *collector = getHandle<Collector>(env, obj, HANDLE_COLLECTOR);
	jclass myoClass = env->GetObjectClass(obj);

	jfieldID fidYaw = env->GetFieldID(myoClass, "result_yaw", "D");
	jfieldID fidPitch = env->GetFieldID(myoClass, "result_pitch", "D");
	jfieldID fidRoll = env->GetFieldID(myoClass, "result_roll", "D");
	env->SetDoubleField(obj, fidYaw, static_cast<jdouble>(collector->yaw));
	env->SetDoubleField(obj, fidPitch, static_cast<jdouble>(collector->pitch));
	env->SetDoubleField(obj, fidRoll, static_cast<jdouble>(collector->roll));
}

JNIEXPORT jint JNICALL Java_myobot_bridge_myo_Myo__1_1getPose(JNIEnv *env, jobject obj) {
	myo::Pose pose = getHandle<Collector>(env, obj, HANDLE_COLLECTOR)->currentPose;
	if (pose == Pose::rest) {
		return myobot_bridge_myo_Myo_POSE_REST;
	}
	if (pose == Pose::fist) {
		return myobot_bridge_myo_Myo_POSE_FIST;
	}
	if (pose == Pose::fingersSpread) {
		return myobot_bridge_myo_Myo_POSE_SPREADFINGERS;
	}
	if (pose == Pose::waveIn) {
		return myobot_bridge_myo_Myo_POSE_WAVEIN;
	}
	if (pose == Pose::waveOut) {
		return myobot_bridge_myo_Myo_POSE_WAVEOUT;
	}
	if (pose == Pose::doubleTap) {
		return myobot_bridge_myo_Myo_POSE_DOUBLETAP;
	}
	return myobot_bridge_myo_Myo_POSE_UNKNOWN;
}