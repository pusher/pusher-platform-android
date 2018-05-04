# pusher-platform-android

[![Twitter](https://img.shields.io/badge/twitter-@Pusher-blue.svg?style=flat)](http://twitter.com/Pusher)
[![GitHub license](https://img.shields.io/badge/license-MIT-lightgrey.svg)](https://raw.githubusercontent.com/pusher/pusher-platform-android/master/LICENSE.md)
[![codecov](https://codecov.io/gh/pusher/pusher-platform-android/branch/master/graph/badge.svg)](https://codecov.io/gh/pusher/pusher-platform-android)
[![Travis branch](https://img.shields.io/travis/pusher/pusher-platform-android/master.svg)](https://travis-ci.org/pusher/pusher-platform-android)

## Table of Contents

* [Installation](#installation)
* [Usage](#usage)
* [Testing](#testing)
* [Communication](#communication)
* [License](#license)


## Installation

### Gradle

In your `build.gradle` file, add this line:

```groovy
dependencies {
    // ...
    api 'com.pusher:pusher-platform-android:$pusher_platform_version'
}
```

## Usage

The main entity used to access to platform services is [Instance](pusher-platform-core/src/main/kotlin/com/pusher/platform/Instance.kt).

The simpler way to create a new instance uses 4 things:

* `locator`: Instance locator id in the form of 'v1:us1:1a234-123a-1234-12a3-1234123aa12'
* `serviceName`: The name of the service this instance will use
* `serviceVersion`: The version of the service
* `dependencies`: An object containing the requirements for the instance. The default implementation for Android provides all that is needed in `com.pusher.platform.AndroidDependencies`

Examples:

```kotlin
class KotlinExample {
    val instance = Instance(
         locator = "v1:us1:1a234-123a-1234-12a3-1234123aa12",
         serviceName = "service",
         serviceVersion = "v1",
         dependencies = AndroidDependencies()
    )
}
```

```java
class JavaExample {
    Instance instance = new Instance(
        "v1:us1:1a234-123a-1234-12a3-1234123aa12",
        "service",
        "v1",
        new AndroidDependencies()
    );
}

```


## Testing

We use Junit 5 and/or Spek to run unit and integration tests. In `pusher-platform-core` we have two test folders. Unit tests can be located in `test` and integration tests in `integrationTest`. In order to run integration tests locally we have to do a couple of extra steps. They can be found [here](pusher-platform-core/src/integrationTest/Readme.md).

## Publishing

The two atifacts this project produces (`pusher-platform-core` and `pusher-platform-android`) are published in `jCenter`.

Firstly, make sure you have a binTray account. To get the api key go to Profile > Edit > Api Key

Then you need to set up a user name and api key.
 
Either on your local ~/.gradle/gradle.properties as:

```properties

bintrayUser=you-bintray-user-name
bintrayApiKey=your-bintray-api-key
```

Or as environment variables (mainly for CI):

```bash
BINTRAY_USER=you-bintray-user-name
BINTRAY_API_KEY=your-bintray-api-key
```

Now, to do the actual release run:

```bash
gradlew bintrayUpload
```

**Note:** The publish action will both override the current release (with the same version name) and automatically publish it.

## Communication

- Found a bug? Please open an [issue](https://github.com/pusher/pusher-platform-android/issues).
- Have a feature request. Please open an [issue](https://github.com/pusher/pusher-platform-android/issues).
- If you want to contribute, please submit a [pull request](https://github.com/pusher/pusher-platform-android/pulls) (preferably with some tests 🙂 ).
- For further questions, you can come say hi on [Slack](https://feedback-beta.pusher.com/)


## License

pusher-platform-android is released under the MIT license. See [LICENSE](https://github.com/pusher/pusher-platform-android/blob/master/LICENSE.md) for details.
