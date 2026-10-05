LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := main

SDL_PATH := ../SDL
TTF_PATH := ../SDL2_ttf

# "SDL2/SDL.h" e "SDL2/SDL_ttf.h" existem como atalhos em jni/src/SDL2/
LOCAL_C_INCLUDES := $(LOCAL_PATH) \
    $(LOCAL_PATH)/$(SDL_PATH)/include \
    $(LOCAL_PATH)/$(TTF_PATH)

# No Android o SDL procura a funcao SDL_main. O main gerado pelo PortugolTipado
# passa a chamar-se SDL_main.
LOCAL_CFLAGS += -Dmain=SDL_main -std=gnu11

LOCAL_SRC_FILES := programa.c graficos.c

LOCAL_SHARED_LIBRARIES := SDL2 SDL2_ttf

LOCAL_LDLIBS := -lGLESv1_CM -lGLESv2 -lOpenSLES -llog -landroid -lm

include $(BUILD_SHARED_LIBRARY)
