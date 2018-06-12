#include "stdafx.h"

#define PORT "6135"

#define MAKE_EXCEPTION(msg, code) std::runtime_error(std::string(msg).append(std::to_string(code)).c_str())

#pragma comment(lib, "Ws2_32.lib")

//Sets up the listener and client sockets
void setupSockets(SOCKET& listener, SOCKET& client) {
	//Most of the code in this method is from the example program provided by the Microsoft docs.
	WSADATA wsaData;
	int iResult;

	iResult = WSAStartup(MAKEWORD(2, 2), &wsaData);

	addrinfo hints;
	addrinfo *result = NULL;

	ZeroMemory(&hints, sizeof(hints));
	hints.ai_family = AF_INET;
	hints.ai_socktype = SOCK_STREAM;
	hints.ai_protocol = IPPROTO_TCP;
	hints.ai_flags = AI_PASSIVE;

	iResult = getaddrinfo(NULL, PORT, &hints, &result);
	if (iResult != 0) {
		std::runtime_error e = MAKE_EXCEPTION("getaddrinfo failed: ", WSAGetLastError());
		WSACleanup();
		throw e;
	}

	listener = INVALID_SOCKET;
	listener = socket(result->ai_family, result->ai_socktype, result->ai_protocol);

	if (listener == INVALID_SOCKET) {
		std::runtime_error e = MAKE_EXCEPTION("Failed to create listener socket: ", WSAGetLastError());
		freeaddrinfo(result);
		WSACleanup();
		throw e;
	}

	iResult = bind(listener, result->ai_addr, result->ai_addrlen);
	if (iResult == SOCKET_ERROR) {
		std::runtime_error e = MAKE_EXCEPTION("Failed to bind: ", WSAGetLastError());
		freeaddrinfo(result);
		closesocket(listener);
		WSACleanup();
		throw e;
	}
	freeaddrinfo(result);

	if (listen(listener, 1) == SOCKET_ERROR) {
		std::runtime_error e = MAKE_EXCEPTION("Failed to listen: ", WSAGetLastError());
		closesocket(listener);
		WSACleanup();
		throw e;
	}

	std::cout << "Socket construction complete. Waiting for connections..." << std::endl;

	client = INVALID_SOCKET;
	client = accept(listener, NULL, NULL);
	if (client == INVALID_SOCKET) {
		std::runtime_error e = MAKE_EXCEPTION("Failed to accept: ", WSAGetLastError());
		closesocket(listener);
		WSACleanup();
		throw e;
	}

	std::cout << "Connection established." << std::endl;
}

//Clean up the client and listener sockets. The sockets will be closed, and Winsock will be cleaned up.
void cleanupSockets(SOCKET& listener, SOCKET& client) {
	if (shutdown(client, SD_SEND) == SOCKET_ERROR) {
		std::runtime_error e = MAKE_EXCEPTION("Failed to shutdown socket: ", WSAGetLastError());
		closesocket(client);
		closesocket(listener);
		WSACleanup();
		throw e;
	}
	closesocket(client);
	closesocket(listener);
	WSACleanup();
}

//Converts a 32-bit integer to an array of chars.
void int32ToChars(int32_t i, char* out) {
	out[0] = (i & 0xFF000000) >> 24;
	out[1] = (i & 0x00FF0000) >> 16;
	out[2] = (i & 0x0000FF00) >> 8;
	out[3] = (i & 0x000000FF);
}

//Sends an action over to the Java program
void sendAction(SOCKET& sock, const uint32_t msg, const char* param) {
	//The 8 bytes consist of 4 bytes of action code and 4 bytes of param
	//For more details refer to MyoBot_Cpp.cpp
	char data[8];
	int32ToChars(msg, data);
	for (unsigned char i = 0; i < 4; i++) {
		data[i + 4] = param[i];
	}
	if (send(sock, data, 8, NULL) == SOCKET_ERROR) {
		throw MAKE_EXCEPTION("Failed to send: ", WSAGetLastError());
	}
}