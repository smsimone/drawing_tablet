#ifndef CURSOR_UTILS_H
#define CURSOR_UTILS_H

#include <CoreFoundation/CoreFoundation.h>
#include <iostream>
#include <ostream>

namespace cursor {

typedef struct {
  float x;
  float y;
} position;

inline std::ostream &operator<<(std::ostream &stream,
                                const cursor::position &pos) {
  stream << "(" << pos.x << ", " << pos.y << ")";
  return stream;
}

void move(const position &pos);
void click_down();
void click_up();

position get_current_position();
} // namespace cursor

namespace screen {
typedef struct {
  int height;
  int width;
} dimensions;

dimensions get_screen_dimensions();
} // namespace screen

#endif // CURSOR_UTILS_H
