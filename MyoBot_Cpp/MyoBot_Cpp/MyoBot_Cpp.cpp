// MyoBot_Cpp.cpp : Defines the entry point for the console application.
//

#include "stdafx.h"

#define PORT "6135"

#define ACT_REST (uint32_t) 0x0000
#define ACT_DRIVEFORWARD (uint32_t) 0x0001

#define MAKE_EXCEPTION(msg, code) std::runtime_error(std::string(msg).append(std::to_string(code)).c_str())

#pragma comment(lib, "Ws2_32.lib")

SOCKET listenerSocket, clientSocket;

void setupSockets(SOCKET& listener, SOCKET& client) {
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

void int2Chars(uint32_t i, char* out) {
	out[0] = (i & 0xF000) >> 24;
	out[1] = (i & 0x0F00) >> 16;
	out[2] = (i & 0x00F0) >> 8;
	out[3] = (i & 0x000F);
}

void sendAction(uint32_t msg, SOCKET& sock) {
	char data[4];
	int2Chars(msg, data);
	if (send(sock, data, 4, NULL) == SOCKET_ERROR) {
		throw MAKE_EXCEPTION("Failed to send: ", WSAGetLastError());
	}
}

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

