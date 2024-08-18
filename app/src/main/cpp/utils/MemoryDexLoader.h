#include <string>
#include <string_view>
#include <memory>

#include <jni.h>

#include "utils/art_symbol_resolver.h"

namespace qauxv {

namespace art {

class DexFile {
public:
    DexFile() = delete;
    ~DexFile() = default;
    // no copy, no assign
    DexFile(const DexFile&) = delete;
    DexFile& operator=(const DexFile&) = delete;
    DexFile(DexFile&&) = delete;
    DexFile& operator=(DexFile&&) = delete;

    jobject ToJavaDexFile(JNIEnv* env) const;

    // Opens a .dex file at the given address, optionally backed by a MemMap
    // see https://cs.android.com/android/platform/superproject/+/android-7.0.0_r1:art/runtime/dex_file.cc
    static const art::DexFile* OpenMemory(const uint8_t* dex_file, size_t size, std::string location, std::string* error_msg);

};

}

}
