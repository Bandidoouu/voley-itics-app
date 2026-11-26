import SwiftUI
import Shared

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            Contenido().ignoresSafeArea()
        }
    }
}

struct Contenido: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        Plataforma_iosKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
