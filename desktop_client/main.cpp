#include "logger.h"
#include "service/main_service.hpp"
#include <CoreGraphics/CoreGraphics.h>
#include <grpc++/grpc++.h>

using grpc::Server;
using grpc::ServerBuilder;

int main() {
  std::string server_address{"0.0.0.0:50051"};
  MainServiceImpl service;

  ServerBuilder builder;
  builder.AddListeningPort(server_address, grpc::InsecureServerCredentials());
  builder.RegisterService(&service);
  std::unique_ptr<Server> server{builder.BuildAndStart()};

  Logger::info(std::format("Server listening on: {}", server_address));
  server->Wait();

  return 0;
}
