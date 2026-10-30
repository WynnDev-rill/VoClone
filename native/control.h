#pragma once
#include <jni.h>
#include <string>
#include <cstdint>
#include <atomic>
// JNI modified UTF-8 encodes supplementary characters differently. Convert
// UTF-16 explicitly so model tokenizers receive standard UTF-8, including emoji.
struct Utf8 {
    std::string storage; const char* chars;
    Utf8(JNIEnv* e,jstring text) {
        chars=nullptr;
        if(e->ExceptionCheck() || !text)return;
        const auto input=e->GetStringChars(text,nullptr);if(!input)return;
        struct CharsLease { JNIEnv* e;jstring text;const jchar* input;~CharsLease(){e->ReleaseStringChars(text,input);} } lease{e,text,input};
        const auto size=e->GetStringLength(text);
        if(input) for(jsize i=0;i<size;++i) {
            uint32_t cp=input[i];
            if(cp>=0xd800&&cp<=0xdbff&&i+1<size&&input[i+1]>=0xdc00&&input[i+1]<=0xdfff) {cp=0x10000+((cp-0xd800)<<10)+(input[++i]-0xdc00);}
            else if(cp>=0xd800&&cp<=0xdfff)cp=0xfffd;
            if(cp<0x80)storage+=static_cast<char>(cp);
            else if(cp<0x800){storage+=static_cast<char>(0xc0|(cp>>6));storage+=static_cast<char>(0x80|(cp&63));}
            else if(cp<0x10000){storage+=static_cast<char>(0xe0|(cp>>12));storage+=static_cast<char>(0x80|((cp>>6)&63));storage+=static_cast<char>(0x80|(cp&63));}
            else {storage+=static_cast<char>(0xf0|(cp>>18));storage+=static_cast<char>(0x80|((cp>>12)&63));storage+=static_cast<char>(0x80|((cp>>6)&63));storage+=static_cast<char>(0x80|(cp&63));}
        }
        chars=storage.c_str();
    }
};
struct Control {
    JNIEnv* owner; JavaVM* vm=nullptr; jobject value; jmethodID check; jmethodID report;
    std::atomic<bool> failed{false};
    struct Attached {
        JavaVM* vm; JNIEnv* env=nullptr; bool detached=false;
        explicit Attached(JavaVM* v):vm(v) {
            if(vm->GetEnv(reinterpret_cast<void**>(&env),JNI_VERSION_1_6)==JNI_EDETACHED) {
                detached=vm->AttachCurrentThread(&env,nullptr)==JNI_OK;
                if(!detached)env=nullptr;
            }
        }
        ~Attached(){if(detached)vm->DetachCurrentThread();}
    };
    Control(JNIEnv* e,jobject v):owner(e),value(nullptr),check(nullptr),report(nullptr) {
        if(e->ExceptionCheck()||!v){failed=true;return;}
        value=e->NewGlobalRef(v);if(!value||e->ExceptionCheck()){failed=true;return;}
        if(e->GetJavaVM(&vm)!=JNI_OK){failed=true;return;}
        auto c=e->GetObjectClass(v);if(!c||e->ExceptionCheck()){failed=true;return;}
        check=e->GetMethodID(c,"cancelled","()Z");
        if(!e->ExceptionCheck())report=e->GetMethodID(c,"progress","(F)V");
        if(!check||!report||e->ExceptionCheck())failed=true;
        e->DeleteLocalRef(c);
    }
    ~Control(){if(value)owner->DeleteGlobalRef(value);}
    bool exception(JNIEnv* e) {
        if(!e||e->ExceptionCheck()) {
            failed=true;
            // Exceptions on an attached native worker cannot unwind the caller's
            // Java stack. Preserve the failure and surface it after native cleanup.
            if(e&&e!=owner)e->ExceptionClear();
            return true;
        }
        return false;
    }
    static bool cancelled(void* data) {
        auto c=static_cast<Control*>(data);
        if(c->failed)return true;
        Attached thread(c->vm);if(c->exception(thread.env))return true;
        const auto result=thread.env->CallBooleanMethod(c->value,c->check);
        return c->exception(thread.env)||result;
    }
    void progress(float f) {
        if(failed)return;
        Attached thread(vm);if(exception(thread.env))return;
        thread.env->CallVoidMethod(value,report,f);exception(thread.env);
    }
};
inline void fail(JNIEnv* e) {
    if(!e->ExceptionCheck()) { auto c=e->FindClass("java/lang/IllegalStateException");if(c&&!e->ExceptionCheck())e->ThrowNew(c,"Native inference failed"); }
}

inline void oom(JNIEnv* e) {
    if(!e->ExceptionCheck()) { auto c=e->FindClass("java/lang/OutOfMemoryError");if(c&&!e->ExceptionCheck())e->ThrowNew(c,"Native allocation failed"); }
}
