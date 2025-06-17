#include "../service/main_service.hpp"
#include "server.hpp"
#include <grpc++/grpc++.h>
#ifdef __APPLE__
#include <arpa/inet.h>
#include <ifaddrs.h>
#include <netinet/in.h>
#include <sys/types.h>
#endif

std::string get_local_address()
{
#ifdef __APPLE__
  struct ifaddrs *ifAddrStruct = nullptr;
  struct ifaddrs *ifa = nullptr;
  void *tmpAddrPtr = nullptr;
  getifaddrs(&ifAddrStruct);
  std::string ipAddr = "unknown";

  for (ifa = ifAddrStruct; ifa != nullptr; ifa = ifa->ifa_next)
  {
    if (!ifa->ifa_addr)
    {
      continue;
    }
    if (ifa->ifa_addr->sa_family == AF_INET)
    { // Check if it is an IPv4 address
      tmpAddrPtr = &((struct sockaddr_in *)ifa->ifa_addr)->sin_addr;
      char addressBuffer[INET_ADDRSTRLEN];
      inet_ntop(AF_INET, tmpAddrPtr, addressBuffer, INET_ADDRSTRLEN);
      if (strncmp(addressBuffer, "192.", 4) == 0)
      {
        ipAddr = addressBuffer;
        break;
      }
    }
  }

  if (ifAddrStruct != nullptr)
  {
    freeifaddrs(ifAddrStruct);
  }

  return ipAddr;
#else
  return std::string{"unknown"};
#endif
}

void RemoteServer::serve(const unsigned int port)
{
  std::string server_address{std::format("0.0.0.0:{}", port)};
  MainServiceImpl service;

  ServerBuilder builder;
  builder.AddListeningPort(server_address, grpc::InsecureServerCredentials());
  builder.RegisterService(&service);
  std::unique_ptr<grpc::Server> server{builder.BuildAndStart()};
  std::string ipAddr = get_local_address();
  Logger::info(std::format("Started server at address: {}:{}", ipAddr, port));
  // server->Wait();
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
