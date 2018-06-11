// MyoBot_Cpp.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include "socketutils.h"

#define ACT_REST (uint32_t) 0x0000
#define ACT_DRIVEFORWARD (uint32_t) 0x0001
#define ACT_TURNLEFT (uint32_t) 0x0002
#define ACT_TURNRIGHT (uint32_t) 0x0003
#define ACT_DRIVEBACK (uint32_t) 0x0004
#define ACT_RAISEELEVATOR (uint32_t) 0x0005
#define ACT_LOWERELEVATOR (uint32_t) 0x0006
#define ACT_INTAKE (uint32_t) 0x0007
#define ACT_OUTTAKE (uint32_t) 0x0008

const char PARAM_NULL[4] = { 0x00, 0x00, 0x00, 0x00 };

#define UPDATE_FREQUENCY 10

#define PI 3.14159265359

//Unlocking behavior of the Myo
enum MyoUnlockMode {
	UNLOCK_NORMAL,
	UNLOCK_HOLD
};
MyoUnlockMode unlockMode = MyoUnlockMode::UNLOCK_HOLD;
//Details of Myo device listeners can be found in the hello-myo example.
class SingleMyoDataCollector : public myo::DeviceListener {
public:
	bool onArm;
	myo::Arm arm;
	bool isUnlocked;
	myo::Pose currentPose;
	bool active;

	//Last orientation data
	float roll, pitch, yaw;
	float refRoll, refPitch, refYaw;
	myo::Quaternion<float>* orientation = nullptr;
	const myo::Quaternion<float>* orientationRaw = nullptr;
	//Keep a pointer so we can lock and unlock anytime
	myo::Myo* theMyo = nullptr;
	myo::Quaternion<float>* refOrientation = nullptr;

	SingleMyoDataCollector() : active(true), onArm(false), isUnlocked(false), currentPose(),
	roll(0), pitch(0), yaw(0) {
	}

	void onUnpair(myo::Myo* myo, uint64_t timestamp) override {
		onArm = false;
		isUnlocked = false;
		theMyo = nullptr;
		active = false;
	}

	void onPose(myo::Myo* myo, uint64_t timestamp, myo::Pose pose) override {
		if (!theMyo) {
			theMyo = myo;
		}
		currentPose = pose;

		if (pose != myo::Pose::unknown && pose != myo::Pose::rest) {
			myo->notifyUserAction();
			
			myo->unlock(myo::Myo::unlockHold);
		}
		//Do locking processing only if unlock mode is normal
		else if (unlockMode == MyoUnlockMode::UNLOCK_NORMAL) {
			myo->unlock(myo::Myo::unlockTimed);
		}
		else {
			myo->unlock(myo::Myo::unlockHold);
		}
	}

	void onOrientationData(myo::Myo* myo, uint64_t timestamp, const myo::Quaternion<float>& quat) override {
		orientationRaw = &quat;

		myo::Quaternion<float> q = quat;

		if (refOrientation) {
			q *= *refOrientation;
		}

		orientation = &q;

		roll = atan2(2.0f * (q.w() * q.x() + q.y() * q.z()),
			1.0f - 2.0f * (q.x() * q.x() + q.y() * q.y()));
		pitch = asin(max(-1.0f, min(1.0f, 2.0f * (q.w() * q.y() - q.z() * q.x()))));
		yaw = atan2(2.0f * (q.w() * q.z() + q.x() * q.y()),
			1.0f - 2.0f * (q.y() * q.y() + q.z() * q.z()));
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

	void setRefOrientation(const myo::Quaternion<float>* ref) {
		myo::Quaternion<float> inverse = ref->conjugate();
		refOrientation = &inverse;

		refRoll = atan2(2.0f * (ref->w() * ref->x() + ref->y() * ref->z()),
			1.0f - 2.0f * (ref->x() * ref->x() + ref->y() * ref->y()));
		refPitch = asin(max(-1.0f, min(1.0f, 2.0f * (ref->w() * ref->y() - ref->z() * ref->x()))));
		refYaw = atan2(2.0f * (ref->w() * ref->z() + ref->x() * ref->y()),
			1.0f - 2.0f * (ref->y() * ref->y() + ref->z() * ref->z()));
	}
};

SOCKET listenerSocket, clientSocket;
SingleMyoDataCollector collector = SingleMyoDataCollector();
void lockMyo() {
	if (collector.theMyo)
		collector.theMyo->lock();
}
void unlockMyo() {
	if (collector.theMyo)
		collector.theMyo->unlock(myo::Myo::unlockHold);
}
bool isMyoUnlocked() {
	return collector.isUnlocked;
}

std::atomic<bool> exitFlag;

LRESULT CALLBACK LowLevelKeyboardHook(int nCode, WPARAM wParam, LPARAM lParam) {
	if (nCode == HC_ACTION && wParam == WM_SYSKEYDOWN) {
		//Check if alt is pressed at the same time
		short altState = GetKeyState(VK_MENU);
		if (!(altState & 0x8000)) {
			return CallNextHookEx(NULL, nCode, wParam, lParam);
		}
		PKBDLLHOOKSTRUCT p = (PKBDLLHOOKSTRUCT)lParam;
		switch (p->vkCode) {
		case 'E':
			exitFlag = true;
			break;
		case 'U':
			if (!collector.theMyo) {
				std::cout << '\a' << std::flush;
				break;
			}
			if (isMyoUnlocked()) {
				lockMyo();
			}
			else {
				unlockMyo();
			}
			break;
		case 'M':
			unlockMode = ((unlockMode == MyoUnlockMode::UNLOCK_HOLD) ? MyoUnlockMode::UNLOCK_NORMAL : MyoUnlockMode::UNLOCK_HOLD);
			break;
		case 'I':
			collector.setRefOrientation(collector.orientationRaw);
			std::cout << "Reference orientation updated: roll=" << collector.refRoll << " pitch=" << collector.refPitch << " yaw=" << collector.refYaw << std::endl;
			break;
		default: return CallNextHookEx(NULL, nCode, wParam, lParam);
		}
		return 1;
	}
	else {
		return CallNextHookEx(NULL, nCode, wParam, lParam);
	}
}

//Low-Level keyboard hooks require a message loop
//The loop is implemented in a separate thread here
unsigned int __stdcall messageLoopThread(void* data) {
	//An atomic bool is shared and if set to true both threads will exit
	std::atomic<bool>& exitFlag = *((std::atomic<bool>*) data);

	HHOOK keyboardHook = SetWindowsHookEx(WH_KEYBOARD_LL, &LowLevelKeyboardHook, NULL, 0);

	MSG msg;
	while (GetMessage(&msg, NULL, 0, 0) != 0 && !exitFlag) {
		TranslateMessage(&msg);
		DispatchMessage(&msg);
	}

	UnhookWindowsHookEx(keyboardHook);
	return 0;
}

int main(int argc, char** argv) {

	exitFlag = false;
	unsigned int threadId;

	std::cout << "Creating Additional Thread..." << std::endl;
	HANDLE msgThread = (HANDLE)_beginthreadex(NULL, 0, &messageLoopThread, &exitFlag, 0, &threadId);
	std::cout << "Thread creation successful." << std::endl;

	try {
		std::cout << "Creating sockets..." << std::endl;
		//setupSockets(listenerSocket, clientSocket);

		std::cout << "Connection established. Connecting to Myo Hub..." << std::endl;
		myo::Hub hub("org.usfirst.frc.team6135.MyoBot_Cpp");

		std::cout << "Waiting for Myo..." << std::endl;
		myo::Myo* myo = hub.waitForMyo(10000);
		if (!myo) {
			throw std::runtime_error("Unable to find Myo");
		}
		std::cout << "Myo found!" << std::endl;
		hub.addListener(&collector);
		std::cout << "The Myo is not initialized.\nPlease point your arm straight forward and parallel to the ground, and press Alt+I to initialize the Myo." << std::endl;
		while (!collector.refOrientation) {
			Sleep(100);
		}

		while (true) {
			if (exitFlag)
				break;

			hub.run(1000 / UPDATE_FREQUENCY);

			if (!collector.active)
				break;

			uint32_t action = ACT_REST;
			const char* param = PARAM_NULL;

			//Send data to move only if the Myo is on arm
			if (collector.onArm) {
				/*if (collector.currentPose == myo::Pose::fist || collector.currentPose == myo::Pose::fingersSpread) {
					action = collector.currentPose == myo::Pose::fist ? ACT_DRIVEFORWARD : ACT_DRIVEBACK;

					float f = max(-PI / 2, min(PI / 2, collector.roll)) / (PI / 2);
					param = reinterpret_cast<char*>(&f);
				}
				else if (collector.currentPose == myo::Pose::waveIn) {
					action = ACT_INTAKE;
				}
				else if (collector.currentPose == myo::Pose::waveOut) {
					action = ACT_OUTTAKE;
				}
				else if (collector.currentPose == myo::Pose::unknown || collector.currentPose == myo::Pose::rest) {
					float f = max(-PI / 2, min(PI / 2, collector.pitch));

					if (abs(f) >= PI / 6) {
						action = f > 0 ? ACT_RAISEELEVATOR : ACT_LOWERELEVATOR;
					}
					else {
						action = ACT_REST;
					}
				}*/

				/*myo::Pose pose = collector.currentPose;

				if (pose == myo::Pose::fist) {
					action = ACT_DRIVEFORWARD;
				}
				else if (pose == myo::Pose::waveIn) {
					//Check which arm the Myo is on to make controls also intuitive for left-handed people
					action = (collector.arm == myo::Arm::armRight) ? ACT_TURNLEFT : ACT_TURNRIGHT;
				}
				else if (pose == myo::Pose::waveOut) {
					action = (collector.arm == myo::Arm::armRight) ? ACT_TURNRIGHT : ACT_TURNLEFT;
				}
				else if (pose == myo::Pose::fingersSpread) {
					action = ACT_DRIVEBACK;
				}

				sendAction(action, clientSocket);*/

				std::cout << "Orientation: roll=" << collector.roll << " pitch=" << collector.pitch << " yaw=" << collector.yaw << std::endl;
				
			}
			else {
				//sendAction(clientSocket, ACT_REST, PARAM_NULL);
			}
			

			/*//Spaces are added to completely cover the original
			std::string myoState = (collector.isUnlocked ? "Unlocked " : "Locked ");
			myoState += ((unlockMode == MyoUnlockMode::UNLOCK_NORMAL) ? "(Normal)" : "(Hold)");
			//Concatenate an empty string in the end to keep the length consistent
			//This makes sure that the previous text is completely overwritten
			std::cout << "\r" << "Myo Unlock State: " << myoState << std::string(17 - myoState.length(), ' ');*/
		}

		//cleanupSockets(listenerSocket, clientSocket);
	}
	catch (const std::exception& e) {
		std::cerr << e.what() << std::endl;
		std::cerr << "Press enter to continue.";
		std::cin.ignore();

		//Clean up message loop thread
		//Signal it to exit
		exitFlag = true;
		//Post a null message so GetMessage() returns
		PostThreadMessage(threadId, WM_NULL, NULL, NULL);
		WaitForSingleObject(msgThread, INFINITE);
		CloseHandle(msgThread);
		return 1;
	}
	catch (...) {
		std::cerr << "An error occurred." << std::endl;
		std::cout << "Press enter to continue." << std::endl;
		std::cin.ignore();

		//Clean up message loop thread
		//Signal it to exit
		exitFlag = true;
		//Post a null message so GetMessage() returns
		PostThreadMessage(threadId, WM_NULL, NULL, NULL);
		WaitForSingleObject(msgThread, INFINITE);
		CloseHandle(msgThread);
		return 1;
	}

	//Clean up message loop thread
	//Signal it to exit
	exitFlag = true;
	//Post a null message so GetMessage() returns
	PostThreadMessage(threadId, WM_NULL, NULL, NULL);
	WaitForSingleObject(msgThread, INFINITE);
	CloseHandle(msgThread);
    return 0;
}

