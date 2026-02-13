import org.jetbrains.changelog.Changelog

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.3.0"
    id("org.jetbrains.changelog") version "2.5.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        val type = providers.gradleProperty("platformType")
        val version = providers.gradleProperty("platformVersion")
        create(type, version)

        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.spring.boot")

        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
    }

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
}

intellijPlatform {
    pluginConfiguration {
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        // CHANGELOG.md에서 현재 버전 또는 Unreleased 변경 내역을 HTML로 변환하여 주입
        changeNotes = provider {
            changelog.renderItem(
                changelog.getOrNull(providers.gradleProperty("pluginVersion").get())
                    ?: changelog.getUnreleased(),
                Changelog.OutputType.HTML
            )
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("sinceBuildVersion")
            untilBuild = providers.gradleProperty("untilBuildVersion")
        }
    }

    // 플러그인 서명 설정 (환경변수 미설정 시 자동 스킵)
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    // JetBrains Marketplace 배포 설정
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // 버전에 alpha/beta/rc 포함 시 beta 채널, 아니면 default 채널
        channels = providers.gradleProperty("pluginVersion").map {
            listOf(
                if (it.contains("beta") || it.contains("rc") || it.contains("alpha")) "beta" else "default"
            )
        }
    }

    // Plugin Verifier로 호환성 검증
    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    withType<JavaCompile> {
        val javaVersion = providers.gradleProperty("javaVersion").get()
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform()
    }
}
