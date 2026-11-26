package mx.uanl.fime.voley

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun AtrasSistema(activo: Boolean, alVolver: () -> Unit) = BackHandler(activo, alVolver)

@Composable
actual fun rememberCompartir(): (String) -> Unit {
    val contexto = LocalContext.current
    return { texto ->
        val envio = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, texto)
        contexto.startActivity(Intent.createChooser(envio, "Compartir reporte"))
    }
}
