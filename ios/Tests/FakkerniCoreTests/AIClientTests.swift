import XCTest
@testable import FakkerniCore

private final class MockProtocol: URLProtocol {
    static var respond: ((URLRequest) throws -> (Int, Data))?
    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func startLoading() {
        do {
            let (status, data) = try Self.respond!(request)
            client?.urlProtocol(self, didReceive: HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: nil, headerFields: nil)!, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data); client?.urlProtocolDidFinishLoading(self)
        } catch { client?.urlProtocol(self, didFailWithError: error) }
    }
    override func stopLoading() {}
}

final class AIClientTests: XCTestCase {
    func client() -> AIClient {
        let config = URLSessionConfiguration.ephemeral; config.protocolClasses = [MockProtocol.self]
        return AIClient(session: URLSession(configuration: config))
    }
    override func tearDown() { MockProtocol.respond = nil; super.tearDown() }
    func testTranscriptionSurvivesBlockedExtractionWithoutProviderFallback() async throws {
        var paths: [String] = []
        MockProtocol.respond = { request in
            XCTAssertEqual(request.url?.host, "api.groq.com")
            paths.append(request.url!.path)
            if request.url!.path.hasSuffix("/audio/transcriptions") { return (200, Data("{\"text\":\"غدوة نتصل بأحمد\"}".utf8)) }
            return (403, Data("{}".utf8))
        }
        let draft = try await client().transcribe(audio: Data([1,2,3]), provider: .groq, key: "gsk_mock_key_for_offline_tests_only")
        XCTAssertEqual(draft.transcript, "غدوة نتصل بأحمد"); XCTAssertTrue(draft.extractionFailed); XCTAssertTrue(draft.date.isEmpty)
        XCTAssertEqual(paths, ["/openai/v1/audio/transcriptions", "/openai/v1/chat/completions"])
    }
    func testAuthenticationFailureDoesNotCreateADraft() async {
        MockProtocol.respond = { _ in (401, Data("{}".utf8)) }
        do { _ = try await client().transcribe(audio: Data([1]), provider: .groq, key: "gsk_mock_key_for_offline_tests_only"); XCTFail("Expected authentication failure") }
        catch { XCTAssertEqual(error as? AIError, .http(401)) }
    }
    func testVerificationRequiresBothModelsAndSendsNoRecording() async {
        MockProtocol.respond = { request in
            XCTAssertEqual(request.httpMethod, "GET"); XCTAssertNil(request.httpBody)
            return (200, Data("{\"data\":[{\"id\":\"whisper-large-v3-turbo\"}]}".utf8))
        }
        do { try await client().verify(provider: .groq, key: "gsk_mock_key_for_offline_tests_only"); XCTFail("Expected missing extraction model") }
        catch { XCTAssertEqual(error as? AIError, .unavailableModels) }
    }
    func testCancelledExtractionIsNotTurnedIntoFallbackDraft() async {
        MockProtocol.respond = { request in
            if request.url!.path.hasSuffix("/audio/transcriptions") { return (200, Data("{\"text\":\"غدوة نتصل بأحمد\"}".utf8)) }
            throw URLError(.cancelled)
        }
        do { _ = try await client().transcribe(audio: Data([1]), provider: .groq, key: "gsk_mock_key_for_offline_tests_only"); XCTFail("Expected cancellation") }
        catch { XCTAssertTrue(error is CancellationError) }
    }
}
