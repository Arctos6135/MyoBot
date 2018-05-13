#pragma once

#include "stdafx.h"

extern void setupSockets(SOCKET&, SOCKET&);
extern void cleanupSockets(SOCKET&, SOCKET&);
extern void sendAction(uint32_t, SOCKET&);