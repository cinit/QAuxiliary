package cc.chenhe.qqnotifyevo.utils

/**
 * 用于标记通知的来源。
 */
enum class Tag(val pkg: String) {
    UNKNOWN(""),
    QQ("com.tencent.mobileqq"),
    QQ_HD("com.tencent.minihd.qq"),
    QQ_LITE("com.tencent.qqlite"),
    TIM("com.tencent.tim");
}