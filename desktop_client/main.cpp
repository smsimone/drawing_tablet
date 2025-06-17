#include "server/server.hpp"
#include <grpc++/grpc++.h>

#define USE_REMOTE true

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
