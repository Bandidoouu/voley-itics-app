package mx.uanl.fime.voley

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

val RESULTADOS = listOf("PUNTO", "CONTINUA", "ERROR")

@Composable
fun PartidoScreen(db: Db, id: Long, atras: () -> Unit, verReporte: () -> Unit) {
    var version by remember { mutableIntStateOf(0) }
    val datos = cargar(version) { Triple(db.partido(id), db.marcador(id), db.jugadas(id)) }
    val jugadores = cargar(Unit) { db.jugadores() }.orEmpty()
    var jugador by remember { mutableStateOf<Jugador?>(null) }
    var tipo by remember { mutableStateOf<String?>(null) }
    var confirmarSet by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    val gris = MaterialTheme.colorScheme.onSurfaceVariant
    val alcance = rememberCoroutineScope()

    fun enviar(bloque: suspend () -> Unit) {
        alcance.launch {
            enFondo(bloque)
            version++
        }
    }

    if (datos == null) {
        Column(Modifier.fillMaxSize()) { Encabezado("Partido", atras); Cargando() }
        return
    }
    val (partido, marcador, jugadas) = datos
    val setActual = marcador.find { it.numSet == partido.setActual } ?: Marcador(partido.setActual, 0, 0)
    val setsCerrados = setsCerrados(partido, marcador)
    val hayJugadasEnSet = marcador.any { it.numSet == partido.setActual }

    val j = jugador
    val t = tipo
    val paso = when {
        j == null -> "¿Quién tocó el balón?"
        t == null -> "${j.nombre}: ¿qué hizo?"
        else -> "${j.nombre} (${nombre(t)}): ¿cómo terminó?"
    }

    Column(Modifier.fillMaxSize()) {
        Encabezado("vs ${partido.rival}", atras)
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Marcador(setActual, partido.setActual, setsCerrados)

            if (partido.terminado) {
                Text("Partido terminado. Puedes reabrirlo para seguir registrando.", color = gris)
                Button(onClick = verReporte, Modifier.fillMaxWidth().height(48.dp)) { Text("Ver reporte del partido") }
                OutlinedButton(onClick = { enviar { db.cambiarTerminado(id, false) } }, Modifier.fillMaxWidth()) { Text("Reabrir partido") }
            } else {
                if (setCompleto(setActual)) Tarjeta(color = MaterialTheme.colorScheme.primaryContainer) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Set ${partido.setActual} completo: ${setActual.nosotros}-${setActual.rival}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold
                        )
                        if (partidoDecidido(setsCerrados + setActual)) {
                            Button(onClick = { enviar { db.cambiarTerminado(id, true) } }, Modifier.fillMaxWidth()) { Text("Terminar partido") }
                        } else {
                            Button(onClick = { confirmarSet = true }, Modifier.fillMaxWidth()) { Text("Cerrar set") }
                        }
                    }
                }

                Text(paso, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                when {
                    j == null -> {
                        if (jugadores.isEmpty()) Text("Primero agrega jugadores en la pestaña Jugadores", color = gris)
                        Opciones(jugadores.map { "#${it.numero} ${it.nombre}" }) { jugador = jugadores[it] }
                    }
                    t == null -> {
                        Opciones(TIPOS.map { nombre(it) }) { tipo = TIPOS[it] }
                        TextButton(onClick = { jugador = null }) { Text("Volver") }
                    }
                    else -> {
                        RESULTADOS.forEach { r ->
                            val (fondo, texto, ayuda) = when (r) {
                                "PUNTO" -> Triple(VERDE, Color.White, "anotamos")
                                "ERROR" -> Triple(MaterialTheme.colorScheme.error, Color.White, "anota el rival")
                                else -> Triple(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, "continúa la jugada")
                            }
                            Button(
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = fondo, contentColor = texto),
                                onClick = {
                                    jugador = null
                                    tipo = null
                                    enviar { db.registrar(id, partido.setActual, j.id, t, r) }
                                }
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(nombre(r), fontWeight = FontWeight.Bold)
                                    Text(ayuda, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        TextButton(onClick = { tipo = null }) { Text("Volver") }
                    }
                }

                HorizontalDivider()
                OutlinedButton(
                    onClick = { enviar { db.registrar(id, partido.setActual, null, "RIVAL", "ERROR") } },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Punto del rival") }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { enviar { db.deshacer(id) } }, enabled = hayJugadasEnSet, modifier = Modifier.weight(1f)
                    ) { Text("Deshacer") }
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { menu = true }, Modifier.fillMaxWidth()) { Text("Más") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Ver reporte") }, onClick = { menu = false; verReporte() })
                            DropdownMenuItem(
                                text = { Text("Cerrar set ${partido.setActual}") },
                                onClick = { menu = false; confirmarSet = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Terminar partido") },
                                onClick = { menu = false; enviar { db.cambiarTerminado(id, true) } }
                            )
                        }
                    }
                }
            }

            Seccion("ÚLTIMAS JUGADAS")
            if (jugadas.isEmpty()) Text("Sin jugadas todavía", color = gris)
            else Tarjeta {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    jugadas.forEach {
                        Text(if (it.jugador == null) "Punto del rival" else "${it.jugador}: ${nombre(it.tipo)}, ${nombre(it.resultado)}")
                    }
                }
            }
        }
    }

    if (confirmarSet) AlertDialog(
        onDismissRequest = { confirmarSet = false },
        title = { Text("¿Cerrar el set ${partido.setActual}?") },
        text = { Text("Queda ${setActual.nosotros} - ${setActual.rival} y se abre el set ${partido.setActual + 1}. Esto no se puede deshacer.") },
        confirmButton = { TextButton(onClick = { confirmarSet = false; enviar { db.siguienteSet(id) } }) { Text("Cerrar set") } },
        dismissButton = { TextButton(onClick = { confirmarSet = false }) { Text("Cancelar") } }
    )
}

@Composable
fun Marcador(actual: Marcador, numSet: Int, cerrados: List<Marcador>) {
    val claro = Color.White.copy(alpha = 0.75f)
    Tarjeta(color = MaterialTheme.colorScheme.primary) {
        Row(
            Modifier.fillMaxWidth().padding(top = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("NOSOTROS", style = MaterialTheme.typography.labelMedium, color = claro)
                Text("${actual.nosotros}", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Surface(shape = MaterialTheme.shapes.small, color = Color.White.copy(alpha = 0.18f)) {
                Text("SET $numSet", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RIVAL", style = MaterialTheme.typography.labelMedium, color = claro)
                Text("${actual.rival}", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Text(
            "Sets ganados ${cerrados.count { it.nosotros > it.rival }} - ${cerrados.count { it.rival > it.nosotros }}   " +
                cerrados.joinToString("  ") { "${it.nosotros}-${it.rival}" },
            Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp), color = claro
        )
    }
}
