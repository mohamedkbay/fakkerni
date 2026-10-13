// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "FakkerniCore",
    platforms: [.iOS(.v16), .macOS(.v13)],
    products: [.library(name: "FakkerniCore", targets: ["FakkerniCore"])],
    targets: [
        .target(name: "FakkerniCore"),
        .testTarget(name: "FakkerniCoreTests", dependencies: ["FakkerniCore"])
    ]
)
