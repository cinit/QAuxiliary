import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:${Version.kotlin}-${Version.ksp}")
    // Note that this API is currently in preview and subject to API changes.
    implementation("com.squareup:kotlinpoet-ksp:1.10.2")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = Version.java.toString()
}

tasks.withType<JavaCompile> {
    sourceCompatibility = Version.java.toString()
    targetCompatibility = Version.java.toString()
}
