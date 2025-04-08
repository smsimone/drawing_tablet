#ifndef MAIN_SERVICE_H
#define MAIN_SERVICE_H

#include "proto/MainService.grpc.pb.h"
#include "proto/MainService.pb.h"
#include <grpc++/grpc++.h>

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
                       ScreenSize *response) override;

  Status OnCursorPosition(ServerContext *context, const CursorPosition *request,
                          Empty *response) override;
};

#endif // MAIN_SERVICE_H
