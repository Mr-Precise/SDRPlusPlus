cmake_minimum_required(VERSION 3.13)
project(sdrpp_core)

if(USE_INTERNAL_LIBCORRECT)
    add_subdirectory("libcorrect/")
endif(USE_INTERNAL_LIBCORRECT)

if(USE_BUNDLE_DEFAULTS)
    add_definitions(-DIS_MACOS_BUNDLE)
endif(USE_BUNDLE_DEFAULTS)

if(USE_LINUX_PORTABLE)
    add_definitions(-DIS_LINUX_PORTABLE)
endif(USE_LINUX_PORTABLE)

## glfw settings
set(GLFW_BUILD_DOCS OFF CACHE BOOL "" FORCE)
set(GLFW_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(GLFW_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(GLFW_INSTALL OFF CACHE BOOL "" FORCE)
set(GLFW_USE_HYBRID_HPG ON CACHE BOOL "" FORCE)
if(MINGW)
    set(GLFW_LIBRARY_TYPE "SHARED" CACHE STRING "" FORCE)
elseif()
    set(GLFW_LIBRARY_TYPE "STATIC" CACHE STRING "" FORCE)
endif()

# add glfw
if(NOT EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/vendor/glfw/CMakeLists.txt" )
    message(FATAL_ERROR "GLFW submodule not found. Did you forget to git clone recursively?\nFix with: git submodule update --init")
endif()
add_subdirectory(vendor/glfw)

if(NOT TARGET glfw)
    message(FATAL_ERROR "GLFW submodule was properly added?\nMaybe fix with: git submodule foreach --recursive git reset --hard")
endif()

set(GLFW3_INCLUDE_DIRS "vendor/glfw/include")
set(GLFW3_LIBRARIES glfw)

# Main code
file(GLOB_RECURSE SRC "src/*.cpp" "src/*.c")

add_definitions(-DSDRPP_IS_CORE)
add_definitions(-DFLOG_ANDROID_TAG="SDR++")
if(MSVC OR MINGW)
    set(CMAKE_WINDOWS_EXPORT_ALL_SYMBOLS ON)
endif()

# Configure backend sources
file(GLOB_RECURSE BACKEND_SRC "backends/glfw/*.cpp" "backends/glfw/*.c")

# Add code to dyn lib
add_library(sdrpp_core SHARED ${SRC} ${BACKEND_SRC})

# Set compiler options
target_compile_options(sdrpp_core PRIVATE ${SDRPP_COMPILER_FLAGS})
set_target_properties(sdrpp_core PROPERTIES PREFIX "")

# Set the install prefix
target_compile_definitions(sdrpp_core PUBLIC INSTALL_PREFIX="${CMAKE_INSTALL_PREFIX}")
# Set the path to install plugins given the CMake install prefix
target_compile_definitions(sdrpp_core PUBLIC INSTALL_MOD_DIR="${CMAKE_INSTALL_PREFIX}/${CMAKE_INSTALL_LIBDIR}")


# Include core headers
target_include_directories(sdrpp_core PUBLIC "src/")
target_include_directories(sdrpp_core PUBLIC "src/imgui")

# Configure backend includes and libraries
target_include_directories(sdrpp_core PUBLIC "backends/glfw")
target_include_directories(sdrpp_core PUBLIC "backends/glfw/imgui")

if(WIN32 OR MINGW)
    find_package(PkgConfig)
    # Volk
    find_package(Volk)
    # target_link_libraries(sdrpp_core PUBLIC volk cpu_features)
    target_link_libraries(sdrpp_core PUBLIC Volk::volk)

    # GLFW3
    # find_package(glfw3 CONFIG REQUIRED)
    target_link_libraries(sdrpp_core PUBLIC ${GLFW3_LIBRARIES})
    target_include_directories(sdrpp_core PUBLIC ${GLFW3_INCLUDE_DIRS})

    # OpenGL
    find_package(OpenGL REQUIRED)
    target_link_libraries(sdrpp_core PUBLIC OpenGL::GL)

    # FFTW3
    pkg_check_modules(FFTW fftw3f REQUIRED)
    target_link_libraries(sdrpp_core PUBLIC ${FFTW_LIBRARIES})

    # WinSock2
    target_link_libraries(sdrpp_core PUBLIC wsock32 ws2_32 iphlpapi)

    # ZSTD
    find_package(zstd CONFIG REQUIRED)
    target_link_libraries(sdrpp_core PUBLIC zstd::libzstd_shared)
else()
    find_package(PkgConfig)
    # pkg_check_modules(GLFW3 REQUIRED glfw3)
    target_include_directories(sdrpp_core PUBLIC ${GLFW3_INCLUDE_DIRS})
    # target_link_directories(sdrpp_core PUBLIC ${GLFW3_LIBRARY_DIRS})
    target_link_libraries(sdrpp_core PUBLIC ${GLFW3_LIBRARIES})
endif()

# Link to libcorrect
if(USE_INTERNAL_LIBCORRECT)
    target_include_directories(sdrpp_core PUBLIC "libcorrect/include")
    target_link_libraries(sdrpp_core PUBLIC correct_static)
endif(USE_INTERNAL_LIBCORRECT)

if(OPT_OVERRIDE_STD_FILESYSTEM)
    target_include_directories(sdrpp_core PUBLIC "std_replacement")
endif(OPT_OVERRIDE_STD_FILESYSTEM)

if(WIN32 OR MINGW)
    # WinSock2
    target_link_libraries(sdrpp_core PUBLIC wsock32 ws2_32 iphlpapi)
else()
    find_package(PkgConfig)
    find_package(OpenGL REQUIRED)

    pkg_check_modules(FFTW3 REQUIRED fftw3f)
    pkg_check_modules(VOLK REQUIRED volk)
    # pkg_check_modules(GLFW3 REQUIRED glfw3)
    pkg_check_modules(LIBZSTD REQUIRED libzstd)

    target_include_directories(sdrpp_core PUBLIC
        ${OPENGL_INCLUDE_DIRS}
        ${FFTW3_INCLUDE_DIRS}
        ${GLFW3_INCLUDE_DIRS}
        ${VOLK_INCLUDE_DIRS}
        ${LIBZSTD_INCLUDE_DIRS}
    )
    
    target_link_directories(sdrpp_core PUBLIC
        ${OPENGL_LIBRARY_DIRS}
        ${FFTW3_LIBRARY_DIRS}
        # ${GLFW3_LIBRARY_DIRS}
        ${VOLK_LIBRARY_DIRS}
        ${LIBZSTD_LIBRARY_DIRS}
    )

    target_link_libraries(sdrpp_core PUBLIC
        ${OPENGL_LIBRARIES}
        ${FFTW3_LIBRARIES}
        ${GLFW3_LIBRARIES}
        ${VOLK_LIBRARIES}
        ${LIBZSTD_LIBRARIES}
    )

    if(NOT USE_INTERNAL_LIBCORRECT)
        pkg_check_modules(CORRECT REQUIRED libcorrect)
        target_include_directories(sdrpp_core PUBLIC ${CORRECT_INCLUDE_DIRS})
        target_link_directories(sdrpp_core PUBLIC ${CORRECT_LIBRARY_DIRS})
        target_link_libraries(sdrpp_core PUBLIC ${CORRECT_LIBRARIES})
    endif(NOT USE_INTERNAL_LIBCORRECT)

    if(${CMAKE_SYSTEM_NAME} MATCHES "Linux")
        target_link_libraries(sdrpp_core PUBLIC stdc++fs)
    endif()

endif()

set(CORE_FILES ${RUNTIME_OUTPUT_DIRECTORY} PARENT_SCOPE)

# Copy sdrpp_core library to build dir
if((${CMAKE_SYSTEM_NAME} MATCHES "Linux" AND USE_LINUX_PORTABLE) OR MINGW)
    add_custom_target(copy-sdrpp_core ALL
        COMMAND ${CMAKE_COMMAND} -E copy
            "$<TARGET_FILE_DIR:sdrpp_core>/sdrpp_core${CMAKE_SHARED_LIBRARY_SUFFIX}"
            "${BUILD_OUTPUT_DIR}"
    )
endif()

# Copy glfw library to build dir
if(MINGW)
    add_custom_target(copy-lib_glfw ALL
        COMMAND ${CMAKE_COMMAND} -E copy
            "$<TARGET_FILE_DIR:glfw>/glfw3${CMAKE_SHARED_LIBRARY_SUFFIX}"
            "${BUILD_OUTPUT_DIR}"
    )
    add_dependencies(copy-lib_glfw glfw)
endif()

# Install directives
install(TARGETS sdrpp_core DESTINATION ${CMAKE_INSTALL_LIBDIR})
