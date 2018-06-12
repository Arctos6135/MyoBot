// MyoBot_Cpp.cpp : Main file for the Myo data reader.
//

#include "stdafx.h"
#include "socketutils.h"

#pragma warning(disable: 4244)

/*
	Each message that this program sends over to the bridge program consists of 8 bytes.

	The first 4 bytes form a 32-bit integer (big endian).
	This integer value determines the 'action' that is to be performed.

	The last 4 bytes are the 'parameters' of the action. This is usually a float, but
	it does not have to be. It is used for additional data such as how fast to perform the action.
*/
//Action code set
#define ACT_REST (uint32_t) 0x0000
#define ACT_DRIVEFORWARD (uint32_t) 0x0001
#define ACT_TURNLEFT (uint32_t) 0x0002 //Unused in the current version; retained for compatibility
#define ACT_TURNRIGHT (uint32_t) 0x0003 //Unused in the current version; retained for compatibility
#define ACT_DRIVEBACK (uint32_t) 0x0004
#define ACT_RAISEELEVATOR (uint32_t) 0x0005
#define ACT_LOWERELEVATOR (uint32_t) 0x0006
#define ACT_INTAKE (uint32_t) 0x0007
#define ACT_OUTTAKE (uint32_t) 0x0008

//A null parameter message. Default value to be sent if the action does not use parameters.
const char PARAM_NULL[4] = { 0x00, 0x00, 0x00, 0x00 };

//How many times data is sent per second
#define UPDATE_FREQUENCY 10

#define PI 3.14159265359

/*
	The following enum, class and functions deal with collecting data from the Myo.
	Most of this code is taken from the hello-myo example available online at
	https://developer.thalmic.com/docs/api_reference/platform/hello-myo_8cpp-example.html

	It is also included in the SDK.
*/
/*
	For the purposes of this application, the normal unlocking behavior of the Myo is overridden.
	There are two modes and the behavior can be switched by pressing Alt+M.

	In UNLOCK_NORMAL, the Myo behaves normally. A period of inactivity will cause it to be locked,
	and the unlock gesture will unlock it.

	In UNLOCK_HOLD, the Myo is locked/unlocked by pressing Alt+U. Inactivity will no longer lock the Myo.
*/
enum MyoUnlockMode {
	UNLOCK_NORMAL,
	UNLOCK_HOLD
};
MyoUnlockMode unlockMode = MyoUnlockMode::UNLOCK_HOLD;
//Myo device listener
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

		//Keep part of the normal behavior of the Myo
		//No matter what unlock mode, if we recognize a gesture, unlock until told otherwise
		if (pose != myo::Pose::unknown && pose != myo::Pose::rest) {
			myo->notifyUserAction();
			
			myo->unlock(myo::Myo::unlockHold);
		}
		//If the unlock mode is normal, unlock for a short period of time only
		else if (unlockMode == MyoUnlockMode::UNLOCK_NORMAL) {
			myo->unlock(myo::Myo::unlockTimed);
		}
		//Otherwise, unlock until told otherwise
		else {
			myo->unlock(myo::Myo::unlockHold);
		}
	}
	
	//Collect orientation data
	void onOrientationData(myo::Myo* myo, uint64_t timestamp, const myo::Quaternion<float>& quat) override {
		orientationRaw = myo::Quaternion<float>(quat);

		//Multiply by the inverse of the reference orientation to change perspective
		orientation = quat * refOrientation;

		//Calculate roll, pitch and yaw
		//Code taken directly from the example
		roll = atan2(2.0f * (orientation.w() * orientation.x() + orientation.y() * orientation.z()),
			1.0f - 2.0f * (orientation.x() * orientation.x() + orientation.y() * orientation.y()));
		pitch = asin(max(-1.0f, min(1.0f, 2.0f * (orientation.w() * orientation.y() - orientation.z() * orientation.x()))));
		yaw = atan2(2.0f * (orientation.w() * orientation.z() + orientation.x() * orientation.y()),
			1.0f - 2.0f * (orientation.y() * orientation.y() + orientation.z() * orientation.z()));
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
	}
};

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

/*
	The following functions deal with the native Windows stuff, namely the keyboard hook.
*/
//Because our program is multi-threaded (see below), we must keep an atomic boolean flag variable to indicate when we are exiting.
std::atomic<bool> exitFlag;

//Win32 Low Level Keyboard Hook
LRESULT CALLBACK LowLevelKeyboardHook(int nCode, WPARAM wParam, LPARAM lParam) {
	//We are only allowed to process the message if the code is HC_ACTION
	//A WM_SYSKEYDOWN event is generated every time a key is pressed with the Alt key,
	//or it may be sent to the top level window. Only process WM_SYSKEYDOWN because
	//we're only interested in keys pressed with Alt
	if (nCode == HC_ACTION && wParam == WM_SYSKEYDOWN) {
		//Check if alt is pressed at the same time
		short altState = GetKeyState(VK_MENU);
		if (!(altState & 0x8000)) {
			//If not, then pass on the keys
			return CallNextHookEx(NULL, nCode, wParam, lParam);
		}
		//Handle the key combination
		PKBDLLHOOKSTRUCT p = (PKBDLLHOOKSTRUCT)lParam;
		switch (p->vkCode) {
		//Alt+E: Exit the program
		case 'E':
			exitFlag = true;
			break;
		//Alt+U: Unlock/Lock the myo
		case 'U':
			//If the Myo is still a nullptr, output an alert and exit to make sure we don't get crashes
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
		//Alt+M: Change unlock mode (normal/hold)
		case 'M':
			unlockMode = ((unlockMode == MyoUnlockMode::UNLOCK_HOLD) ? MyoUnlockMode::UNLOCK_NORMAL : MyoUnlockMode::UNLOCK_HOLD);
			break;
		//Alt+I: Initialize orientation/update reference orientation
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

//A message loop is required to handle the low level keyboard hook messages. This is why we didn't have to
//put it in a DLL. However, the GetMessage() function blocks, so our message loop should be in a separate thread.
unsigned int __stdcall messageLoopThread(void* data) {
	//An atomic bool is shared and if set to true both threads will exit
	std::atomic<bool>& exitFlag = *((std::atomic<bool>*) data);

	//Set the keyboard hook from this thread so messages will be sent to this thread
	HHOOK keyboardHook = SetWindowsHookEx(WH_KEYBOARD_LL, &LowLevelKeyboardHook, NULL, 0);

	//Classic Win32 message loop
	MSG msg;
	//Check for WM_QUIT as well as the value of our exit flag
	while (GetMessage(&msg, NULL, 0, 0) != 0 && !exitFlag) {
		TranslateMessage(&msg);
		DispatchMessage(&msg);
	}

	//When the program exits, unhook the keyboard hook
	UnhookWindowsHookEx(keyboardHook);
	return 0;
}

SOCKET listenerSocket, clientSocket;
int main(int argc, char** argv) {

	exitFlag = false;
	//ID of our message loop thread
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
		hub.addListener(&collector);
		std::cout << "Warning: The Myo's orientation is not initialized. Please put your arm to the front "
		<< "with your palm parallel to the ground, and then press Alt+I to record your reference orientation." << std::endl;

		while (true) {
			if (exitFlag)
				break;

			hub.run(1000 / UPDATE_FREQUENCY);

			if (!collector.active)
				break;

			//Initialize values for our default action and null param
			uint32_t action = ACT_REST;
			const char* param = PARAM_NULL;

			//Send data only if the Myo is on arm and unlocked.
			if (collector.onArm && collector.isUnlocked) {
				//First check if pose is fist or spread fingers
				if (collector.currentPose == myo::Pose::fist || collector.currentPose == myo::Pose::fingersSpread) {
					action = collector.currentPose == myo::Pose::fist ? ACT_DRIVEFORWARD : ACT_DRIVEBACK;

					//Take the roll of the Myo and make that the param data
					//First constrain to [-60, 60], then divide to obtain a fraction that represents how much to turn
					//Note the roll, pitch and yaw are in radians
					float f = max(-PI / 3, min(PI / 3, collector.roll)) / (PI / 3);
					//Cast to char* to take raw bytes
					param = reinterpret_cast<char*>(&f);
				}
				//Check if pose is intake or outtake
				else if (collector.currentPose == myo::Pose::waveIn) {
					action = ACT_INTAKE;
				}
				else if (collector.currentPose == myo::Pose::waveOut) {
					action = ACT_OUTTAKE;
				}
				//If the pose is unknown, then check if the arm is raised
				else if (collector.currentPose == myo::Pose::unknown || collector.currentPose == myo::Pose::rest) {
					//Same as the turning, constrain to [-60, 60] degrees
					float f = max(-PI / 3, min(PI / 3, collector.pitch));

					//Check if our pitch is more than 15 degrees
					if (abs(f) >= PI / 12) {
						action = f > 0 ? ACT_RAISEELEVATOR : ACT_LOWERELEVATOR;

						//Decrement by 15 degrees
						//Copy the sign of f to PI/12 to make sure we are decrementing the absolute value
						//even if f is negative.
						f -= copysign(PI / 12, f);
						//Because we subtracted 15 degrees, the maximum absolute value that f can have
						//is now 60-15=45 degrees.
						f /= PI / 4;
						//Cast to char* to take raw bytes
						param = reinterpret_cast<char*>(&f);
					}
					else {
						action = ACT_REST;
					}
				}
				else {
					action = ACT_REST;
				}

				sendAction(clientSocket, action, param);
			}
			else {
				//If the Myo is not on arm, or is locked, send the do nothing message to make sure everything stops.
				sendAction(clientSocket, ACT_REST, PARAM_NULL);
			}

			//Spaces are added here to keep the length consistent (see below)
			std::string myoState = (collector.isUnlocked ? "Unlocked " : "Locked ");
			myoState += ((unlockMode == MyoUnlockMode::UNLOCK_NORMAL) ? "(Normal)" : "(Hold)");
			//Concatenate an empty string in the end to keep the length consistent
			//The \r character puts the cursor back to the beginning of the line so we can overwrite the line.
			//However if our new string is shorter than our old one, then some characters of the old string
			//will still remain.
			std::cout << "\r" << "Myo Unlock State: " << myoState << std::string(17 - myoState.length(), ' ');
		}

		cleanupSockets(listenerSocket, clientSocket);
	}
	catch (const std::exception& e) {
		std::cerr << e.what() << std::endl;
		std::cerr << "Press enter to continue.";
		std::cin.ignore();

		//Make sure the Myo is locked when we exit
		if (collector.theMyo)
			lockMyo();

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

		//Make sure the Myo is locked when we exit
		if (collector.theMyo)
			lockMyo();

		//Clean up message loop thread
		//Signal it to exit
		exitFlag = true;
		//Post a null message so GetMessage() returns
		PostThreadMessage(threadId, WM_NULL, NULL, NULL);
		WaitForSingleObject(msgThread, INFINITE);
		CloseHandle(msgThread);
		return 1;
	}

	//Make sure the Myo is locked when we exit
	if (collector.theMyo)
		lockMyo();
	//Clean up message loop thread
	//Signal it to exit
	exitFlag = true;
	//Post a null message so GetMessage() returns
	PostThreadMessage(threadId, WM_NULL, NULL, NULL);
	WaitForSingleObject(msgThread, INFINITE);
	CloseHandle(msgThread);
    return 0;
}

