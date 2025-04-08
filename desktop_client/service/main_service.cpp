
#include "main_service.hpp"
#include "../cursor_utils/cursor_utils.h"
#include "../logger.h"
#include <format>

Status MainServiceImpl::GetScreenSize(ServerContext *context,
                                      const Empty *request,
                                      ScreenSize *response) {
  Logger::info("Received getScreenSize request");
  screen::dimensions dims = screen::get_screen_dimensions();
  response->set_height(dims.height);
  response->set_width(dims.width);
  return Status::OK;
}

Status MainServiceImpl::OnCursorPosition(ServerContext *context,
                                         const CursorPosition *request,
                                         Empty *response) {
  Coordinates coords = request->coordinates();
  Logger::info(std::format("Moved to ({}, {}) with click: {}", coords.x(),
                           coords.y(), request->clicking()));

  cursor::move(cursor::position{.x = coords.x(), .y = coords.y()});

  /// if started clicking now -> click_down
  if (!is_clicking && request->clicking()) {
    cursor::click_down();
  } else if (is_clicking && !request->clicking()) {
    cursor::click_up();
  }
  is_clicking = request->clicking();

  return Status::OK;
}
