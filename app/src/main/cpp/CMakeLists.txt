cmake_minimum_required(VERSION 3.10.2)

project("siyox_verify")

add_library(
    siyox_verify
    SHARED
    siyox_verify.c
)

find_library(
    log-lib
    log
)

target_link_libraries(
    siyox_verify
    ${log-lib}
)
