//
// Created by Qiu on 2021/10/13.
//

#ifndef GPUIMAGE_LOG_H
#define GPUIMAGE_LOG_H

#if defined(ANDROID) || defined(__ANDROID__)

#include <android/log.h>

#define LOG_TAG "gif_native"
#define DEBUG true
static int openLog = 1;

#ifdef DEBUG
#define LOGD(...)  do {if(openLog == 1) { (void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__); } } while(0)
#define LOGI(...)  do {if(openLog == 1) { (void)__android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__); }} while(0)
#define LOGW(...)  do {if(openLog == 1) { (void)__android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__); }} while(0)
#define LOGE(...)  do {if(openLog == 1) { (void)__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__); }} while(0)
#else
#define LOGD(...)
#define LOGI(...)
#define LOGW(...)
#define LOGE(...)
#endif // DEBUG

#endif


#endif //GPUIMAGE_LOG_H
