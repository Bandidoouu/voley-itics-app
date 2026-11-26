package mx.uanl.fime.voley

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

val NOMBRES = mapOf(
    "SAQUE" to "Saque", "RECEPCION" to "Recepción", "ARMADO" to "Colocación",
    "ATAQUE" to "Ataque", "BLOQUEO" to "Bloqueo", "DEFENSA" to "Defensa",
    "PUNTO" to "Punto", "CONTINUA" to "Sigue", "ERROR" to "Error",
    "ARMADOR" to "Colocador", "OPUESTO" to "Opuesto", "PUNTA" to "Punta",
    "CENTRAL" to "Central", "LIBERO" to "Líbero",
)
fun nombre(codigo: String) = NOMBRES[codigo] ?: codigo

val VERDE = Color(0xFF15803D)

@Composable
fun colorPuntaje(p: Int?) = when {
    p == null -> MaterialTheme.colorScheme.onSurfaceVariant
    p >= 60 -> VERDE
    p >= 40 -> Color(0xFFD97706)
    else -> MaterialTheme.colorScheme.error
}

@Composable
fun Encabezado(titulo: String, atras: (() -> Unit)? = null, acciones: @Composable () -> Unit = {}) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (atras != null) IconButton(onClick = atras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás") }
            Text(
                titulo, Modifier.weight(1f).padding(start = 8.dp),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold
            )
            acciones()
        }
    }
}

@Composable
fun Tarjeta(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    alTocar: (() -> Unit)? = null,
    contenido: @Composable ColumnScope.() -> Unit
) {
    val colores = CardDefaults.cardColors(containerColor = color)
    val forma = RoundedCornerShape(16.dp)
    val plano = CardDefaults.cardElevation(0.dp)
    if (alTocar == null) Card(modifier.fillMaxWidth(), shape = forma, colors = colores, elevation = plano, content = contenido)
    else Card(alTocar, modifier.fillMaxWidth(), shape = forma, colors = colores, elevation = plano, content = contenido)
}

@Composable
fun Etiqueta(texto: String) = Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
    Text(
        texto, Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer
    )
}

@Composable
fun CampoTexto(valor: String, cambiar: (String) -> Unit, etiqueta: String, contrasena: Boolean = false) =
    OutlinedTextField(
        valor, cambiar, Modifier.fillMaxWidth(),
        label = { Text(etiqueta) }, singleLine = true, shape = RoundedCornerShape(12.dp),
        visualTransformation = if (contrasena) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (contrasena) KeyboardType.Password else KeyboardType.Text)
    )

@Composable
fun Seccion(texto: String) = Text(
    texto, style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold
)

@Composable
fun Vacio(texto: String) = Text(
    texto, Modifier.fillMaxWidth().padding(32.dp),
    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun Numero(n: Int) = Box(
    Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
    contentAlignment = Alignment.Center
) {
    Text(
        "$n", color = MaterialTheme.colorScheme.onPrimaryContainer,
        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
    )
}

@Composable
fun Opciones(textos: List<String>, alElegir: (Int) -> Unit) {
    textos.indices.chunked(2).forEach { fila ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fila.forEach { i ->
                FilledTonalButton(
                    onClick = { alElegir(i) }, Modifier.weight(1f).height(56.dp), contentPadding = PaddingValues(4.dp)
                ) { Text(textos[i], maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            repeat(2 - fila.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

object Avisos {
    var texto by mutableStateOf<String?>(null)
}

suspend fun <T> enFondo(bloque: suspend () -> T): T? {
    val r = runCatching { bloque() }
    r.exceptionOrNull()?.let {
        if (it is CancellationException) throw it
        Avisos.texto = if (it is ErrorServidor) "No se pudo completar. Intenta de nuevo." else "Sin conexión. Revisa tu internet."
    }
    return r.getOrNull()
}

@Composable
fun Aviso() {
    val texto = Avisos.texto ?: return
    LaunchedEffect(texto) {
        delay(3500)
        Avisos.texto = null
    }
    Box(Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, bottom = 120.dp), contentAlignment = Alignment.BottomCenter) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF323232)) {
            Text(texto, Modifier.padding(horizontal = 18.dp, vertical = 12.dp), color = Color.White, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun Logo(modifier: Modifier) {
    val azul = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val u = size.width / 100f
        drawCircle(azul, radius = 42 * u, center = center, style = Stroke(6 * u))
        listOf(31f to 55f, 45f to 43f, 59f to 31f).forEach { (x, arriba) ->
            drawRoundRect(azul, Offset(x * u, arriba * u), Size(10 * u, (70 - arriba) * u), CornerRadius(5 * u))
        }
        drawLine(azul, Offset(27 * u, 72 * u), Offset(73 * u, 72 * u), 4 * u, StrokeCap.Round)
    }
}

@Composable
fun <T> cargar(clave: Any?, bloque: suspend () -> T): T? {
    var valor by remember { mutableStateOf<T?>(null) }
    LaunchedEffect(clave) { enFondo(bloque)?.let { valor = it } }
    return valor
}

@Composable
fun Cargando() = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }

@Composable
fun TarjetaEficacia(e: Estadistica) = Tarjeta {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(nombre(e.tipo), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("${eficacia(e)}%", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        }
        LinearProgressIndicator(progress = { eficacia(e) / 100f }, Modifier.fillMaxWidth().height(8.dp))
        Text(
            "${plural(e.total, "toque")}, ${plural(e.puntos, "punto")}, ${plural(e.errores, "error")}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
