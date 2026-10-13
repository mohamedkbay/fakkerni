import AppKit

// Render the existing docs/media/brand.svg geometry; no new logo or external artwork.
let size = 1024
let space = CGColorSpaceCreateDeviceRGB()
let context = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: size*4, space: space, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
context.setFillColor(CGColor(red: 5/255, green: 6/255, blue: 7/255, alpha: 1))
context.fill(CGRect(x: 0, y: 0, width: size, height: size))
context.translateBy(x: 0, y: CGFloat(size)); context.scaleBy(x: CGFloat(size)/320, y: -CGFloat(size)/320)
context.setStrokeColor(CGColor(red: 231/255, green: 236/255, blue: 233/255, alpha: 1)); context.setLineWidth(13); context.setLineCap(.round)
context.strokeEllipse(in: CGRect(x: 57, y: 57, width: 206, height: 206))
context.move(to: CGPoint(x: 88, y: 113)); context.addCurve(to: CGPoint(x: 190, y: 195), control1: CGPoint(x: 119, y: 107), control2: CGPoint(x: 135, y: 190)); context.addCurve(to: CGPoint(x: 243, y: 170), control1: CGPoint(x: 218, y: 197), control2: CGPoint(x: 236, y: 185)); context.strokePath()
context.move(to: CGPoint(x: 81, y: 199)); context.addCurve(to: CGPoint(x: 166, y: 214), control1: CGPoint(x: 102, y: 240), control2: CGPoint(x: 140, y: 245)); context.addCurve(to: CGPoint(x: 223, y: 127), control1: CGPoint(x: 191, y: 185), control2: CGPoint(x: 196, y: 143)); context.addCurve(to: CGPoint(x: 256, y: 126), control1: CGPoint(x: 235, y: 120), control2: CGPoint(x: 245, y: 120)); context.strokePath()
let representation = NSBitmapImageRep(cgImage: context.makeImage()!)
try representation.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: CommandLine.arguments[1]))
