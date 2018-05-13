// MyoBot_Cpp.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"
#include "socketutils.h"

#define ACT_REST (uint32_t) 0x0000
#define ACT_DRIVEFORWARD (uint32_t) 0x0001

SOCKET listenerSocket, clientSocket;


int main() {
	try {
		setupSockets(listenerSocket, clientSocket);

		cleanupSockets(listenerSocket, clientSocket);
	}
	catch (std::runtime_error e) {
		std::cerr << e.what() << std::endl;
		return 1;
	}

    return 0;
}

