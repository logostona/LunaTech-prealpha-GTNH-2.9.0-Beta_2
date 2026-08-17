plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// The validation harness described in SCOPE.md section 3 runs on JUnit 5.
// A tolerance or invariant violation must fail the build, so this is not optional tooling.
tasks.test.configure {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
