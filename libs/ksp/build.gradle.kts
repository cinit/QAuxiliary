plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.ksp)
    implementation(libs.kotlinpoet.ksp)
}

kotlin {
    jvmToolchain(Version.java.toString().toInt())
}

tasks.withType<JavaCompile> {
    sourceCompatibility = Version.java.toString()
    targetCompatibility = Version.java.toString()
}
