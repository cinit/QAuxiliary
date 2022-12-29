import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

dependencies {
    implementation(libs.ksp)
    implementation(libs.kotlinpoet.ksp)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = Version.java.toString()
}

tasks.withType<JavaCompile> {
    sourceCompatibility = Version.java.toString()
    targetCompatibility = Version.java.toString()
}
