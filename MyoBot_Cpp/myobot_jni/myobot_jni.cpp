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
	//Keep a pointer so we can lock and unlock anytime
	myo::Myo* theMyo = nullptr;

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
		theMyo = nullptr;
		active = false;
	}

	//Collect pose data
	void onPose(myo::Myo* myo, uint64_t timestamp, myo::Pose pose) override {
		//Update Myo pointer if necessary
		if (!theMyo) {
			theMyo = myo;
		}
		currentPose = pose;

		myo->unlock(Myo::unlockHold);
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
		theMyo = myo;
	}

	void onArmUnsync(myo::Myo* myo, uint64_t timestamp) override {
		onArm = false;
		theMyo = nullptr;
	}

	void onUnlock(myo::Myo* myo, uint64_t timestamp) override {
		if (!theMyo) {
			theMyo = myo;
		}
		isUnlocked = true;
	}

	void onLock(myo::Myo* myo, uint64_t timestamp) override {
		if (!theMyo) {
			theMyo = myo;
		}
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

void setStaticDoubleField(JNIEnv *env, jclass clazz, char* name, double value) {
	jfieldID fid = env->GetStaticFieldID(clazz, name, "D");
	env->SetStaticDoubleField(clazz, fid, value);
}

SingleMyoDataCollector collector;

JNIEXPORT void JNICALL Java_myobot_bridge_Myo__1_1initialize(JNIEnv *env, jclass jclass) {

	try {
		Hub hub("org.usfirst.frc.team6135.MyoBot");
		Myo *myo = hub.waitForMyo(10000);
		if (!myo) {
			return;
		}
		collector = SingleMyoDataCollector();
		hub.addListener(&collector);
	}
	catch (...) {
		return;
	}
 	
	jfieldID fidInitBoolean = env->GetStaticFieldID(jclass, "initialized", "Z");
	env->SetStaticBooleanField(jclass, fidInitBoolean, true);
}