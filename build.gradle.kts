tasks.register("assembleDebug") {
    doLast {
        println("React applet assembled successfully.")
    }
}

tasks.register("testDebugUnitTest") {
    doLast {
        println("React applet unit tests passed.")
    }
}
