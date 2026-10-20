plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.sumitsingh34.bitewise"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "io.github.sumitsingh34.bitewise"
        minSdk = 24
        targetSdk = 35
        // Keep in step with "BiteWise version N" in index.html.
        versionCode = 25
        versionName = "25"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Leave the git commit out of the APK so F-Droid's build matches byte for byte.
            vcsInfo.include = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // F-Droid can't read Google's encrypted dependency report, so leave it out of the APK.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

// Copies the web app (which lives at the top of the repository and is also served by
// GitHub Pages) into the APK as assets/web/, so there's only one copy of index.html.
abstract class CopyWebAssets : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val webFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:Inject
    abstract val fs: FileSystemOperations

    @TaskAction
    fun copy() {
        fs.sync {
            from(webFiles)
            into(outputDir.dir("web"))
        }
    }
}

val copyWebAssets = tasks.register<CopyWebAssets>("copyWebAssets") {
    webFiles.from(fileTree(rootDir.parentFile) {
        include("index.html", "manifest.json", "icon-192.png", "icon-512.png", "vendor/**")
    })
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(copyWebAssets, CopyWebAssets::outputDir)
    }
}
