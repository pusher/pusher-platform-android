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

To create a new instance we need 4 things:

* id: The instance id
* baseClient: A client with all the required dependencies
* serviceName: the name of the service this instance will use
* serviceVersion: The version of the service



## Testing

We use Junit 5 and/or Spek to run unit and integration tests. In `pusher-platform-core` we have two test folders. Unit tests can be located in `test` and integration tests in `integrationTest`. In order to run integration tests locally have to do a couple of extra steps. They can be found [here](pusher-platform-core/src/integrationTest/Readme.md).

## Communication

- Found a bug? Please open an [issue](https://github.com/pusher/pusher-platform-android/issues).
- Have a feature request. Please open an [issue](https://github.com/pusher/pusher-platform-android/issues).
- If you want to contribute, please submit a [pull request](https://github.com/pusher/pusher-platform-android/pulls) (preferably with some tests 🙂 ).
- For further questions, you can come say hi on [Slack](https://feedback-beta.pusher.com/)


## License

pusher-platform-android is released under the MIT license. See [LICENSE](https://github.com/pusher/pusher-platform-android/blob/master/LICENSE.md) for details.
