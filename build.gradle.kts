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

    implementation("org.hibernate.orm:hibernate-core:6.4.4.Final")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")
    implementation("jakarta.activation:jakarta.activation-api:2.1.3")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.2")
    implementation("com.mysql:mysql-connector-j:8.3.0")

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

val prepareJpackageInput by tasks.registering(Copy::class) {
    dependsOn(tasks.jar)

    into(layout.buildDirectory.dir("jpackage-input"))

    from(tasks.jar) {
        rename { "$appName.jar" }
    }

    from(configurations.runtimeClasspath)
}

tasks.register<Exec>("jpackage") {
    group = "distribution"
    description = "Create native installer using jpackage"

    dependsOn(prepareJpackageInput)

    val osName = System.getProperty("os.name").lowercase()

    val installerType = when {
        osName.contains("win") -> "exe"
        osName.contains("mac") -> "dmg"
        else -> "deb"
    }

    val iconPath = when {
        osName.contains("win") -> file("src/main/resources/icon.ico")
        osName.contains("mac") -> file("src/main/resources/icon.icns")
        else -> null
    }

    val jpackageExecutable = "${System.getProperty("java.home")}/bin/jpackage"

    doFirst {
        delete(layout.buildDirectory.dir("jpackage"))
        mkdir(layout.buildDirectory.dir("jpackage"))
    }

    val args = mutableListOf(
        jpackageExecutable,
        "--type", installerType,
        "--name", appName,
        "--input", layout.buildDirectory.dir("jpackage-input").get().asFile.absolutePath,
        "--main-jar", "$appName.jar",
        "--main-class", mainClassName,
        "--app-version", version.toString(),
        "--vendor", "L7POS",
        "--dest", layout.buildDirectory.dir("jpackage").get().asFile.absolutePath
    )

    if (iconPath != null && iconPath.exists()) {
        args.add("--icon")
        args.add(iconPath.absolutePath)
    }

    if (osName.contains("win")) {
        args.add("--win-shortcut")
        args.add("--win-menu")
    }

    commandLine(args)
}