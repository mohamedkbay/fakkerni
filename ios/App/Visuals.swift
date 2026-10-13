import SwiftUI

enum Palette {
    static let background = Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? UIColor(red: 5/255, green: 6/255, blue: 7/255, alpha: 1) : UIColor(red: 247/255, green: 248/255, blue: 245/255, alpha: 1) })
    static let surface = Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? UIColor(red: 28/255, green: 32/255, blue: 33/255, alpha: 1) : .white })
    static let accent = Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? UIColor(red: 231/255, green: 236/255, blue: 233/255, alpha: 1) : UIColor(red: 32/255, green: 38/255, blue: 37/255, alpha: 1) })
    static let inverse = Color(uiColor: UIColor { $0.userInterfaceStyle == .dark ? .black : .white })
}

private enum MotionPolicy {
    static var isUITest: Bool {
        #if DEBUG
        return ProcessInfo.processInfo.arguments.contains("-ui-testing")
        #else
        return false
        #endif
    }
}

extension Font {
    static func cairo(_ size: CGFloat = 17, relativeTo style: Font.TextStyle = .body) -> Font { .custom("Cairo-Regular", size: size, relativeTo: style) }
}

struct PrimaryButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var enabled
    func makeBody(configuration: Configuration) -> some View {
        configuration.label.font(.cairo().weight(.semibold)).frame(maxWidth: .infinity).frame(minHeight: 54)
            .foregroundStyle(Palette.inverse).background(Palette.accent.opacity(configuration.isPressed ? 0.75 : 1), in: Capsule())
            .opacity(enabled ? 1 : 0.45)
    }
}

struct BrandMark: View {
    var body: some View {
        GeometryReader { geometry in
            let s = min(geometry.size.width, geometry.size.height) / 320
            Path { path in
                path.addEllipse(in: CGRect(x: 57*s, y: 57*s, width: 206*s, height: 206*s))
                path.move(to: CGPoint(x: 88*s, y: 113*s))
                path.addCurve(to: CGPoint(x: 190*s, y: 195*s), control1: CGPoint(x: 119*s, y: 107*s), control2: CGPoint(x: 135*s, y: 190*s))
                path.addCurve(to: CGPoint(x: 243*s, y: 170*s), control1: CGPoint(x: 218*s, y: 197*s), control2: CGPoint(x: 236*s, y: 185*s))
                path.move(to: CGPoint(x: 81*s, y: 199*s))
                path.addCurve(to: CGPoint(x: 166*s, y: 214*s), control1: CGPoint(x: 102*s, y: 240*s), control2: CGPoint(x: 140*s, y: 245*s))
                path.addCurve(to: CGPoint(x: 223*s, y: 127*s), control1: CGPoint(x: 191*s, y: 185*s), control2: CGPoint(x: 196*s, y: 143*s))
                path.addCurve(to: CGPoint(x: 256*s, y: 126*s), control1: CGPoint(x: 235*s, y: 120*s), control2: CGPoint(x: 245*s, y: 120*s))
            }.stroke(.primary, style: StrokeStyle(lineWidth: 13*s, lineCap: .round, lineJoin: .round))
        }.accessibilityHidden(true)
    }
}

private struct IconPart: Decodable { var p: [Double]; var closed: Bool }
private struct IconCatalog: Decodable {
    var shapes: [String: [IconPart]]
    var transitions: [String: [[IconPart]]]
    static let shared: IconCatalog = {
        guard let url = Bundle.main.url(forResource: "morphicons", withExtension: "json"), let data = try? Data(contentsOf: url), let result = try? JSONDecoder().decode(IconCatalog.self, from: data) else { return IconCatalog(shapes: [:], transitions: [:]) }
        return result
    }()
}

/// Draws the same actual Morphicons-generated frame data as the Android app.
struct MorphIcon: View {
    let name: String
    var spinning = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var previous = ""
    @State private var changed = Date.distantPast
    var body: some View {
        TimelineView(.animation(minimumInterval: 1/30, paused: reduceMotion || MotionPolicy.isUITest || (!spinning && Date().timeIntervalSince(changed) > 0.7))) { timeline in
            Canvas { context, size in
                let frames = IconCatalog.shared.transitions[previous + "_" + name]
                let elapsed = timeline.date.timeIntervalSince(changed)
                let index = Int(max(0, elapsed) * 60)
                let parts = (!reduceMotion && !MotionPolicy.isUITest && frames != nil && index < frames!.count) ? frames![index] : IconCatalog.shared.shapes[name] ?? []
                let scale = min(size.width, size.height) / 24
                var path = Path()
                for part in parts {
                    guard part.p.count >= 2 else { continue }
                    path.move(to: CGPoint(x: part.p[0]*scale, y: part.p[1]*scale))
                    for i in stride(from: 2, to: part.p.count-1, by: 2) { path.addLine(to: CGPoint(x: part.p[i]*scale, y: part.p[i+1]*scale)) }
                    if part.closed { path.closeSubpath() }
                }
                context.stroke(path, with: .foreground, style: StrokeStyle(lineWidth: 1.7*scale, lineCap: .round, lineJoin: .round))
            }.rotationEffect(.degrees(spinning && !reduceMotion && !MotionPolicy.isUITest ? timeline.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 1.2)/1.2*360 : 0))
        }
        .onChange(of: name) { old in previous = currentName; currentName = old; changed = Date() }
        .onAppear { currentName = name }
        .accessibilityHidden(true)
    }
    @State private var currentName = ""
}

struct AssistantOrb: View {
    var recording = false
    var processing = false
    var amplitude: Double = 0
    @Environment(\.colorScheme) private var scheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.scenePhase) private var scenePhase
    var body: some View {
        TimelineView(.animation(minimumInterval: 1/30, paused: reduceMotion || MotionPolicy.isUITest || scenePhase != .active)) { timeline in
            Canvas { context, size in
                let phase = reduceMotion || MotionPolicy.isUITest ? 0 : timeline.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 11)/11 * Double.pi*2
                let radius = min(size.width, size.height) * (recording ? 0.375 + amplitude*0.018 : 0.365)
                let spin = phase * (processing ? 2.3 : 1)
                let color = scheme == .dark ? Color(red: 0.90, green: 0.95, blue: 0.93) : Color(red: 0.15, green: 0.21, blue: 0.19)
                for bandIndex in 0..<44 {
                    let band = Double(bandIndex)*Double.pi*2/44
                    var path = Path()
                    for i in 0...112 {
                        let a = Double(i)*Double.pi*2/112
                        let ripple = sin(4*a+band*3+spin)*0.075 + cos(7*a-band*2-spin*0.7)*0.028
                        let r = radius*(0.68+0.19*cos(band+3*a+spin*0.45)+ripple)
                        let point = CGPoint(x: size.width/2+r*cos(a)+radius*0.10*sin(3*a+band+spin), y: size.height/2+r*sin(a)*0.88+radius*0.12*cos(2*a-band-spin*0.8))
                        if i == 0 { path.move(to: point) } else { path.addLine(to: point) }
                    }
                    let alpha = (scheme == .dark ? 0.14 : 0.18) + 0.40*pow(0.5+0.5*cos(band+spin*0.3),2)
                    context.stroke(path, with: .color(color.opacity(alpha)), lineWidth: bandIndex % 9 == 0 ? 1.1 : 0.65)
                }
            }
        }.accessibilityHidden(true)
    }
}
