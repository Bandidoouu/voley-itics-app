package mx.uanl.fime.voley

import kotlin.math.abs

val PESOS = mapOf(
    "ARMADOR" to mapOf("ARMADO" to 50, "DEFENSA" to 20, "SAQUE" to 15, "BLOQUEO" to 10, "ATAQUE" to 5),
    "OPUESTO" to mapOf("ATAQUE" to 50, "BLOQUEO" to 20, "SAQUE" to 15, "DEFENSA" to 15),
    "PUNTA" to mapOf("ATAQUE" to 40, "RECEPCION" to 30, "DEFENSA" to 15, "SAQUE" to 10, "BLOQUEO" to 5),
    "CENTRAL" to mapOf("BLOQUEO" to 40, "ATAQUE" to 35, "SAQUE" to 15, "DEFENSA" to 10),
    "LIBERO" to mapOf("RECEPCION" to 50, "DEFENSA" to 40, "ARMADO" to 10),
)
val POSICIONES = PESOS.keys

val TIPOS = listOf("SAQUE", "RECEPCION", "ARMADO", "ATAQUE", "BLOQUEO", "DEFENSA")

val TERMINALES = setOf("SAQUE", "ATAQUE", "BLOQUEO")

fun eficacia(e: Estadistica): Int {
    val continuas = e.total - e.puntos - e.errores
    val valorContinua = if (e.tipo in TERMINALES) 0.5 else 1.0
    return ((e.puntos + continuas * valorContinua) * 100 / e.total).toInt()
}

fun puntaje(posicion: String, stats: List<Estadistica>): Int? {
    val pesos = PESOS.getValue(posicion)
    val usadas = stats.filter { it.tipo in pesos }
    if (usadas.isEmpty()) return null
    return usadas.sumOf { eficacia(it) * pesos.getValue(it.tipo) } / usadas.sumOf { pesos.getValue(it.tipo) }
}

fun mejorPosicion(posicion: String, stats: List<Estadistica>) =
    (listOf(posicion) + POSICIONES).maxBy { puntaje(it, stats) ?: -1 }

fun calificacion(puntaje: Int) = when {
    puntaje >= 80 -> "Excelente"
    puntaje >= 60 -> "Bien"
    puntaje >= 40 -> "Regular"
    else -> "Bajo"
}

fun setCompleto(m: Marcador) =
    maxOf(m.nosotros, m.rival) >= (if (m.numSet >= 5) 15 else 25) && abs(m.nosotros - m.rival) >= 2

fun partidoDecidido(sets: List<Marcador>) =
    sets.count { it.nosotros > it.rival } >= 3 || sets.count { it.rival > it.nosotros } >= 3

fun setsCerrados(p: Partido, marcador: List<Marcador>) = marcador.filter { it.numSet < p.setActual || p.terminado }

fun plural(n: Int, palabra: String) = "$n $palabra" + if (n == 1) "" else if (palabra.endsWith("r")) "es" else "s"

data class FilaReporte(val jugador: Jugador, val toques: Int, val puntos: Int, val errores: Int, val puntaje: Int?)

fun filasReporte(jugadores: List<Jugador>, stats: Map<Long, List<Estadistica>>) =
    jugadores.filter { it.id in stats }.map { j ->
        val s = stats.getValue(j.id)
        FilaReporte(j, s.sumOf { it.total }, s.sumOf { it.puntos }, s.sumOf { it.errores }, puntaje(j.posicion, s))
    }.sortedByDescending { it.puntaje ?: -1 }

fun porToque(stats: Map<Long, List<Estadistica>>) =
    stats.values.flatten().groupBy { it.tipo }
        .map { (tipo, l) -> Estadistica(tipo, l.sumOf { it.total }, l.sumOf { it.puntos }, l.sumOf { it.errores }) }
        .sortedBy { TIPOS.indexOf(it.tipo) }

fun textoReporte(p: Partido, marcador: List<Marcador>, filas: List<FilaReporte>): String {
    val cerrados = setsCerrados(p, marcador)
    val aFavor = marcador.sumOf { it.nosotros }
    val enContra = marcador.sumOf { it.rival }
    val errores = filas.sumOf { it.errores }
    return buildString {
        appendLine("Reporte Voley-itics")
        appendLine("vs ${p.rival} (${p.fecha})")
        appendLine("Sets: ${cerrados.count { it.nosotros > it.rival }} - ${cerrados.count { it.rival > it.nosotros }}")
        appendLine("Parciales: " + marcador.joinToString(", ") { "${it.nosotros}-${it.rival}" })
        appendLine("Puntos a favor: $aFavor. En contra: $enContra ($errores por errores propios).")
        appendLine()
        appendLine("Jugadores:")
        filas.forEach {
            appendLine(
                "#${it.jugador.numero} ${it.jugador.nombre}: ${plural(it.toques, "toque")}, " +
                    "${plural(it.puntos, "punto")}, ${plural(it.errores, "error")}, puntaje ${it.puntaje ?: "-"}"
            )
        }
    }.trim()
}
