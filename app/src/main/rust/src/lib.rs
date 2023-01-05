use std::ffi::c_void;

use jni::sys::{jint, JavaVM, JNI_VERSION_1_6};
use log::debug;

fn init_logger() {
    android_logger::init_once(
        android_logger::Config::default()
            .with_min_level(log::Level::Debug)
            .with_tag("QAuxv"),
    );
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "C" fn RUST_JNI_OnLoad(_: *mut JavaVM, _: *mut c_void) -> jint {
    init_logger();

    debug!("Hello Rust!");

    JNI_VERSION_1_6
}
