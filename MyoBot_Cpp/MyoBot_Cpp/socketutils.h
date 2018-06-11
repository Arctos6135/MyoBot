#pragma once

#include "stdafx.h"

extern void setupSockets(SOCKET&, SOCKET&);
extern void cleanupSockets(SOCKET&, SOCKET&);
extern void sendAction(SOCKET&, uint32_t, const char*);