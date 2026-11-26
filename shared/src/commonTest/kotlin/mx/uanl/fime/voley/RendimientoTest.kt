package mx.uanl.fime.voley

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test

class RendimientoTest {
    private val ataque = Estadistica("ATAQUE", total = 10, puntos = 4, errores = 2)
    private val defensa = Estadistica("DEFENSA", total = 10, puntos = 0, errores = 1)

    @Test
    fun eficacia_terminal_y_continua() {
        assertEquals(60, eficacia(ataque))
        assertEquals(90, eficacia(defensa))
    }

    @Test
    fun puntaje_ponderado_por_posicion() {
        assertEquals(68, puntaje("PUNTA", listOf(ataque, defensa)))
        assertNull(puntaje("LIBERO", listOf(ataque)))
        assertEquals("LIBERO", mejorPosicion("PUNTA", listOf(ataque, defensa)))
        assertEquals("LIBERO", mejorPosicion("LIBERO", listOf(defensa)))
    }

    @Test
    fun set_completo_a_25_o_15_con_ventaja_de_2() {
        assertTrue(setCompleto(Marcador(1, 25, 23)))
        assertFalse(setCompleto(Marcador(1, 25, 24)))
        assertTrue(setCompleto(Marcador(1, 24, 26)))
        assertTrue(setCompleto(Marcador(5, 15, 13)))
        assertFalse(setCompleto(Marcador(1, 15, 13)))
    }

    @Test
    fun partido_decidido_con_3_sets() {
        val ganado = Marcador(1, 25, 20)
        val perdido = Marcador(2, 20, 25)
        assertFalse(partidoDecidido(listOf(ganado, ganado, perdido, perdido)))
        assertTrue(partidoDecidido(listOf(ganado, perdido, ganado, ganado)))
        assertTrue(partidoDecidido(listOf(perdido, perdido, perdido)))
    }

    @Test
    fun plural_en_espanol() {
        assertEquals("1 toque", plural(1, "toque"))
        assertEquals("3 toques", plural(3, "toque"))
        assertEquals("0 errores", plural(0, "error"))
        assertEquals("1 error", plural(1, "error"))
    }

    @Test
    fun reporte_por_partido() {
        val ana = Jugador(1, "Ana", 4, "PUNTA")
        val luz = Jugador(2, "Luz", 7, "LIBERO")
        val stats = mapOf(1L to listOf(ataque, defensa), 2L to listOf(defensa))
        val filas = filasReporte(listOf(ana, luz, Jugador(3, "Sin datos", 9, "CENTRAL")), stats)
        assertEquals(listOf("Luz", "Ana"), filas.map { it.jugador.nombre })
        assertEquals(20, filas[1].toques)
        assertEquals(3, filas[1].errores)
        assertEquals(listOf("ATAQUE", "DEFENSA"), porToque(stats).map { it.tipo })
        assertEquals(20, porToque(stats)[1].total)

        val partido = Partido(1, "Tigres", "2026-11-25", 3, true)
        val texto = textoReporte(partido, listOf(Marcador(1, 25, 20), Marcador(2, 20, 25), Marcador(3, 15, 10)), filas)
        assertTrue("Sets: 2 - 1" in texto)
        assertTrue("Puntos a favor: 60. En contra: 55 (4 por errores propios)." in texto)
        assertTrue("#4 Ana: 20 toques, 4 puntos, 3 errores, puntaje 68" in texto)
    }
}
