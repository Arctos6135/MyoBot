#pragma once

#include "stdafx.h"

extern bool isBigEndian();

extern void int32ToChars(int32_t, char*);
extern void int32ToCharsBigEndian(int32_t, char*);
extern void int32ToCharsLittleEndian(int32_t, char*);