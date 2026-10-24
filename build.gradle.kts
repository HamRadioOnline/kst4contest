plugins {
    java
    // Applied here as well as in the subprojects: the root needs the JacocoReport task
    // type and the tool classpath for the aggregated report below.
    jacoco
    alias(libs.plugins.spotbugs) apply false
}

allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("version").get()

    repositories {
        mavenCentral()
        // Compose Multiplatform pulls its androidx.lifecycle and androidx.annotation
        // artifacts from Google's repository; they are not on Maven Central.
        google()
    }
}

val pmdVersion = libs.versions.pmd.get()
val spotbugsVersion = libs.versions.spotbugs.get()
val jacocoVersion = libs.versions.jacoco.get()

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "pmd")
    apply(plugin = "jacoco")

    // Both tools report only; the Maven build had the failing goals commented out.
    extensions.configure<PmdExtension> {
        toolVersion = pmdVersion
        ruleSetFiles = files(rootProject.file("pmd-ruleset.xml"))
        ruleSets = emptyList()
        isIgnoreFailures = true
    }

    extensions.configure<com.github.spotbugs.snom.SpotBugsExtension> {
        toolVersion.set(spotbugsVersion)
        ignoreFailures.set(true)
    }

    // Without a configured report SpotBugs prints its findings to stdout only and leaves
    // no file behind; with ignoreFailures they then scroll past unread. AGENTS.md asks for
    // the findings visible in a build to be reported, which needs something to read them
    // from: XML for CI to count, HTML for people. Default location, which the plugin names
    // after the source set: <module>/build/reports/spotbugs/main.{xml,html} (#110).
    tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
        reports.create("xml") { required.set(true) }
        reports.create("html") { required.set(true) }
    }

    /*
     * Coverage is a report, not a gate: there is no jacocoTestCoverageVerification and no
     * minimum percentage. A threshold set at today's number would trip on the next unrelated
     * change; one set at a target would fail every build until tests are written for the
     * number's sake. The figure is published so it can be read when deciding what to test
     * next (docs/superpowers/specs/2026-10-11-test-coverage-design.md, #62).
     */
    extensions.configure<JacocoPluginExtension> {
        toolVersion = jacocoVersion
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    /*
     * Tests never see the operator's real home directory. ApplicationFileUtils resolves the
     * application folder below user.home, and the no-argument ChatPreferences constructor
     * seeds preferences.xml there when it is missing; a test that slipped through would
     * create or rewrite files in ~/.praktiKST of whoever runs the build. Each test task's JVM
     * gets its own scratch home, build/test-user-home/<task name>, emptied before the task
     * runs, so one test task can never wipe another's. Tests that need their own profile
     * directory still set user.home themselves and restore it afterwards.
     *
     * The redirection alone would hide the mistake, so every test task is finalized by
     * checkTestUserHome, which fails the build if a preferences.xml was left in any scratch
     * home: tests are expected to use ChatPreferences(Path) with a @TempDir. It is a
     * finalizer and not a doLast because a doLast is skipped when a test fails, and a
     * failing test must not hide the stray file. The check stays deliberately narrow: other
     * files land in the scratch .praktiKST by design, for example the sound files
     * PlayAudioUtils seeds from the bundled resources, and absorbing those is what the
     * redirection is for. Only preferences.xml marks a test that should have used its own file.
     *
     * The opt-in ON4KST practice tests, which deliberately read the real profile, need
     * -Don4kst.live=true, which the test task does not forward; they skip here either way
     * and are meant to be run from an IDE.
     */
    val testUserHomeRoot = layout.buildDirectory.dir("test-user-home").get().asFile
    val checkTestUserHome = tasks.register("checkTestUserHome") {
        description = "Fails if a test wrote preferences.xml into its scratch user.home"
        group = "verification"
        doLast {
            val strayPreferences = testUserHomeRoot.listFiles().orEmpty()
                .map { it.resolve(".praktiKST/preferences.xml") }
                .filter { it.exists() }
            if (strayPreferences.isNotEmpty()) {
                throw GradleException(
                    "A test wrote ${strayPreferences.joinToString()}: it used the user " +
                        "profile instead of ChatPreferences(Path) with a @TempDir"
                )
            }
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()

        val testUserHome = testUserHomeRoot.resolve(name)
        /*
         * The scratch home is a declared output. Test tasks are cacheable, and a cache hit
         * skips doFirst; as an output the directory is restored together with the cached
         * result, so checkTestUserHome always judges the files that belong to these inputs
         * and never a leftover from another run.
         *
         * Skiko unpacks its native library (about 30 MB) below user.home unless told
         * otherwise. It is kept beside the scratch home instead, so it neither bloats every
         * cache entry nor is unpacked again on each run.
         *
         * Both paths are passed through an argument provider rather than systemProperty():
         * a system property is a task input, and an absolute path there would make the
         * cache key differ between checkouts.
         */
        val skikoDataPath = layout.buildDirectory.dir("skiko-data").get().asFile
        outputs.dir(testUserHome)
        jvmArgumentProviders.add(CommandLineArgumentProvider {
            listOf(
                "-Duser.home=${testUserHome.absolutePath}",
                "-Dskiko.data.path=${skikoDataPath.absolutePath}"
            )
        })
        doFirst {
            // deleteRecursively() reports failure instead of throwing. A file left over
            // from an earlier run would later be blamed on this one, so stop here.
            if (!testUserHome.deleteRecursively()) {
                throw GradleException("Could not empty the test user.home $testUserHome")
            }
            testUserHome.mkdirs()
        }
        /*
         * A test that never returns must fail by name instead of holding the job until
         * GitHub kills it after six hours: Nightly #302 hung in :app-desktop:test for over
         * an hour, and the log named only the task. The limit is far above the slowest test
         * (under four seconds). SEPARATE_THREAD also ends a test whose thread ignores
         * interruption, such as a busy loop or a blocked native call, and the failure's
         * cause carries that thread's stack, so it shows where the test stood.
         */
        systemProperty("junit.jupiter.execution.timeout.default", "2m")
        systemProperty("junit.jupiter.execution.timeout.thread.mode.default", "SEPARATE_THREAD")
        // The default short format prints only the first frame of each cause, which for a
        // timed-out test is the line it hung in but not the call that blocked there.
        testLogging {
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        finalizedBy(checkTestUserHome)
    }

    // XML for CI to read, HTML for people. Gradle does not wire the report after the test
    // task on its own; without this dependsOn the report task would read stale or missing
    // execution data. Kotlin classes count too: the Kotlin plugin adds its output to the
    // main source set, which is what the default report reads.
    tasks.withType<JacocoReport>().configureEach {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

/*
 * One report over the two shipped modules. i18n-generator stays out: it is a build tool,
 * and its coverage says nothing about the application. Sources and classes are resolved
 * lazily because the root script is evaluated before the subprojects have their source sets.
 */
val coveredProjects = listOf(project(":core"), project(":app-desktop"))

val jacocoAggregatedReport by tasks.registering(JacocoReport::class) {
    description = "Merges the JaCoCo coverage of core and app-desktop into one report"
    group = "verification"

    dependsOn(coveredProjects.map { it.tasks.named("test") })

    executionData.setFrom(coveredProjects.map { it.layout.buildDirectory.file("jacoco/test.exec") })
    sourceDirectories.setFrom(provider {
        coveredProjects.flatMap { it.extensions.getByType<SourceSetContainer>()["main"].allSource.srcDirs }
    })
    classDirectories.setFrom(provider {
        coveredProjects.map { it.extensions.getByType<SourceSetContainer>()["main"].output }
    })

    reports {
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/aggregated/jacoco.xml"))
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/aggregated/html"))
    }
}
