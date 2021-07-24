plugins {
    id("kotlin")
    id("com.github.dcendents.android-maven")
}

group = "com.github.xiazunyang"
version = "1.0.0"

dependencies {
    compileOnly("com.squareup.retrofit2:retrofit:2.9.0")
    compileOnly("com.squareup.okhttp3:okhttp:4.9.1")
}