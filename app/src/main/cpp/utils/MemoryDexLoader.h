#include <string>
#include <string_view>

#include <jni.h>

namespace qauxv {

std::string GetLibArtPath();

bool InitLibArtElfView();

bool InitLSPlantImpl(JNIEnv* env);

}
