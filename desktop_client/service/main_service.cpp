
#include "main_service.hpp"
#include "../cursor_utils/cursor_utils.h"
#include "../logger.h"
#include <chrono>
#include <format>
#include <thread>

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

  if (!is_clicking && request->clicking()) {
    cursor::click_down();
    Logger::info("Sending mouse down");
  } else if (is_clicking && !request->clicking()) {
    cursor::click_up();
    Logger::info("Sending mouse up");
  }
  is_clicking = request->clicking();

  cursor::position coords = cursor::position{request->x(), request->y()};
  Logger::info(std::format("Moved to ({}, {}) with click: {}", coords.x,
                           coords.y, request->clicking()));

  cursor::move(coords);

  return Status::OK;
}
