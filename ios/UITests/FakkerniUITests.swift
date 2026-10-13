import XCTest

final class FakkerniUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false }
    private func launch(_ extra: [String] = []) -> XCUIApplication {
        let app = XCUIApplication(); app.launchArguments = ["-ui-testing", "-reset-fixture", "-english"] + extra; app.launch(); return app
    }
    private func capture(_ app: XCUIApplication, _ name: String) {
        let attachment = XCTAttachment(screenshot: app.screenshot()); attachment.name = name; attachment.lifetime = .keepAlways; add(attachment)
    }
    func testOnboardingAndApprovalGate() throws {
        let app = launch()
        XCTAssertTrue(app.buttons["get-started"].waitForExistence(timeout: 10)); app.buttons["get-started"].tap()
        app.buttons["type-instead"].tap()
        let title = app.textFields["reminder-title"].exists ? app.textFields["reminder-title"] : app.textViews["reminder-title"]
        XCTAssertTrue(title.waitForExistence(timeout: 5)); title.tap(); title.typeText("Review the proposal")
        XCTAssertFalse(app.buttons["approve-reminder"].isEnabled)
        app.buttons["cancel-editor"].tap(); app.buttons["reminders"].tap()
        XCTAssertFalse(app.staticTexts["Review the proposal"].exists)
    }
    func testSaveManualReminderAndAlertTest() throws {
        let app = launch(["-skip-onboarding"])
        app.buttons["type-instead"].tap()
        let title = app.textFields["reminder-title"].exists ? app.textFields["reminder-title"] : app.textViews["reminder-title"]
        title.tap(); title.typeText("Call the client")
        app.switches["has-event-time"].tap()
        app.buttons["approve-reminder"].tap()
        let springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        if springboard.alerts.buttons["Allow"].waitForExistence(timeout: 5) { springboard.alerts.buttons["Allow"].tap() }
        XCTAssertTrue(app.buttons["reminders"].waitForExistence(timeout: 10)); app.buttons["reminders"].tap()
        XCTAssertTrue(app.staticTexts["Call the client"].waitForExistence(timeout: 5))
    }
    func testScreenshotsAndToneSelection() throws {
        let app = launch(["-screenshots"])
        XCTAssertTrue(app.buttons["type-instead"].waitForExistence(timeout: 10)); capture(app, "assistant-dark")
        app.buttons["type-instead"].tap(); capture(app, "editor-dark"); app.buttons["cancel-editor"].tap()
        app.buttons["settings"].tap(); capture(app, "settings-dark")
        app.buttons["alert-sounds"].tap()
        app.buttons["tone-bright"].tap()
        XCTAssertTrue(app.buttons["tone-bright"].isSelected)
        capture(app, "sounds-dark")
        app.terminate()
        let light = launch(["-screenshots", "-light"])
        XCTAssertTrue(light.buttons["type-instead"].waitForExistence(timeout: 10)); capture(light, "assistant-light")
        light.buttons["reminders"].tap(); capture(light, "reminders-light")
    }
}
