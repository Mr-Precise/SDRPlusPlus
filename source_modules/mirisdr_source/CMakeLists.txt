cmake_minimum_required(VERSION 3.13)
project(mirisdr_source)

include(CheckSymbolExists)

set(SRC "src/main.cpp")

include(${SDRPP_MODULE_CMAKE})

find_package(PkgConfig)
pkg_check_modules(MIRISDR REQUIRED libmirisdr)

# check mirisdr library for compatiblity
list(APPEND CMAKE_REQUIRED_LIBRARIES ${MIRISDR_LIBRARIES})
check_symbol_exists(mirisdr_get_sample_format "mirisdr.h" HAS_MIRISDR_FUNCTION)
list(REMOVE_ITEM CMAKE_REQUIRED_LIBRARIES ${MIRISDR_LIBRARIES})

if(HAS_MIRISDR_FUNCTION)
    message(STATUS "libmirisdr compatible.")
else()
    message(FATAL_ERROR "libmirisdr uncompatible. This library is outdated.")
endif()

target_include_directories(mirisdr_source PRIVATE ${MIRISDR_INCLUDE_DIRS})
target_link_directories(mirisdr_source PRIVATE ${MIRISDR_LIBRARY_DIRS})
target_link_libraries(mirisdr_source PRIVATE ${MIRISDR_LIBRARIES})

# Include it because for some reason pkgconfig doesn't look here?
if (${CMAKE_SYSTEM_NAME} MATCHES "Darwin")
    target_include_directories(mirisdr_source PRIVATE "/usr/local/include")
endif()
