#include "logger.h"
#include "service/main_service.hpp"
#include <grpc++/grpc++.h>
#include "server/server.hpp"

#define USE_REMOTE false

using grpc::Server;
using grpc::ServerBuilder;

int main()
{
#if USE_REMOTE
  RemoteServer server;
  server.serve(50051);
  server.wait();
#else
  SerialServer server; 
  server.list_devices();
#endif

  return 0;
}
