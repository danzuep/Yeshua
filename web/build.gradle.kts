/*
 * Copyright (C) 2023 Xizhi Zhu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


plugins {
    id(Dependencies.Kotlin.Multiplatform.plugin)
}

val translationIds = providers.gradleProperty("translations").orElse("engwebu,eng-kjv2006,eng-asv")
val translationsDir = layout.buildDirectory.dir("translations")

kotlin {
    // Converts eBible USFM to the static JSON files the web site reads, see docs/WEB.md.
    jvm {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(Versions.Kotlin.jvmTarget))
        }
    }

    js {
        browser {
            commonWebpackConfig {
                outputFileName = "yeshua.js"
            }
            testTask { enabled = false }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(Dependencies.Kotlin.coroutinesCore)
            implementation(Dependencies.Kotlin.serializationJson)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// Downloads the translations from eBible.org and writes build/translations/<shortName>.json plus index.json.
tasks.register<JavaExec>("convertTranslations") {
    group = "web"
    description = "Converts eBible USFM translations to static JSON, e.g. -Ptranslations=engwebu,eng-kjv2006"
    val jvmMain = kotlin.jvm().compilations.getByName("main")
    dependsOn(jvmMain.compileTaskProvider)
    classpath(jvmMain.output.allOutputs, jvmMain.runtimeDependencyFiles)
    mainClass.set("com.github.danzuep.yeshua.android.web.ConvertKt")
    args(
        rootProject.file("app/src/main/assets/translations.csv").absolutePath,
        translationsDir.get().asFile.absolutePath,
        translationIds.get(),
    )
    outputs.dir(translationsDir)
}

// The site: the webpack bundle, the page and the translations.
tasks.register<Sync>("assembleSite") {
    group = "web"
    description = "Collects the web site in build/site"
    dependsOn("jsBrowserDistribution", "convertTranslations")
    from(layout.buildDirectory.dir("dist/js/productionExecutable"))
    from(translationsDir) { into("translations") }
    into(layout.buildDirectory.dir("site"))
}
