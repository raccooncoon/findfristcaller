plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.raccoon"
version = "2.7-SNAPSHOT"

repositories {
    mavenCentral()
}
dependencies {
    implementation("org.projectlombok:lombok:1.18.28")
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2022.1.4")
    type.set("IC") // Target IDE Platform

    plugins.set(listOf("com.intellij.java"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    patchPluginXml {
        sinceBuild.set("221")
        untilBuild.set("233.*")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}

dependencies {
    // lombok
    compileOnly ("org.projectlombok:lombok:1.18.22") // 롬복 라이브러리 추가
    annotationProcessor ("org.projectlombok:lombok:1.18.22") // 롬복 어노테이션 프로세서 추가

//    // H2 Database
//    implementation("com.h2database:h2:2.2.224")
//
//    // JPA API
//    implementation ("javax.persistence:javax.persistence-api:2.2")
//
//    // Hibernate JPA Provider
//    implementation ("org.hibernate:hibernate-core:5.6.6.Final")

//    implementation ("org.hibernate:hibernate-entitymanager:5.5.7.Final") // Hibernate Entity Manager
//    implementation ("org.hibernate:hibernate-java8:5.5.7.Final") // Hibernate Java 8 Support
//    implementation ("org.hibernate:hibernate-c3p0:5.5.7.Final") // Hibernate Connection Pool (Optional)


}
