import XCTest

final class VoleyUITests: XCTestCase {
    let app = XCUIApplication()
    let carpeta = ProcessInfo.processInfo.environment["CAPTURAS"] ?? NSTemporaryDirectory()

    override func setUp() {
        continueAfterFailure = false
        app.launch()
    }

    func foto(_ nombre: String) {
        sleep(1)
        let captura = XCUIScreen.main.screenshot()
        try? captura.pngRepresentation.write(to: URL(fileURLWithPath: carpeta).appendingPathComponent("\(nombre).png"))
        let adjunto = XCTAttachment(screenshot: captura)
        adjunto.name = nombre
        adjunto.lifetime = .keepAlways
        add(adjunto)
    }

    func elemento(_ texto: String, exacto: Bool = false) -> XCUIElement {
        let filtro = exacto ? NSPredicate(format: "label == %@", texto) : NSPredicate(format: "label BEGINSWITH %@", texto)
        return app.descendants(matching: .any).matching(filtro).firstMatch
    }

    func espera(_ texto: String, _ segundos: TimeInterval = 20, file: StaticString = #filePath, line: UInt = #line) {
        XCTAssertTrue(elemento(texto).waitForExistence(timeout: segundos), "No apareció: \(texto)", file: file, line: line)
    }

    func toca(_ texto: String, exacto: Bool = false, file: StaticString = #filePath, line: UInt = #line) {
        let e = elemento(texto, exacto: exacto)
        if !e.waitForExistence(timeout: 20) { print(app.debugDescription) }
        XCTAssertTrue(e.exists, "No encontré: \(texto)", file: file, line: line)
        e.tap()
        sleep(1)
    }

    func escribe(_ campo: String, _ valor: String) {
        let filtro = NSPredicate(format: "label == %@ OR placeholderValue == %@ OR value == %@", campo, campo, campo)
        let e = app.descendants(matching: .any).matching(filtro).firstMatch
        if !e.waitForExistence(timeout: 20) {
            print(app.debugDescription)
            XCTFail("No encontré el campo: \(campo)")
        }
        e.tap()
        sleep(1)
        app.typeText(valor)
    }

    func testRecorridoCompleto() {
        espera("Voley-itics")
        foto("01_login")

        toca("Crear cuenta nueva")
        espera("Crea tu cuenta")
        foto("02_registro")
        toca("Atrás", exacto: true)

        escribe("Usuario", "demo")
        escribe("Contraseña", "voleyitics2026")
        toca("Entrar", exacto: true)
        espera("PUNTAJE DEL EQUIPO")
        foto("03_jugadores")

        toca("Agregar jugador")
        espera("Nuevo jugador")
        foto("04_nuevo_jugador")
        toca("Cancelar", exacto: true)

        toca("Fernanda López")
        espera("de 100")
        foto("05_detalle_jugador")
        toca("Atrás", exacto: true)

        toca("Partidos", exacto: true)
        espera("vs Linces")
        foto("06_partidos")

        toca("vs Linces")
        espera("¿Quién tocó el balón?")
        foto("07_paso1")
        toca("#3 Fernanda")
        espera("Fernanda López: ¿qué hizo?")
        foto("08_paso2")
        toca("Ataque", exacto: true)
        espera("Fernanda López (Ataque)")
        foto("09_paso3")
        toca("Volver", exacto: true)
        toca("Volver", exacto: true)

        toca("Más", exacto: true)
        foto("10_menu_mas")
        toca("Ver reporte")
        espera("POR JUGADOR")
        foto("11_reporte_en_juego")
        toca("Atrás", exacto: true)
        espera("¿Quién tocó el balón?")
        toca("Atrás", exacto: true)

        toca("vs Halcones")
        espera("Ver reporte del partido")
        foto("12_partido_terminado")
        toca("Ver reporte del partido")
        espera("RESULTADO FINAL")
        foto("13_reporte")
        app.swipeUp()
        foto("14_reporte_jugadores")
        app.swipeUp()
        app.swipeUp()
        foto("15_reporte_toques")
        toca("Compartir", exacto: true)
        sleep(2)
        foto("16_compartir")
    }
}
