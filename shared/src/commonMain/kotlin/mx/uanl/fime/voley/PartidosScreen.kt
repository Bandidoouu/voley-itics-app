package mx.uanl.fime.voley

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun PartidosScreen(db: Db, salir: () -> Unit, abrir: (Long) -> Unit) {
    val datos = cargar(Unit) { Triple(db.partidos(), db.marcadores(), db.jugadores().isNotEmpty()) }
    val partidos = datos?.first.orEmpty()
    val hayJugadores = datos?.third == true
    var creando by remember { mutableStateOf(false) }
    var rival by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    Column(Modifier.fillMaxSize()) {
        Encabezado("Partidos") {
            IconButton(onClick = salir) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Salir") }
        }
        Button(
            onClick = { creando = true }, enabled = hayJugadores,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp).height(48.dp)
        ) { Text("Nuevo partido") }
        if (datos == null) Cargando()
        else if (!hayJugadores) Vacio("Primero agrega jugadores")
        else if (partidos.isEmpty()) Vacio("Aún no hay partidos.")
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(partidos) { p ->
                val sets = setsCerrados(p, datos?.second?.get(p.id).orEmpty())
                Tarjeta(alTocar = { abrir(p.id) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("vs ${p.rival}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(p.fecha, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Etiqueta(if (p.terminado) "Terminado" else "En juego")
                        }
                        Text(
                            "Sets ${sets.count { it.nosotros > it.rival }} - ${sets.count { it.rival > it.nosotros }}",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
    if (creando) AlertDialog(
        onDismissRequest = { creando = false },
        title = { Text("Nuevo partido") },
        text = { CampoTexto(rival, { rival = it }, "Rival") },
        confirmButton = {
            TextButton(enabled = !guardando && rival.isNotBlank(), onClick = {
                guardando = true
                alcance.launch {
                    val nuevo = enFondo { db.crearPartido(rival.trim()) }
                    guardando = false
                    if (nuevo != null) abrir(nuevo)
                }
            }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = { creando = false }) { Text("Cancelar") } }
    )
}
