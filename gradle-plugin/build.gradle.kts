plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("org.jreleaser")
}

group = "io.github.alien-tools"
val releaseVersion = findProperty("releaseVersion") as? String
version = releaseVersion ?: "0.7.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.github.alien-tools:roseau-core:0.7.0-SNAPSHOT")

    testImplementation(gradleTestKit())
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.7")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

gradlePlugin {
    website.set("https://github.com/alien-tools/roseau")
    vcsUrl.set("https://github.com/alien-tools/roseau.git")

    plugins {
        create("roseauPlugin") {
            id = "io.github.alien-tools.roseau"
            implementationClass = "io.github.alien.roseau.gradle.RoseauGradlePlugin"
            displayName = "Roseau API Compatibility"
            description = "Detects binary and source API breaking changes between Java library versions"
            tags.set(listOf("api", "compatibility", "breaking-changes", "java", "semver"))
        }
    }
}

publishing {
    publications {
        withType<MavenPublication> {
            pom {
                name.set("Roseau Gradle Plugin")
                description.set(
                    "Gradle plugin for Roseau — API breaking change detection for Java libraries")
                url.set("https://github.com/alien-tools/roseau")
                licenses {
                    license {
                        name.set("MIT")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("alien-tools")
                        name.set("Thomas Degueule")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/alien-tools/roseau.git")
                    developerConnection.set("scm:git:ssh://github.com:alien-tools/roseau.git")
                    url.set("https://github.com/alien-tools/roseau/tree/main")
                }
            }
        }
    }

    repositories {
        mavenLocal()
        maven {
            name = "staging"
            url = layout.buildDirectory.dir("staging-deploy")
        }
    }
}

jreleaser {
    signing {
        active = "ALWAYS"
        armored = true
    }

    deploy {
        maven {
            mavenCentral {
                sonatype {
                    active = "ALWAYS"
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository("build/staging-deploy")
                }
            }
        }
    }
}
