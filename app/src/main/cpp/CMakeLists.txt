cmake_minimum_required(VERSION 3.10.2)

project("siyox_verify")

set(CMAKE_CXX_STANDARD 17)

add_library(
    siyox_verify
    SHARED
    SiyoX_Verify.cpp
)

target_include_directories(
    siyox_verify
    PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

find_library(
    log-lib
    log
)

target_link_libraries(
    siyox_verify
    ${log-lib}
)
