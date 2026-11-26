package mx.uanl.fime.voley

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Composable
actual fun AtrasSistema(activo: Boolean, alVolver: () -> Unit) {}

@Composable
actual fun rememberCompartir(): (String) -> Unit = { texto ->
    val hoja = UIActivityViewController(listOf(texto), null)
    UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(hoja, true, null)
}

fun MainViewController() = ComposeUIViewController { App() }
