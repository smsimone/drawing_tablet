
#include "main_service.hpp"
#include "../cursor_utils/cursor_utils.h"
#include <iostream>

Status MainServiceImpl::GetScreenSize(ServerContext *context,
                                      const Empty *request,
                                      ScreenSize *response) {
  std::cout << "Received getScreenSize request" << std::endl;
  screen::dimensions dims = screen::get_screen_dimensions();
  response->set_height(dims.height);
  response->set_width(dims.width);
  return Status::OK;
}

Status MainServiceImpl::OnCursorPosition(ServerContext *context,
                                         const CursorPosition *request,
                                         Empty *response) {
  Coordinates coords = request->coordinates();
  std::cout << "Moved to (" << coords.x() << ", " << coords.y()
            << ") with click: " << request->clicking() << std::endl;

  cursor::move(cursor::position{.x = coords.x(), .y = coords.y()},
               request->clicking());

  return Status::OK;
}
