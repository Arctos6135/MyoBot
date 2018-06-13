#include "stdafx.h"

bool isBigEndian() {
	volatile union {
		uint32_t i;
		char c[4];
	} u = { 0x01020304 };

	return u.c[0] == 1;
}

void int32ToChars(uint32_t i, char* out) {
	out[0] = (i & 0xFF000000) >> 24;
	out[1] = (i & 0x00FF0000) >> 16;
	out[2] = (i & 0x0000FF00) >> 8;
	out[3] = (i & 0x000000FF);
}
void int16ToChars(uint16_t i, char* out) {
	out[0] = (i & 0xFF00) >> 8;
	out[1] = (i & 0x00FF);
}