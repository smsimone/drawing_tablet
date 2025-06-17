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

# Imposta CMAKE_PREFIX_PATH per aiutare find_package a trovare le dipendenze di Homebrew
if(APPLE)
    # Cerca il percorso di openssl@3 installato da Homebrew
    execute_process(COMMAND brew --prefix openssl@3 OUTPUT_VARIABLE OPENSSL_HOMEBREW_PREFIX OUTPUT_STRIP_TRAILING_WHITESPACE)
    if(EXISTS "${OPENSSL_HOMEBREW_PREFIX}/lib")
        message(STATUS "Using Homebrew OpenSSL@3 prefix: ${OPENSSL_HOMEBREW_PREFIX}")
        # Aggiungi i percorsi specifici di OpenSSL@3 al CMAKE_PREFIX_PATH
        list(APPEND CMAKE_PREFIX_PATH "${OPENSSL_HOMEBREW_PREFIX}")
        # Aggiungi anche i percorsi a CMAKE_LIBRARY_PATH e CMAKE_INCLUDE_PATH per find_library/find_path
        list(APPEND CMAKE_LIBRARY_PATH "${OPENSSL_HOMEBREW_PREFIX}/lib")
        list(APPEND CMAKE_INCLUDE_PATH "${OPENSSL_HOMEBREW_PREFIX}/include")
    else()
        message(WARNING "Homebrew OpenSSL@3 not found at ${OPENSSL_HOMEBREW_PREFIX} or lib directory is missing.")
    endif()

    # Assicurati che il percorso generale di Homebrew sia comunque presente
    list(APPEND CMAKE_PREFIX_PATH "/opt/homebrew")
    list(APPEND CMAKE_LIBRARY_PATH "/opt/homebrew/lib")
    list(APPEND CMAKE_INCLUDE_PATH "/opt/homebrew/include")
endif()

# Importante: find_package(OpenSSL) deve avvenire PRIMA di find_package(gRPC)
# perché gRPC dipende da OpenSSL.
find_package(OpenSSL REQUIRED)
message(STATUS "Found OpenSSL: ${OpenSSL_FOUND}")
message(STATUS "OpenSSL Include Dirs: ${OpenSSL_INCLUDE_DIRS}")
message(STATUS "OpenSSL Libraries: ${OpenSSL_LIBRARIES}")

# Verifica se i target OpenSSL::SSL e OpenSSL::Crypto sono stati creati da find_package
if(TARGET OpenSSL::SSL)
    message(STATUS "OpenSSL::SSL target found by find_package.")
else()
    message(WARNING "OpenSSL::SSL target NOT found by find_package. This might cause issues with gRPC.")
endif()
if(TARGET OpenSSL::Crypto)
    message(STATUS "OpenSSL::Crypto target found by find_package.")
else()
    message(WARNING "OpenSSL::Crypto target NOT found by find_package. This might cause issues with gRPC.")
endif()


find_package(Protobuf CONFIG REQUIRED)
find_package(gRPC REQUIRED) # Ora gRPC può trovare i target OpenSSL se sono stati creati


include_directories(${Protobuf_INCLUDE_DIRS})
include_directories(${gRPC_INCLUDE_DIRS})
include_directories(${OpenSSL_INCLUDE_DIRS}) # Assicurati di includere gli header di OpenSSL

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
    # OpenSSL::SSL # NON aggiungere qui, gRPC dovrebbe già gestirlo.
    # Non c'è bisogno di ${OpenSSL_LIBRARIES} qui se usi i target.
)

target_include_directories(drawing_tablet PRIVATE ${CMAKE_CURRENT_BINARY_DIR})
if(ENABLE_UPNP)
	#list(APPEND LIBRARIES miniupnpc)
	message(STATUS "Included UPnP library")
endif()


# Collega direttamente ai target di gRPC e Protobuf.
# gRPC::grpc porta con sé le sue dipendenze (come OpenSSL::SSL e OpenSSL::Crypto)
# se sono state correttamente definite.
target_link_libraries(drawing_tablet PRIVATE 
    ${LIBRARIES} # Le librerie custom/frameworks MacOS
    Protobuf::protobuf # Usa il target di Protobuf
    gRPC::grpc++
    gRPC::grpc++_reflection
    gRPC::grpc
)
