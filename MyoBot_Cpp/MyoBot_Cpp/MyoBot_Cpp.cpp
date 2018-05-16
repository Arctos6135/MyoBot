// MyoBot_Cpp.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include "socketutils.h"

#define ACT_REST (uint32_t) 0x0000
#define ACT_DRIVEFORWARD (uint32_t) 0x0001
#define ACT_TURNLEFT (uint32_t) 0x0002
#define ACT_TURNRIGHT (uint32_t) 0x0003
#define ACT_DRIVEBACK (uint32_t) 0x0004

#define UPDATE_FREQUENCY 10

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

int main(int argc, char** argv) {

	try {
		std::cout << "Creating sockets..." << std::endl;
		setupSockets(listenerSocket, clientSocket);

		std::cout << "Connection established. Creating Myo hub..." << std::endl;
		myo::Hub hub("org.usfirst.frc.team6135.MyoBot_Cpp");

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

			if (!pdc.active)
				break;

			uint32_t action = ACT_REST;

			//Send data to move only if the Myo is on arm
			if (pdc.onArm) {
				myo::Pose pose = pdc.currentPose;

				if (pose == myo::Pose::fist) {
					action = ACT_DRIVEFORWARD;
				}
				else if (pose == myo::Pose::waveIn) {
					//Check which arm the Myo is on to make controls also intuitive for left-handed people
					action = (pdc.arm == myo::Arm::armRight) ? ACT_TURNLEFT : ACT_TURNRIGHT;
				}
				else if (pose == myo::Pose::waveOut) {
					action = (pdc.arm == myo::Arm::armRight) ? ACT_TURNRIGHT : ACT_TURNLEFT;
				}
				else if (pose == myo::Pose::fingersSpread) {
					action = ACT_DRIVEBACK;
				}
			}
			
			sendAction(action, clientSocket);

			std::cout << "\r" << "Myo: " << ((pdc.isUnlocked) ? "Unlocked" : "Locked");
		}

		cleanupSockets(listenerSocket, clientSocket);
	}
	catch (const std::exception& e) {
		std::cerr << e.what() << std::endl;
		std::cerr << "Press enter to continue.";
		std::cin.ignore();
		return 1;
	}
	catch (...) {
		std::cerr << "An error occurred." << std::endl;
		std::cout << "Press enter to continue." << std::endl;
		std::cin.ignore();
		return 1;
	}

    return 0;
}

