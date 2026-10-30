# Third-party notices

ArchiMan itself is MIT-licensed (see `LICENSE` and `NOTICE`). This file lists
third-party software redistributed with or used to build the application.

Unless noted otherwise, each component is used under its own license terms.
Apache-2.0 and MIT dependencies are compatible with distributing ArchiMan under MIT.

## Carbon Icons

ArchiMan uses icon geometry from Carbon Design System's `@carbon/icons-react`
version 11.87.0 in both the Android application and its phone-hosted web
portal.

Copyright 2015 IBM Corp.

Licensed under the Apache License, Version 2.0.
https://www.apache.org/licenses/LICENSE-2.0

Upstream: https://github.com/carbon-design-system/carbon

## Archi Calc (AcharyaOS / Paperdesk calculator)

Siteman's embedded Archi Calc expression evaluator and length/area/volume
measure helper adapt logic from Holagundi Consulting Works' AcharyaOS /
Paperdesk calculator (`CalculatorEngine`, `UnitConverter`).

The standalone `HolagundiWorks/architect-calculator` repository was not
available as a cloneable module at integration time; the engines are ported
in-process under `com.example.domain.archicalc` and remain quantity-only
(no rates or commercial fields).

Licensed under the Apache License, Version 2.0.
https://www.apache.org/licenses/LICENSE-2.0

## AndroidX / Jetpack / Compose / Room

Google AndroidX libraries (Core KTX, Lifecycle, Activity Compose, Compose UI,
Material 3, Room, and related tooling) as declared in `gradle/libs.versions.toml`.

Licensed under the Apache License, Version 2.0.
https://developer.android.com/jetpack/androidx/releases

## Kotlin & kotlinx.coroutines

JetBrains Kotlin standard library and kotlinx.coroutines.

Licensed under the Apache License, Version 2.0.
https://github.com/JetBrains/kotlin
https://github.com/Kotlin/kotlinx.coroutines

## Coil

Coil image loading for Compose (`io.coil-kt:coil-compose`).

Licensed under the Apache License, Version 2.0.
https://github.com/coil-kt/coil

## Moshi

Square Moshi JSON (`com.squareup.moshi`).

Licensed under the Apache License, Version 2.0.
https://github.com/square/moshi

## JUnit 4 (tests only)

JUnit 4 is used for unit tests and is not packaged into the release APK.

Eclipse Public License 1.0.
https://junit.org/junit4/

## Robolectric (tests only)

Robolectric is used for Android unit tests and is not packaged into the release APK.

Licensed under the MIT License.
https://github.com/robolectric/robolectric

## Android Gradle Plugin & build tooling

Build-time only (not redistributed in the APK). See Google / JetBrains / Gradle
license terms for the versions pinned in `gradle/libs.versions.toml`.
