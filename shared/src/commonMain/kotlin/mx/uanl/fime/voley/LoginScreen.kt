package mx.uanl.fime.voley

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(db: Db, entrar: () -> Unit, irRegistro: () -> Unit) {
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    fun entrarConCuenta() {
        cargando = true
        alcance.launch {
            val ok = enFondo { db.iniciarSesion(usuario.trim(), contrasena) }
            cargando = false
            if (ok == true) entrar() else if (ok == false) error = "Usuario o contraseña incorrectos"
        }
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).imePadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Logo(Modifier.size(96.dp))
        Text(
            "Voley-itics", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
        )
        Text("Estadísticas para tu equipo", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Tarjeta {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(usuario, { usuario = it }, "Usuario")
                CampoTexto(contrasena, { contrasena = it }, "Contraseña", contrasena = true)
                if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
                Button(
                    onClick = { entrarConCuenta() }, Modifier.fillMaxWidth().height(48.dp),
                    enabled = !cargando && usuario.isNotBlank() && contrasena.isNotEmpty()
                ) { Text(if (cargando) "Entrando..." else "Entrar") }
                OutlinedButton(onClick = irRegistro, Modifier.fillMaxWidth().height(48.dp)) { Text("Crear cuenta nueva") }
            }
        }
    }
}

@Composable
fun RegistroScreen(db: Db, entrar: () -> Unit, volver: () -> Unit) {
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var confirmacion by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var cargando by remember { mutableStateOf(false) }
    val alcance = rememberCoroutineScope()

    fun crear() {
        error = when {
            usuario.isBlank() -> "Escribe un usuario"
            !Regex("[A-Za-z0-9._-]+").matches(usuario.trim()) -> "El usuario solo puede tener letras sin acentos, números, punto o guion"
            contrasena.length < 8 -> "La contraseña necesita al menos 8 caracteres"
            contrasena != confirmacion -> "Las contraseñas no coinciden"
            else -> ""
        }
        if (error.isNotEmpty()) return
        cargando = true
        alcance.launch {
            val r = runCatching { db.crearCuenta(usuario.trim(), contrasena) }
            cargando = false
            r.onSuccess { entrar() }.onFailure {
                error = if (it is ErrorServidor && "already" in it.respuesta) "Ese usuario ya existe"
                else "No se pudo crear la cuenta. Revisa tu internet."
            }
        }
    }

    AtrasSistema(true, volver)
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Encabezado("Crear cuenta", volver)
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Crea tu cuenta de entrenador para guardar a tu equipo y tus partidos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            CampoTexto(usuario, { usuario = it }, "Usuario")
            CampoTexto(contrasena, { contrasena = it }, "Contraseña (mínimo 8)", contrasena = true)
            CampoTexto(confirmacion, { confirmacion = it }, "Confirmar contraseña", contrasena = true)
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
            Button(onClick = { crear() }, Modifier.fillMaxWidth().height(48.dp), enabled = !cargando) {
                Text(if (cargando) "Creando..." else "Crear cuenta")
            }
        }
    }
}
