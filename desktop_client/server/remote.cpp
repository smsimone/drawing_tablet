#include "server.hpp"
#include "../service/main_service.hpp"
#include <grpc++/grpc++.h>

void RemoteServer::serve(const unsigned int port)
{
  std::string server_address{std::format("0.0.0.0:{}", port)};
  MainServiceImpl service;

  ServerBuilder builder;
  builder.AddListeningPort(server_address, grpc::InsecureServerCredentials());
  builder.RegisterService(&service);
  std::unique_ptr<grpc::Server> server{builder.BuildAndStart()};
  Logger::info(std::format("Started server at address: {}", server_address));
  this->server.swap(server);
  Logger::info("Set instance into RemoteServer");
  initialized = true;
}

void RemoteServer::wait()
{
  if (!this->initialized)
    throw std::runtime_error("Remote server was not initialized");
  Logger::info("Waiting on server termination");
  this->server->Wait();
}