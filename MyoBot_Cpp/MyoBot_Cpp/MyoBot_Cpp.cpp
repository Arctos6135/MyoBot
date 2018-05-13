// MyoBot_Cpp.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include "socketutils.h"

#define ACT_REST (uint32_t) 0x0000
#define ACT_DRIVEFORWARD (uint32_t) 0x0001

#define UPDATE_FREQUENCY 20

class PoseDataCollector : public myo::DeviceListener {
public:
	bool onArm;
	myo::Arm arm;
	bool isUnlocked;
	myo::Pose currentPose;
	bool active;

	PoseDataCollector() : active(true), onArm(false), isUnlocked(false), currentPose() {
	}

	void onUnpair(myo::Myo* myo, uint64_t timestamp) override {
		active = false;
		onArm = false;
		isUnlocked = false;
	}

	void onPose(myo::Myo* myo, uint64_t timestamp, myo::Pose pose) override {
		currentPose = pose;

		if (pose != myo::Pose::unknown && pose != myo::Pose::rest) {
			//Unlock the myo until it is told to lock again
			//Allows pose holding without locking
			myo->unlock(myo::Myo::unlockHold);
			myo->notifyUserAction();
		}
		else {
			//Unlock for a short period of time only to allow for pose changes
			myo->unlock(myo::Myo::unlockTimed);
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
};

SOCKET listenerSocket, clientSocket;

int main() {
	try {
		std::cout << "Creating sockets..." << std::endl;
		setupSockets(listenerSocket, clientSocket);

		myo::Hub hub("org.usfirst.frc.team6135");
		std::cout << "Waiting for Myo..." << std::endl;
		myo::Myo* myo = hub.waitForMyo(10000);
		if (!myo) {
			throw std::runtime_error("Unable to find Myo");
		}
		std::cout << "Myo detected!" << std::endl;
		PoseDataCollector pdc;
		hub.addListener(&pdc);

		while (true) {
			hub.run(1000 / UPDATE_FREQUENCY);

			myo::Pose pose = pdc.currentPose;

			uint32_t action;
			
			if (pose == myo::Pose::fist) {
				action = ACT_DRIVEFORWARD;
			}
			else if (pose == myo::Pose::rest) {
				action = ACT_REST;
			}
			else {
				action = ACT_REST;
			}
			
			sendAction(action, clientSocket);
		}

		cleanupSockets(listenerSocket, clientSocket);
	}
	catch (std::runtime_error e) {
		std::cerr << e.what() << std::endl;
		return 1;
	}

    return 0;
}

