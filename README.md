[![](https://jitpack.io/v/arielmazor/cropper.svg)](https://jitpack.io/#arielmazor/cropper)
<h1 align="center" id="title">Cropper</h1>
<p align="center">Lightweight cropping library for Jetpack compose</p> 


<h2>🛠️ Installation Steps:</h2>
1. Download - Add the JitPack repository to your settings.grade

```kotlin
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
  }
}
```

2. Add the dependency

```kotlin
dependencies {
  implementation 'com.github.arielmazor:cropper:TAG'
}
```

<h2>Usage</h2>

1. Create A CropperState

```kotlin
val state = rememberCropperState(image = YOUR_IMAGE)
```

2. Usage - set your Cropper dimensions by wrapping it with aBox with custom width and height

```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(.75f)
) {
    Cropper(state)
}
```