cmake_minimum_required(VERSION 3.30)
project(drawing_tablet)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_COMPILER "clang++")
set(protobuf_MODULE_COMPATIBLE TRUE)

file(GLOB_RECURSE SOURCES "*.cpp")
list(REMOVE_ITEM SOURCES "${CMAKE_BINARY_DIR}/CMakeFiles/4.0.0/CompilerIdCXX/CMakeCXXCompilerId.cpp")

#### GRPC e PROTOBUF
find_package(Protobuf CONFIG REQUIRED)
find_package(gRPC REQUIRED)

if(${gRPC_FOUND})
   message(STATUS "Grpc plugin found") 
endif(${gRPC_FOUND})


include_directories(${Protobuf_INCLUDE_DIRS})
include_directories(${gRPC_INCLUDE_DIRS})

file(COPY ${CMAKE_CURRENT_SOURCE_DIR}/proto DESTINATION ${CMAKE_CURRENT_BINARY_DIR})
set(PROTO_FILES proto/MainService.proto)
set(GENERATED_SRCS
    ${CMAKE_CURRENT_BINARY_DIR}/proto/MainService.pb.cc
    ${CMAKE_CURRENT_BINARY_DIR}/proto/MainService.grpc.pb.cc)

set(gRPC_CPP_PLUGIN_EXECUTABLE "/opt/homebrew/bin/grpc_cpp_plugin")
message(STATUS "Using ${Protobuf_PROTOC_EXECUTABLE} protoc exec")
message(STATUS "Using ${gRPC_CPP_PLUGIN_EXECUTABLE} grpc plugin")

add_custom_command(
    OUTPUT ${GENERATED_SRCS}
    COMMAND ${Protobuf_PROTOC_EXECUTABLE}
    ARGS --grpc_out ${CMAKE_CURRENT_BINARY_DIR}
         --plugin=protoc-gen-grpc=${gRPC_CPP_PLUGIN_EXECUTABLE}
         --cpp_out ${CMAKE_CURRENT_BINARY_DIR}
         -I ${CMAKE_CURRENT_SOURCE_DIR}
         ${PROTO_FILES}
    DEPENDS ${PROTO_FILES}
    COMMENT "Running gRPC C++ protocol buffer compiler on ${PROTO_FILES}"
    VERBATIM)

add_executable(drawing_tablet ${SOURCES} ${GENERATED_SRCS})
target_link_libraries(drawing_tablet
    "-framework CoreGraphics"
    "-framework CoreFoundation"
    ${Protobuf_LIBRARIES}
    gRPC::grpc++
    gRPC::grpc++_reflection
    gRPC::grpc)

target_include_directories(drawing_tablet PRIVATE ${CMAKE_CURRENT_BINARY_DIR})
