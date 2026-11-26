package mx.uanl.fime.voley

import androidx.compose.runtime.Composable

@Composable
expect fun AtrasSistema(activo: Boolean, alVolver: () -> Unit)

@Composable
expect fun rememberCompartir(): (String) -> Unit
