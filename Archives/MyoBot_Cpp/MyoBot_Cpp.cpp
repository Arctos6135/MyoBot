// MyoBot_Cpp.cpp : Main file for the Myo data reader.
// IMPORTANT: The Myo must be worn with the Thalmic Labs logo downwards; otherwise the controls will be flipped.

#include "stdafx.h"
#include "socketutils.h"
#include "bitops.h"

#pragma warning(disable: 4244)
#pragma warning(disable: 4838)

/*
	Each message that this program sends over to the bridge program consists of 10 bytes.

	The first 2 bytes form a 16 bit integer that is the action code. This dicates which action is to be performed.

	The last 8 bytes are the 'parameters' of the action. These bytes can represent anything and 
	can vary according to action.
*/
//Action code set
//Param: None
#define ACT_REST (uint16_t) 0x0000
//Param: First two bytes store an unsigned 16 bit integer representing how much to turn (big endian),
//Last bit of the third byte stores the turn direction (1 for left and 0 for right)
//Second last bit of the third byte is whether the intake should also be run
//Third last bit of the third byte is the direction of the intake (0 for in, 1 for out)
//Fourth last bit of the third byte is whether a drive speed is being provided (to keep compatibility with older controls)
//Fourth and fifth bytes store an unsigned 16 bit integer representing the drive speed
#define ACT_DRIVEFORWARD (uint16_t) 0x0001
//Unused in the current version; retained for compatibility
#define ACT_TURNLEFT (uint16_t) 0x0002
//Unused in the current version; retained for compatibility
#define ACT_TURNRIGHT (uint16_t) 0x0003 
//Param: First two bytes store an unsigned 16 bit integer representing how much to turn (big endian),
//Last bit of the third byte stores the turn direction (1 for left and 0 for right)
//Second last bit of the third byte is whether the intake should also be run
//Third last bit of the third byte is the direction of the intake (0 for in, 1 for out)
//Fourth last bit of the third byte is whether a drive speed is being provided (to keep compatibility with older controls)
//Fourth and fifth bytes store an unsigned 16 bit integer representing the drive speed
#define ACT_DRIVEBACK (uint16_t) 0x0004
//Param: First two bytes store an unsigned 16 bit integer representing the speed (big endian)
#define ACT_RAISEELEVATOR (uint16_t) 0x0005 
//Param: First two bytes store an unsigned 16 bit integer representing the speed (big endian)
#define ACT_LOWERELEVATOR (uint16_t) 0x0006 
//Param: None
#define ACT_INTAKE (uint16_t) 0x0007
//Param: None
#define ACT_OUTTAKE (uint16_t) 0x0008

//A null parameter message. Default value to be sent if the action does not use parameters.
const unsigned char PARAM_NULL[PARAM_SIZE] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };

//How many times data is sent per second
#define UPDATE_FREQUENCY 10

#define PI 3.14159265359f

float toDegrees(float);
std::string toStringRound(float, size_t);
#define _DISP(n) toStringRound(toDegrees(n), 0)

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
enum class MyoUnlockMode {
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

		//Keep part of the normal behavior of the Myo
		//No matter what unlock mode, if we recognize a gesture, unlock until told otherwise
		if (pose != myo::Pose::unknown && pose != myo::Pose::rest) {
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

SingleMyoDataCollector collector = SingleMyoDataCollector();
inline void lockMyo() {
	if (collector.theMyo)
		collector.theMyo->lock();
}
inline void unlockMyo() {
	if (collector.theMyo)
		collector.theMyo->unlock(myo::Myo::unlockHold);
}
inline bool isMyoUnlocked() {
	return collector.isUnlocked;
}
inline void vibrateMyo() {
	if (collector.theMyo)
		collector.theMyo->notifyUserAction();
}

/*
	The following functions deal with the native Windows stuff, namely the keyboard hook.
*/
//Because our program is multi-threaded (see below), we must keep an atomic boolean flag variable to indicate when we are exiting.
std::atomic<bool> exitFlag;


enum class ControlsMode {
	CLASSIC,
	VERSION_2_0,
	VERSION_2_1,
};
std::string controlsModeName(ControlsMode mode) {
	switch (mode) {
	case ControlsMode::CLASSIC:
		return "Classic";
	case ControlsMode::VERSION_2_0:
		return "2.0";
	case ControlsMode::VERSION_2_1:
		return "2.1";
	default: return "Unknown";
	}
}
ControlsMode controlsMode = ControlsMode::VERSION_2_1;
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
		//Alt+O: Initialize orientation/update reference orientation
		case 'O':
			collector.setRefOrientation(collector.orientationRaw);
			std::cout << "\nReference orientation updated: roll=" << _DISP(collector.refRoll) << " pitch=" << _DISP(collector.refPitch) << " yaw=" << _DISP(collector.refYaw) << std::endl;
			break;
		//Alt+I: Invert Euler angles (in case the armband is worn backwards)
		case 'I':
			collector.invertAngles = !collector.invertAngles;
			std::cout << "\nOrientation angles inverted." << std::endl;
			break;
		//Alt+C: Change controls mode
		case 'C':
			switch (controlsMode) {
			case ControlsMode::CLASSIC:
				controlsMode = ControlsMode::VERSION_2_0;
				break;
			case ControlsMode::VERSION_2_0:
				controlsMode = ControlsMode::VERSION_2_1;
				break;
			case ControlsMode::VERSION_2_1:
				controlsMode = ControlsMode::CLASSIC;
				break;
			default: controlsMode = ControlsMode::VERSION_2_1;
			}
			std::cout << "\nControls Mode Updated. Please update reference orientation to match if necessary." << std::endl;
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

//This console routine handler makes sure that when our program exits, the Myo is locked.
extern SOCKET clientSocket, listenerSocket;
BOOL WINAPI ConsoleHandler(DWORD dwCtrlType) {
	switch (dwCtrlType) {
	//Handle exit via Ctrl+C, Ctrl+Break, and the close button
	//No need to worry about logoff and shutdown
	case CTRL_C_EVENT:
	case CTRL_BREAK_EVENT:
	case CTRL_CLOSE_EVENT:
		if (collector.theMyo)
			lockMyo();
		cleanupSockets(clientSocket, listenerSocket);
		break;
	default: return FALSE;
	}
	return TRUE;
}

inline float toDegrees(float radians) {
	return radians * (180 / PI);
}
inline std::string toStringRound(float f, size_t decimals) {
	std::string s = std::to_string(f);
	return s.substr(0, decimals == 0 ? s.find('.') : s.find('.') + decimals + 1);
}

enum class DriveDirection {
	FORWARDS,
	BACKWARDS
};
DriveDirection driveDirection;
SOCKET listenerSocket, clientSocket;
int main(int argc, char** argv) {

	SetConsoleTitle(_T("MyoBot Myo Reader Program"));
	exitFlag = false;
	//Add our console routine handler
	SetConsoleCtrlHandler(&ConsoleHandler, TRUE);
	std::cout << std::fixed << std::setprecision(1);

	//ID of our message loop thread
	unsigned int threadId;

	std::cout << "Creating Additional Thread..." << std::endl;
	HANDLE msgThread = (HANDLE)_beginthreadex(NULL, 0, &messageLoopThread, &exitFlag, 0, &threadId);
	std::cout << "Thread creation successful." << std::endl;

	try {

		std::cout << "Connecting to Myo Hub..." << std::endl;
		myo::Hub hub("org.usfirst.frc.team6135.MyoBot_Cpp");

		std::cout << "Waiting for Myo..." << std::endl;
		myo::Myo* myo = hub.waitForMyo(10000);
		if (!myo) {
			throw std::runtime_error("Unable to find Myo");
		}
		std::cout << "Myo found!" << std::endl;
		hub.addListener(&collector);
		std::cout << "Warning: Please initialize the reference orientation first." << std::endl;

		std::cout << "Creating sockets..." << std::endl;
		setupSockets(listenerSocket, clientSocket);

		size_t lastStatusLen = 0;
		uint16_t lastAction = ACT_REST;

		bool doubleTapping = false;
		while (!exitFlag) {

			hub.run(1000 / UPDATE_FREQUENCY);

			if (!collector.active)
				break;

			//Initialize values for our default action and null param
			uint16_t action = ACT_REST;
			const unsigned char* param = PARAM_NULL;

			//Send data only if the Myo is on arm and unlocked.
			if (collector.onArm && collector.isUnlocked) {

				if (controlsMode == ControlsMode::VERSION_2_1) {
					if (collector.currentPose == myo::Pose::doubleTap && !doubleTapping) {
						driveDirection = driveDirection == DriveDirection::FORWARDS ? DriveDirection::BACKWARDS : DriveDirection::FORWARDS;
						doubleTapping = true;
						vibrateMyo();
					}
					else if(collector.currentPose != myo::Pose::doubleTap) {
						doubleTapping = false;

						if (collector.pitch >= -PI / 4) {
							action = driveDirection == DriveDirection::FORWARDS ? ACT_DRIVEFORWARD : ACT_DRIVEBACK;

							unsigned char paramData[PARAM_SIZE] = { 0x00 };

							float f1 = min(PI / 2, collector.pitch + PI / 4) / (PI / 2);
							uint16_t driveSpeed = static_cast<uint16_t>(floorf(f1 * 0xFFFF));
							paramData[3] = driveSpeed >> 8;
							paramData[4] = driveSpeed & 0xFF;

							uint8_t flags = 0x00;
							flags |= 0b0000'1000;
							
							if (abs(collector.yaw) >= PI / 18) {
								if (collector.yaw >= 0) {
									flags |= 0b0000'0001;
								}
								
								float f2 = min(7 * PI / 36, abs(collector.yaw) - PI / 18) / (7 * PI / 36);
								uint16_t turnSpeed = static_cast<uint16_t>(floorf(f2 * 0xFFFF));
								paramData[0] = turnSpeed >> 8;
								paramData[1] = turnSpeed & 0xFF;
							}

							if (collector.currentPose == myo::Pose::fist) {
								flags |= 0b0000'0010;
							}
							else if (collector.currentPose == myo::Pose::fingersSpread) {
								flags |= 0b0000'0110;
							}

							paramData[2] = flags;
							param = paramData;
						}
						else if (collector.currentPose == myo::Pose::fist) {
							action = ACT_INTAKE;
						}
						else if (collector.currentPose == myo::Pose::fingersSpread) {
							action = ACT_OUTTAKE;
						}
						else if (collector.currentPose == myo::Pose::waveOut) {
							action = ACT_RAISEELEVATOR;
							unsigned char paramData[PARAM_SIZE] = { 0xFF, 0xFF };
							param = paramData;
						}
						else if (collector.currentPose == myo::Pose::waveIn) {
							action = ACT_LOWERELEVATOR;
							unsigned char paramData[PARAM_SIZE] = { 0xFF, 0xFF };
							param = paramData;
						}

						if (action != lastAction && action != ACT_REST) {
							vibrateMyo();
						}
						lastAction = action;

						sendAction(clientSocket, action, param);
					}
				}
				else if (controlsMode == ControlsMode::VERSION_2_0) {
					//If the pose is unknown, then check if the arm is raised
					if (abs(collector.pitch) >= PI / 12) {
						//Same as the turning, constrain to [-60, 60] degrees
						float f = max(-PI / 3, min(PI / 3, collector.pitch));

						//Check if our pitch is more than 15 degrees
						if (abs(f) >= PI / 12) {
							action = (f >= 0 ? ACT_RAISEELEVATOR : ACT_LOWERELEVATOR);

							//Decrement by 15 degrees
							//Copy the sign of f to PI/12 to make sure we are decrementing the absolute value
							//even if f is negative.
							f -= copysignf(PI / 12, f);
							//Because we subtracted 15 degrees, the maximum absolute value that f can have
							//is now 60-15=45 degrees. Take the absolute value because direction is already in the action code.
							f = abs(f / (PI / 4));
							//Convert to integer
							uint16_t paramData = static_cast<uint16_t>(floorf(f * 0xFFFF));

							unsigned char c[PARAM_SIZE] = { paramData >> 8, paramData & 0xFF, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
							param = c;
						}
						else {
							action = ACT_REST;
						}
					}
					//Check if pose is fist or spread fingers
					else if (collector.currentPose == myo::Pose::fist || collector.currentPose == myo::Pose::fingersSpread) {
						action = collector.currentPose == myo::Pose::fist ? ACT_DRIVEFORWARD : ACT_DRIVEBACK;

						//Check if roll is greater than 10 degrees to account for human error
						if (abs(collector.roll) >= PI / 18) {
							//Take the roll of the Myo and make that the param data
							//First check the direction
							unsigned char direction = collector.roll > 0 ? 1 : 0;
							//The sign is no longer needed, so take the absolute value and constrain
							float f = min(PI / 4, abs(collector.roll));
							//Decrement by the minimum and calculate fraction
							f = (f - PI / 18) / (7 * PI / 36);
							//Multiply by 0xFFFF to get the integer representation
							uint16_t paramData = static_cast<uint16_t>(floorf(abs(f) * 0xFFFF));

							unsigned char c[PARAM_SIZE] = { paramData >> 8, paramData & 0xFF, direction, 0x00, 0x00, 0x00, 0x00, 0x00 };
							param = c;
						}
					}
					//Check if pose is intake or outtake
					else if (collector.currentPose == myo::Pose::waveIn) {
						action = ACT_INTAKE;
					}
					else if (collector.currentPose == myo::Pose::waveOut) {
						action = ACT_OUTTAKE;
					}
					else {
						action = ACT_REST;
					}

					if (action != lastAction && action != ACT_REST) {
						vibrateMyo();
					}
					lastAction = action;

					sendAction(clientSocket, action, param);
				}
				else if(controlsMode == ControlsMode::CLASSIC) {
					if (collector.currentPose == myo::Pose::fist) {
						action = ACT_DRIVEFORWARD;
					}
					else if (collector.currentPose == myo::Pose::fingersSpread) {
						action = ACT_DRIVEBACK;
					}
					else if (collector.currentPose == myo::Pose::waveIn) {
						action = collector.arm == myo::Arm::armRight ? ACT_TURNLEFT : ACT_TURNRIGHT;
					}
					else if (collector.currentPose == myo::Pose::waveOut) {
						action = collector.arm == myo::Arm::armRight ? ACT_TURNRIGHT : ACT_TURNLEFT;
					}
					else {
						action = ACT_REST;
					}

					sendAction(clientSocket, action, PARAM_NULL);
				}
				else {
					sendAction(clientSocket, ACT_REST, PARAM_NULL);
				}
			}
			else {
				//If the Myo is not on arm, or is locked, send the do nothing message to make sure everything stops.
				sendAction(clientSocket, ACT_REST, PARAM_NULL);
			}

			std::string myoState("Status: ");
			myoState += (collector.isUnlocked ? "Unlocked " : "Locked ");
			myoState += ((unlockMode == MyoUnlockMode::UNLOCK_NORMAL) ? "(Normal)" : "(Hold)");
			myoState += " Roll=" + _DISP(collector.roll);
			myoState += " Pitch=" + _DISP(collector.pitch);
			myoState += " Yaw=" + _DISP(collector.yaw);
			myoState += collector.invertAngles ? " (Inverted)" : " (Normal)";
			myoState += " Controls: ";
			myoState += controlsModeName(controlsMode);
			//Concatenate an empty string in the end to keep the length consistent
			//The \r character puts the cursor back to the beginning of the line so we can overwrite the line.
			//However if our new string is shorter than our old one, then some characters of the old string
			//will still remain.
			if (lastStatusLen <= myoState.length()) {
				std::cout << "\r" << myoState << std::flush;
			}
			else {
				std::cout << "\r" << myoState << std::string(lastStatusLen - myoState.length(), ' ') << std::flush;
			}
			lastStatusLen = myoState.length();
		}

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
		cleanupSockets(listenerSocket, clientSocket);
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
		cleanupSockets(listenerSocket, clientSocket);
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
	cleanupSockets(listenerSocket, clientSocket);
    return 0;
}

