#include "service/main_service.hpp"
#include <CoreGraphics/CoreGraphics.h>
#include <grpc++/grpc++.h>
#include <iostream>

using grpc::Server;
using grpc::ServerBuilder;

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
