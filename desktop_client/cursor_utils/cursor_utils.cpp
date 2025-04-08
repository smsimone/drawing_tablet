#include "cursor_utils.h"
#include <CoreFoundation/CFBase.h>
#include <CoreFoundation/CFCGTypes.h>
#include <CoreGraphics/CGEvent.h>
#include <CoreGraphics/CGEventTypes.h>
#include <CoreGraphics/CGGeometry.h>
#include <cstddef>

namespace cursor {
position get_current_position() {
#ifdef __APPLE__
  CGEventRef event = CGEventCreate(NULL);
  CGPoint point = CGEventGetLocation(event);
  CFRelease(event);
  return position{.x = static_cast<float>(point.x),
                  .y = static_cast<float>(point.y)};
#else
  throw runtime_error("Not implemented");
#endif
}

void move(const position &pos, const bool &click_down) {
#ifdef __APPLE__
  CGEventType mouseEvent =
      click_down ? kCGEventOtherMouseDragged : kCGEventMouseMoved;
  CGEventRef event = CGEventCreateMouseEvent(
      NULL, mouseEvent, CGPointMake(pos.x, pos.y), kCGMouseButtonLeft);
  CGPoint point = CGPointMake(pos.x, pos.y);
  CGEventSetLocation(event, point);
  CGEventPost(kCGHIDEventTap, event);
  CFRelease(event);
#else
  throw runtime_error("Not implemented");
#endif
}

} // namespace cursor

namespace screen {

dimensions get_screen_dimensions() {
#ifdef __APPLE__
  CGDirectDisplayID displayId = CGMainDisplayID();
  size_t width = CGDisplayPixelsWide(displayId);
  size_t height = CGDisplayPixelsHigh(displayId);

  return dimensions{.height = static_cast<int>(height),
                    .width = static_cast<int>(width)};
#else
  throw runtime_error("Not implemented");
#endif
}
} // namespace screen
