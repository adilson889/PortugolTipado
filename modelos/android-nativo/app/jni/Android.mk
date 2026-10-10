LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := programa

# Todos os .c de jni/src: o programa.c gerado pelo PortugolTipado
# e, quando existir, o runtime C da layoutnativos.
LOCAL_SRC_FILES := $(subst $(LOCAL_PATH)/,,$(wildcard $(LOCAL_PATH)/src/*.c))

LOCAL_CFLAGS  := -Os -fvisibility=hidden -ffunction-sections -fdata-sections
LOCAL_LDFLAGS := -Wl,--gc-sections
LOCAL_LDLIBS  := -llog -lm

include $(BUILD_SHARED_LIBRARY)
