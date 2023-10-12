//
// Created by sulfate on 2023-10-12.
//

#ifndef QAUXV_VXSIGN_H
#define QAUXV_VXSIGN_H

#include <cstdint>
#include <string>
#include <jni.h>

namespace teble::v2sign {

bool checkSignature(JNIEnv* env, bool isInHostAsModule);

}

#endif //QAUXV_VXSIGN_H
