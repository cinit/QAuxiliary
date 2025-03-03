//
// Created by sulfate on 2024-08-18.
//

#ifndef QAUXV_LSPLANTBRIDGE_H
#define QAUXV_LSPLANTBRIDGE_H

#include <jni.h>

namespace qauxv {

bool InitLSPlantImpl(JNIEnv* env);

void HookArtProfileSaver();

}

#endif //QAUXV_LSPLANTBRIDGE_H
