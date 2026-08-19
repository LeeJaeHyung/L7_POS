plugins {
    java
    application
    id("org.openjfx.javafxplugin") version "0.0.13"
}

group = "com.l7pos"
version = "1.0.0"

repositories {
    mavenCentral()
}

val appName = "L7_POS"
val mainClassName = "com.l7pos.l7_pos.Launcher"
val junitVersion = "5.12.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainClass.set(mainClassName)
}

javafx {
    version = "21.0.6"
    modules = listOf(
        "javafx.controls",
        "javafx.fxml",
        "javafx.web",
        "javafx.swing",
        "javafx.media"
    )
}

dependencies {
    implementation("org.controlsfx:controlsfx:11.2.1")

    implementation("com.dlsc.formsfx:formsfx-core:11.6.0") {
        exclude(group = "org.openjfx")
    }

    implementation("net.synedra:validatorfx:0.6.1") {
        exclude(group = "org.openjfx")
    }

    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.bootstrapfx:bootstrapfx-core:0.4.0")

    implementation("eu.hansolo:tilesfx:21.0.9") {
        exclude(group = "org.openjfx")
    }

    // JPA / Hibernate
    implementation("org.hibernate.orm:hibernate-core:6.4.4.Final")
    implementation("org.hibernate.orm:hibernate-community-dialects:6.4.4.Final")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")
    implementation("jakarta.activation:jakarta.activation-api:2.1.3")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")

    // SQLite
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")

    // MySQL 제거
    // implementation("com.mysql:mysql-connector-j:8.3.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jar {
    archiveFileName.set("$appName.jar")

    manifest {
        attributes["Main-Class"] = mainClassName
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

/*
 * 설치 파일 만들기
 *
 * jpackage 는 크로스 빌드를 못 한다.
 * 윈도우 exe 는 반드시 윈도우에서 빌드해야 한다.
 * (맥에서 --type exe 를 주면 "Invalid or unsupported type" 으로 거부된다)
 *
 * 자바 런타임은 jpackage 가 자동으로 함께 넣으므로
 * 사용자 PC 에 자바를 따로 설치할 필요가 없다.
 *
 * SQLite 는 sqlite-jdbc jar 안에 윈도우용 DLL 이 들어있어
 * 별도 처리 없이 그대로 동작한다.
 */
val prepareJpackageInput by tasks.registering(Copy::class) {
    dependsOn(tasks.jar)

    into(layout.buildDirectory.dir("jpackage-input"))

    from(tasks.jar) {
        rename { "$appName.jar" }
    }

    // JavaFX 는 플랫폼별 jar 를 쓴다. 빌드하는 OS 것이 자동으로 들어간다.
    from(configurations.runtimeClasspath)

    // 같은 이름의 jar 가 겹칠 때 빌드가 깨지지 않도록
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

/**
 * jpackage 공통 인자
 *
 * type 이 "app-image" 면 설치 파일이 아니라
 * 실행 가능한 폴더만 만든다. (빌드 점검용)
 */
fun jpackageArguments(type: String): List<String> {
    val osName = System.getProperty("os.name").lowercase()
    val isWindows = osName.contains("win")
    val isMac = osName.contains("mac")

    val iconPath = when {
        isWindows -> file("src/main/resources/icon.ico")
        isMac -> file("src/main/resources/icon.icns")
        else -> null
    }

    val args = mutableListOf(
        "${System.getProperty("java.home")}/bin/jpackage",
        "--type", type,
        "--name", appName,
        "--input", layout.buildDirectory.dir("jpackage-input").get().asFile.absolutePath,
        "--main-jar", "$appName.jar",
        "--main-class", mainClassName,
        "--app-version", version.toString(),
        "--vendor", "L7POS",
        "--description", "L7 POS 매장 판매 관리",
        "--dest", layout.buildDirectory.dir("jpackage").get().asFile.absolutePath,
        // 한글이 깨지지 않도록 인코딩을 고정한다
        "--java-options", "-Dfile.encoding=UTF-8"
    )

    if (iconPath != null && iconPath.exists()) {
        args.add("--icon")
        args.add(iconPath.absolutePath)
    }

    // 설치 파일일 때만 의미 있는 옵션들
    if (isWindows && type != "app-image") {
        args.addAll(
            listOf(
                "--win-shortcut",           // 바탕화면 바로가기
                "--win-menu",               // 시작 메뉴 등록
                "--win-menu-group", "L7 POS",
                "--win-dir-chooser",        // 설치 경로 선택 가능
                "--win-per-user-install",   // 관리자 권한 없이 설치
                // 재설치할 때 새로 깔리지 않고 갱신되도록 고정 ID 를 준다
                "--win-upgrade-uuid", "0873EC1F-ECD0-4EFA-BFB2-9CC8D31C5CD0"
            )
        )
    }

    return args
}

/**
 * 설치 파일 생성 (윈도우 exe / 맥 dmg / 리눅스 deb)
 */
tasks.register<Exec>("jpackage") {
    group = "distribution"
    description = "현재 OS 용 설치 파일을 만든다 (윈도우 exe 는 윈도우에서 실행해야 함)"

    dependsOn(prepareJpackageInput)

    val osName = System.getProperty("os.name").lowercase()

    val installerType = when {
        osName.contains("win") -> "exe"
        osName.contains("mac") -> "dmg"
        else -> "deb"
    }

    doFirst {
        delete(layout.buildDirectory.dir("jpackage"))
        mkdir(layout.buildDirectory.dir("jpackage"))

        logger.lifecycle("설치 파일 형식: $installerType (빌드 OS: $osName)")

        if (installerType != "exe") {
            logger.lifecycle(
                "윈도우 exe 가 필요하면 윈도우에서 gradlew.bat jpackage 를 실행하거나, " +
                        "GitHub Actions 의 windows-installer 워크플로를 사용하세요."
            )
        }
    }

    commandLine(jpackageArguments(installerType))
}

/**
 * 설치 파일 없이 실행 폴더만 만든다.
 *
 * 자바 런타임이 제대로 들어갔는지, 의존 jar 가 빠지지 않았는지
 * 빠르게 확인할 때 쓴다.
 */
tasks.register<Exec>("jpackageAppImage") {
    group = "distribution"
    description = "설치 파일 대신 실행 폴더만 만든다 (패키징 점검용)"

    dependsOn(prepareJpackageInput)

    doFirst {
        delete(layout.buildDirectory.dir("jpackage"))
        mkdir(layout.buildDirectory.dir("jpackage"))
    }

    commandLine(jpackageArguments("app-image"))
}
