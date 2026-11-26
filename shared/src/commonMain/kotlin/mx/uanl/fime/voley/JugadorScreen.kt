package mx.uanl.fime.voley

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun JugadorScreen(db: Db, id: Long, atras: () -> Unit) {
    val datos = cargar(id) { db.jugador(id) to db.estadisticas(id) }
    if (datos == null) {
        Column(Modifier.fillMaxSize()) { Encabezado("Jugador", atras); Cargando() }
        return
    }
    val (jugador, stats) = datos
    val puntaje = puntaje(jugador.posicion, stats)
    val gris = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxSize()) {
        Encabezado(jugador.nombre, atras)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Tarjeta {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("#${jugador.numero}", color = gris)
                        Etiqueta(nombre(jugador.posicion))
                    }
                    Spacer(Modifier.height(16.dp))
                    if (puntaje == null) {
                        Text("Sin jugadas registradas", color = gris)
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = { puntaje / 100f }, Modifier.size(130.dp),
                                strokeWidth = 10.dp, color = colorPuntaje(puntaje), trackColor = MaterialTheme.colorScheme.primaryContainer
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "$puntaje", style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold, color = colorPuntaje(puntaje)
                                )
                                Text("de 100", style = MaterialTheme.typography.bodySmall, color = gris)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(calificacion(puntaje), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        val toques = stats.sumOf { it.total }
                        Text(
                            if (toques >= 10) "Mejor posición según datos: ${nombre(mejorPosicion(jugador.posicion, stats))}"
                            else "Registra al menos 10 toques para sugerir posición ($toques)",
                            color = gris
                        )
                    }
                }
            }
            if (stats.isNotEmpty()) Seccion("EFICACIA POR TOQUE")
            stats.forEach { TarjetaEficacia(it) }
        }
    }
}
