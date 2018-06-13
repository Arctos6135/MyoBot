#pragma once

#include "stdafx.h"

#define MESSAGE_SIZE 6
#define ACTION_SIZE 2
#define PARAM_SIZE 4

extern void setupSockets(SOCKET&, SOCKET&);
extern void cleanupSockets(SOCKET&, SOCKET&);
extern void sendAction(SOCKET&, uint16_t, const unsigned char*);