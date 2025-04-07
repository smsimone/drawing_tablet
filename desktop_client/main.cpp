#include "cursor_utils/cursor_utils.h"
#include "proto/MainService.grpc.pb.h"
#include <CoreGraphics/CoreGraphics.h>
#include <grpc++/grpc++.h>
#include <iostream>

using namespace cursor;
using google::protobuf::Empty;
using grpc::Server;
using grpc::ServerBuilder;
using grpc::ServerContext;
using grpc::Status;
using it::smaso::drawingtablet::Coordinates;
using it::smaso::drawingtablet::CursorPosition;
using it::smaso::drawingtablet::MainService;
using it::smaso::drawingtablet::ScreenSize;

class MainServiceImpl final : public MainService::Service {
  Status GetScreenSize(ServerContext *context, const Empty *request,
                       ScreenSize *response) override {
    std::cout << "Received getScreenSize request" << std::endl;
    screen::dimensions dims = screen::get_screen_dimensions();
    response->set_height(dims.height);
    response->set_width(dims.width);
    return Status::OK;
  }

  Status OnCursorPosition(ServerContext *context, const CursorPosition *request,
                          Empty *response) override {
    Coordinates coords = request->coordinates();
    std::cout << "Moved to (" << coords.x() << ", " << coords.y()
              << ") with click: " << request->clicking() << std::endl;

    cursor::move(cursor::position{.x = coords.x(), .y = coords.y()},
                 request->clicking());

    return Status::OK;
  }
};

int main() {
  std::string server_address{"0.0.0.0:50051"};
  MainServiceImpl service;

  ServerBuilder builder;
  builder.AddListeningPort(server_address, grpc::InsecureServerCredentials());
  builder.RegisterService(&service);
  std::unique_ptr<Server> server{builder.BuildAndStart()};

  std::cout << "Server listening on " << server_address << std::endl;
  server->Wait();

  return 0;
}
