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

//Unlocking behavior of the Myo
enum MyoUnlockMode {
	UNLOCK_NORMAL,
	UNLOCK_HOLD
};
MyoUnlockMode unlockMode = MyoUnlockMode::UNLOCK_HOLD;
//Details of Myo device listeners can be found in the hello-myo example.
class PoseDataCollector : public myo::DeviceListener {
public:
	bool onArm;
	myo::Arm arm;
	bool isUnlocked;
	myo::Pose currentPose;
	bool active;
	//Keep a pointer so we can lock and unlock anytime
	myo::Myo* theMyo = nullptr;

	PoseDataCollector() : active(true), onArm(false), isUnlocked(false), currentPose() {
	}

	void onUnpair(myo::Myo* myo, uint64_t timestamp) override {
		onArm = false;
		isUnlocked = false;
		theMyo = nullptr;
		active = false;
	}

	void onPose(myo::Myo* myo, uint64_t timestamp, myo::Pose pose) override {
		currentPose = pose;

		if (pose != myo::Pose::unknown && pose != myo::Pose::rest) {
			myo->notifyUserAction();
			
			//Do locking processing only if unlock mode is normal
			if (unlockMode == MyoUnlockMode::UNLOCK_NORMAL) {
				myo->unlock(myo::Myo::unlockHold);
			}
		}
		else if (unlockMode == MyoUnlockMode::UNLOCK_NORMAL) {
			myo->unlock(myo::Myo::unlockTimed);
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
		isUnlocked = true;
	}

	void onLock(myo::Myo* myo, uint64_t timestamp) override {
		isUnlocked = false;
	}
};

SOCKET listenerSocket, clientSocket;
PoseDataCollector pdc = PoseDataCollector();
void lockMyo() {
	pdc.theMyo->lock();
}
void unlockMyo() {
	pdc.theMyo->unlock(myo::Myo::unlockHold);
}
bool isMyoUnlocked() {
	return pdc.isUnlocked;
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
		setupSockets(listenerSocket, clientSocket);

		std::cout << "Connection established. Connecting to Myo Hub..." << std::endl;
		myo::Hub hub("org.usfirst.frc.team6135.MyoBot_Cpp");

		std::cout << "Waiting for Myo..." << std::endl;
		myo::Myo* myo = hub.waitForMyo(10000);
		if (!myo) {
			throw std::runtime_error("Unable to find Myo");
		}
		std::cout << "Myo found!" << std::endl;
		hub.addListener(&pdc);
		std::cout << "Waiting for Myo to connect..." << std::endl;
		while (pdc.theMyo == nullptr) {
			if (exitFlag)
				throw std::runtime_error("Aborted.");
			Sleep(100);
		}
		std::cout << "Myo is connected." << std::endl;

		while (true) {
			if (exitFlag)
				break;

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

			//Spaces are added to completely cover the original
			std::string myoState = (pdc.isUnlocked ? "Unlocked " : "Locked ");
			myoState += ((unlockMode == MyoUnlockMode::UNLOCK_NORMAL) ? "(Normal)" : "(Hold)");
			//Concatenate an empty string in the end to keep the length consistent
			//This makes sure that the previous text is completely overwritten
			std::cout << "\r" << "Myo Unlock State: " << myoState << std::string(17 - myoState.length(), ' ');
		}

		cleanupSockets(listenerSocket, clientSocket);
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

