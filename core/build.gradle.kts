plugins {
    kotlin("jvm") version "1.9.0"
    application
}

repositories { mavenCentral() }

sourceSets {
    main {
        kotlin.srcDirs(".")
    }
}

application {
    mainClass.set("co.adilson889.typec.port.MainKt")
}

tasks.register<Jar>("fatJar") {
    archiveBaseName.set("port")
    manifest { attributes["Main-Class"] = "co.adilson889.typec.port.MainKt" }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith("jar") }
            .map { zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
