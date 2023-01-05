use std::ffi::c_void;

use jni::sys::{jint, JavaVM, JNI_VERSION_1_6};
use log::debug;

fn init_logger() {
    android_logger::init_once(
        android_logger::Config::default()
            .with_min_level(if cfg!(debug_assertions) {
                log::Level::Debug
            } else {
                log::Level::Info
            })
            .with_tag("RustLib"),
    );
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "C" fn JNI_OnLoad(_: *mut JavaVM, _: *mut c_void) -> jint {
    init_logger();

    debug!("Call JNI_OnLoad");

    JNI_VERSION_1_6
}
