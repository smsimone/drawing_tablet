#ifndef SERVER_H
#define SERVER_H

#include <grpc++/grpc++.h>
#include <memory>
#include "../logger.h"

class DTServer
{
};

class RemoteServer final : DTServer
{
private:
  std::unique_ptr<grpc::Server> server;
  volatile bool initialized = false;

public:
  // Serves the remote server and waits for his termination
  void serve(const unsigned int port);

  // Waits for server termination
  void wait();
};

class SerialServer final : DTServer
{

public:
  void list_devices();
};

#endif // SERVER_H
