# Get needed values depending on if this is in-tree or out-of-tree
if (NOT SDRPP_CORE_ROOT)
    set(SDRPP_CORE_ROOT "@SDRPP_CORE_ROOT@")
endif ()
if (NOT SDRPP_MODULE_COMPILER_FLAGS)
    set(SDRPP_MODULE_COMPILER_FLAGS @SDRPP_MODULE_COMPILER_FLAGS@)
endif ()

# Created shared lib and link to core
add_library(${PROJECT_NAME} SHARED ${SRC})
target_link_libraries(${PROJECT_NAME} PRIVATE sdrpp_core)
target_include_directories(${PROJECT_NAME} PRIVATE "${SDRPP_CORE_ROOT}/src/")
set_target_properties(${PROJECT_NAME} PROPERTIES PREFIX "")

# Set compile arguments
target_compile_options(${PROJECT_NAME} PRIVATE ${SDRPP_MODULE_COMPILER_FLAGS})

# Copy libraries to modules dir
if(MINGW OR ${CMAKE_SYSTEM_NAME} MATCHES "Linux")
    add_custom_target(copy-module_${PROJECT_NAME} ALL COMMAND ${CMAKE_COMMAND} -E copy \"$<TARGET_FILE_DIR:${PROJECT_NAME}>/${PROJECT_NAME}${CMAKE_SHARED_LIBRARY_SUFFIX}\" \"${BUILD_OUTPUT_DIR}/modules/\")
endif()

# Install directives
install(TARGETS ${PROJECT_NAME} DESTINATION lib/sdrpp/plugins)