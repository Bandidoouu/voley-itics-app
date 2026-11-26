package mx.uanl.fime.voley

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class DatosReporte(
    val partido: Partido,
    val marcador: List<Marcador>,
    val jugadores: List<Jugador>,
    val stats: Map<Long, List<Estadistica>>,
)

@Composable
fun ReporteScreen(db: Db, id: Long, atras: () -> Unit) {
    val compartir = rememberCompartir()
    val datos = cargar(id) { DatosReporte(db.partido(id), db.marcador(id), db.jugadores(), db.estadisticasPartido(id)) }
    if (datos == null) {
        Column(Modifier.fillMaxSize()) { Encabezado("Reporte", atras); Cargando() }
        return
    }
    val (partido, marcador, jugadores, stats) = datos

    val filas = filasReporte(jugadores, stats)
    val cerrados = setsCerrados(partido, marcador)
    val aFavor = marcador.sumOf { it.nosotros }
    val enContra = marcador.sumOf { it.rival }
    val errores = filas.sumOf { it.errores }
    val gris = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxSize()) {
        Encabezado("Reporte vs ${partido.rival}", atras) {
            IconButton(onClick = { compartir(textoReporte(partido, marcador, filas)) }) { Icon(Icons.Default.Share, "Compartir") }
        }
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Tarjeta(color = MaterialTheme.colorScheme.primary) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (partido.terminado) "RESULTADO FINAL" else "PARTIDO EN JUEGO",
                        style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f)
                    )
                    Text(
                        "${cerrados.count { it.nosotros > it.rival }} - ${cerrados.count { it.rival > it.nosotros }}",
                        style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color.White
                    )
                    Text(
                        marcador.joinToString("   ") { "${it.nosotros}-${it.rival}" }.ifEmpty { "Sin puntos todavía" },
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Dato("A favor", aFavor, Modifier.weight(1f))
                Dato("En contra", enContra, Modifier.weight(1f))
                Dato("Errores propios", errores, Modifier.weight(1f))
            }

            Seccion("POR JUGADOR")
            if (filas.isEmpty()) Text("Sin jugadas registradas", color = gris)
            filas.forEach { f ->
                Tarjeta {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Numero(f.jugador.numero)
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(f.jugador.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${plural(f.toques, "toque")}, ${plural(f.puntos, "punto")}, ${plural(f.errores, "error")}",
                                style = MaterialTheme.typography.bodySmall, color = gris
                            )
                        }
                        Text(
                            f.puntaje?.toString() ?: "-", style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold, color = colorPuntaje(f.puntaje)
                        )
                    }
                }
            }

            val toques = porToque(stats)
            if (toques.isNotEmpty()) Seccion("POR TIPO DE TOQUE")
            toques.forEach { TarjetaEficacia(it) }
        }
    }
}

@Composable
fun Dato(titulo: String, valor: Int, modifier: Modifier) = Tarjeta(modifier) {
    Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$valor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(titulo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
