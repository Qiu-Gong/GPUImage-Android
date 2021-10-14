cmake_minimum_required(VERSION 3.10.2)

project("gif-lib")

include_directories(
        ${CMAKE_SOURCE_DIR}/gif
        ${CMAKE_SOURCE_DIR}
)

find_library(log-lib log)
find_library(jnigraphics-lib jnigraphics)

add_library(
        gif-lib
        SHARED
        gif-lib.cpp

        gif/dgif_lib.c
        gif/gifalloc.c
)

target_link_libraries(
        gif-lib
        ${jnigraphics-lib}
        ${log-lib}
)