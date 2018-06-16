#include <jni.h>
#include "myobot_bridge_Myo.h"
#include <iostream>
#include <myo/myo.hpp>

JNIEXPORT void JNICALL Java_myobot_bridge_Myo_hello(JNIEnv *jniEnv, jclass jClass) {
	std::cout << "hello" << std::endl;
}