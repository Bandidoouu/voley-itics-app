package mx.uanl.fime.voley

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val COLORES = lightColorScheme(
    primary = Color(0xFF1E3A8A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF1E3A8A),
    background = Color(0xFFF4F5F7),
    surface = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFFAFAFA),
    onSurfaceVariant = Color(0xFF5B6472),
    outline = Color(0xFFC9CED6),
    error = Color(0xFFDC2626),
)

enum class Vista { LOGIN, REGISTRO, APP }

sealed class Pantalla {
    object Jugadores : Pantalla()
    object Partidos : Pantalla()
    data class Detalle(val jugadorId: Long) : Pantalla()
    data class EnVivo(val partidoId: Long) : Pantalla()
    data class Reporte(val partidoId: Long) : Pantalla()
}

@Composable
fun App() {
    val db = remember { Db() }
    MaterialTheme(colorScheme = COLORES) {
        Box {
            var vista by remember { mutableStateOf(Vista.LOGIN) }
            when (vista) {
                Vista.LOGIN -> LoginScreen(db, entrar = { vista = Vista.APP }, irRegistro = { vista = Vista.REGISTRO })
                Vista.REGISTRO -> RegistroScreen(db, entrar = { vista = Vista.APP }, volver = { vista = Vista.LOGIN })
                Vista.APP -> Principal(db) { db.cerrarSesion(); vista = Vista.LOGIN }
            }
            Aviso()
        }
    }
}

@Composable
fun Principal(db: Db, salir: () -> Unit) {
    var pantalla by remember { mutableStateOf<Pantalla>(Pantalla.Jugadores) }
    val enLista = pantalla == Pantalla.Jugadores || pantalla == Pantalla.Partidos
    val atras = {
        pantalla = when (val p = pantalla) {
            is Pantalla.Detalle -> Pantalla.Jugadores
            is Pantalla.Reporte -> Pantalla.EnVivo(p.partidoId)
            else -> Pantalla.Partidos
        }
    }

    AtrasSistema(!enLista, atras)
    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            if (enLista) NavigationBar {
                NavigationBarItem(
                    selected = pantalla == Pantalla.Jugadores,
                    onClick = { pantalla = Pantalla.Jugadores },
                    icon = { Icon(Icons.Default.Person, null) },
                    label = { Text("Jugadores") }
                )
                NavigationBarItem(
                    selected = pantalla == Pantalla.Partidos,
                    onClick = { pantalla = Pantalla.Partidos },
                    icon = { Icon(Icons.Default.DateRange, null) },
                    label = { Text("Partidos") }
                )
            }
        }
    ) { relleno ->
        Box(Modifier.padding(relleno)) {
            when (val p = pantalla) {
                Pantalla.Jugadores -> JugadoresScreen(db, salir) { pantalla = Pantalla.Detalle(it) }
                Pantalla.Partidos -> PartidosScreen(db, salir) { pantalla = Pantalla.EnVivo(it) }
                is Pantalla.Detalle -> JugadorScreen(db, p.jugadorId, atras)
                is Pantalla.EnVivo -> PartidoScreen(db, p.partidoId, atras) { pantalla = Pantalla.Reporte(p.partidoId) }
                is Pantalla.Reporte -> ReporteScreen(db, p.partidoId, atras)
            }
        }
    }
}
