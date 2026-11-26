package mx.uanl.fime.voley

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun JugadoresScreen(db: Db, salir: () -> Unit, abrir: (Long) -> Unit) {
    var version by remember { mutableIntStateOf(0) }
    val datos = cargar(version) { db.jugadores() to db.estadisticasPorJugador() }
    val jugadores = datos?.first.orEmpty()
    val puntajes = jugadores.associate { it.id to puntaje(it.posicion, datos?.second?.get(it.id).orEmpty()) }
    val conDatos = puntajes.values.filterNotNull()
    val promedio = if (conDatos.isEmpty()) null else conDatos.average().toInt()
    var agregando by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Encabezado("Jugadores") {
            IconButton(onClick = salir) { Icon(Icons.AutoMirrored.Filled.ExitToApp, "Salir") }
        }
        Button(
            onClick = { agregando = true },
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 16.dp).height(48.dp)
        ) { Text("Agregar jugador") }
        if (datos == null) Cargando()
        else if (jugadores.isEmpty()) Vacio("Aún no hay jugadores.")
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (jugadores.isNotEmpty()) item { PuntajeEquipo(promedio, conDatos.size) }
            items(jugadores.sortedByDescending { puntajes[it.id] ?: -1 }) { j ->
                val p = puntajes[j.id]
                Tarjeta(alTocar = { abrir(j.id) }) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Numero(j.numero)
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(j.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Etiqueta(nombre(j.posicion))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                p?.toString() ?: "-", style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = colorPuntaje(p)
                            )
                            Text(
                                if (p == null) "sin datos" else calificacion(p),
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
    if (agregando) NuevoJugadorDialog(db) { agregando = false; version++ }
}

@Composable
fun PuntajeEquipo(promedio: Int?, jugadoresConDatos: Int) {
    val claro = Color.White.copy(alpha = 0.75f)
    Tarjeta(color = MaterialTheme.colorScheme.primary) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("PUNTAJE DEL EQUIPO", style = MaterialTheme.typography.labelMedium, color = claro)
            Text(promedio?.toString() ?: "-", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                if (promedio == null) "Registra jugadas en un partido para ver puntajes"
                else "${calificacion(promedio)}, promedio de $jugadoresConDatos jugadores con datos",
                color = claro
            )
        }
    }
}

@Composable
fun NuevoJugadorDialog(db: Db, cerrar: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var posicion by remember { mutableStateOf(POSICIONES.first()) }
    var error by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = cerrar,
        title = { Text("Nuevo jugador") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto(nombre, { nombre = it }, "Nombre")
                OutlinedTextField(
                    numero, { numero = it.filter { c -> c.isDigit() } }, Modifier.fillMaxWidth(),
                    label = { Text("Número") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                POSICIONES.forEach { p ->
                    Row(Modifier.fillMaxWidth().clickable { posicion = p }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = posicion == p, onClick = { posicion = p })
                        Text(nombre(p))
                    }
                }
                if (error) Text("Número repetido o fuera de 0 a 99", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(enabled = !guardando && nombre.isNotBlank() && numero.isNotBlank(), onClick = {
                guardando = true
                alcance.launch {
                    val ok = enFondo { db.agregarJugador(nombre.trim(), numero.toInt(), posicion) } != null
                    guardando = false
                    if (ok) cerrar() else error = true
                }
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = cerrar) { Text("Cancelar") } }
    )
}
