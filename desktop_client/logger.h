#ifndef LOGGER_H
#define LOGGER_H

#include <iostream>
#include <string>

class Logger final {
public:
  inline static void info(std::string message) {
    std::cout << "[INFO] " << message << "\n";
  }

  inline static void warning(std::string message) {
    std::cout << "[WARN] " << message << "\n";
  }

  inline static void error(std::string message) {
    std::cout << "[ERROR] " << message << "\n";
  }

private:
  Logger();
  ~Logger() = default;
};

#endif // LOGGER_H
