cmake_minimum_required(VERSION 3.30)
project(drawing_tablet)
include(ExternalProject)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_COMPILER "clang++")
set(protobuf_MODULE_COMPATIBLE TRUE)

file(GLOB_RECURSE SOURCES "*.cpp")
list(REMOVE_ITEM SOURCES "${CMAKE_BINARY_DIR}/CMakeFiles/4.0.0/CompilerIdCXX/CMakeCXXCompilerId.cpp")

option(ENABLE_UPNP "Enabled UPnP protocol" OFF)

#### GRPC and PROTOBUF
find_package(Protobuf CONFIG REQUIRED)
find_package(gRPC REQUIRED)

include_directories(${Protobuf_INCLUDE_DIRS})
include_directories(${gRPC_INCLUDE_DIRS})

file(COPY ${CMAKE_CURRENT_SOURCE_DIR}/proto DESTINATION ${CMAKE_CURRENT_BINARY_DIR})
set(PROTO_FILES proto/MainService.proto)
set(GENERATED_SRCS
    ${CMAKE_CURRENT_BINARY_DIR}/proto/MainService.pb.cc
    ${CMAKE_CURRENT_BINARY_DIR}/proto/MainService.grpc.pb.cc)

set(gRPC_CPP_PLUGIN_EXECUTABLE "/opt/homebrew/bin/grpc_cpp_plugin")

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
    VERBATIM
)

### UPnP dependency
if(ENABLE_UPNP)
	message(STATUS "Fetching upnp library")
	ExternalProject_Add(
		miniupnpc
		PREFIX ${CMAKE_BINARY_DIR}/external/miniupnpc
		GIT_REPOSITORY https://github.com/miniupnp/miniupnp.git
		GIT_TAG master
		CMAKE_ARGS -DCMAKE_INSTALL_PREFIX=${CMAKE_BINARY_DIR}/miniupnpc-install
		BUILD_COMMAND make -j 12
		INSTALL COMMAND make install
	)
	add_library(miniupnpc STATIC IMPORTED)
endif()

add_executable(drawing_tablet ${SOURCES} ${GENERATED_SRCS})
set(LIBRARIES
    "-framework CoreGraphics"
    "-framework CoreFoundation"
    "-framework IOKit"
    ${Protobuf_LIBRARIES}
    gRPC::grpc++
    gRPC::grpc++_reflection
    gRPC::grpc
)

target_include_directories(drawing_tablet PRIVATE ${CMAKE_CURRENT_BINARY_DIR})
if(ENABLE_UPNP)
	#list(APPEND LIBRARIES miniupnpc)
	message(STATUS "Included UPnP library")
endif()


target_link_libraries(drawing_tablet PRIVATE ${LIBRARIES})
