#pragma once

#include "stdafx.h"

#define MESSAGE_SIZE 10
#define ACTION_SIZE 2
#define PARAM_SIZE 8

extern void setupSockets(SOCKET&, SOCKET&);
extern void cleanupSockets(SOCKET&, SOCKET&);
extern void sendAction(SOCKET&, uint16_t, const unsigned char*);