//
// Created by sulfate on 2023-10-12.
//

#ifndef QAUXV_V2SIGN_H
#define QAUXV_V2SIGN_H

#include <cstdint>
#include <string>
#include <jni.h>

namespace teble::v2sign {

bool checkSignature(JNIEnv* env, bool isInHostAsModule);

}

#endif //QAUXV_V2SIGN_H
