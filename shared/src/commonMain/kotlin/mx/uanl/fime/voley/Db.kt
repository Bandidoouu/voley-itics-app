package mx.uanl.fime.voley

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

const val SUPABASE_URL = "https://xxizifpofjjeimrnxfaw.supabase.co"
const val SUPABASE_KEY = "sb_publishable_TAN0KzAz4RsL09PQdpBj2Q_h2xAvYhA"

data class Jugador(val id: Long, val nombre: String, val numero: Int, val posicion: String)
data class Partido(val id: Long, val rival: String, val fecha: String, val setActual: Int, val terminado: Boolean)
data class Marcador(val numSet: Int, val nosotros: Int, val rival: Int)
data class Estadistica(val tipo: String, val total: Int, val puntos: Int, val errores: Int)
data class Jugada(val jugador: String?, val tipo: String, val resultado: String)

class ErrorServidor(val codigo: Int, val respuesta: String) : Exception("Error $codigo: $respuesta")

private fun json(vararg pares: Pair<String, Any?>) = JsonObject(pares.associate { (clave, valor) ->
    clave to when (valor) {
        null -> JsonNull
        is Number -> JsonPrimitive(valor)
        is Boolean -> JsonPrimitive(valor)
        is JsonObject -> valor
        else -> JsonPrimitive(valor.toString())
    }
})

private fun JsonObject.texto(clave: String) = getValue(clave).jsonPrimitive.content
private fun JsonObject.entero(clave: String) = getValue(clave).jsonPrimitive.int
private fun JsonObject.largo(clave: String) = getValue(clave).jsonPrimitive.long

class Db {
    private val cliente = HttpClient { install(HttpTimeout) { requestTimeoutMillis = 10_000 } }
    private var token: String? = null
    private var refresco: String? = null

    private suspend fun pedir(metodo: HttpMethod, ruta: String, cuerpo: JsonObject? = null, reintentar: Boolean = true): String {
        val r = cliente.request(SUPABASE_URL + ruta) {
            method = metodo
            header("apikey", SUPABASE_KEY)
            header("Prefer", "return=representation")
            token?.let { header("Authorization", "Bearer $it") }
            if (cuerpo != null) {
                contentType(ContentType.Application.Json)
                setBody(cuerpo.toString())
            }
        }
        val codigo = r.status.value
        val texto = r.bodyAsText()
        if (codigo == 401 && reintentar && refresco != null) {
            refrescar()
            return pedir(metodo, ruta, cuerpo, false)
        }
        if (codigo >= 400) throw ErrorServidor(codigo, texto)
        return texto
    }

    private suspend fun <T> lista(ruta: String, fila: (JsonObject) -> T): List<T> =
        Json.parseToJsonElement(pedir(HttpMethod.Get, ruta)).jsonArray.map { fila(it.jsonObject) }

    private fun guardarSesion(respuesta: String) {
        val o = Json.parseToJsonElement(respuesta).jsonObject
        token = o.texto("access_token")
        refresco = o.texto("refresh_token")
    }

    private suspend fun refrescar() {
        val r = refresco
        token = null
        refresco = null
        guardarSesion(pedir(HttpMethod.Post, "/auth/v1/token?grant_type=refresh_token", json("refresh_token" to r), false))
    }

    private fun correo(usuario: String) = "${usuario.lowercase()}@voley-itics.app"

    suspend fun crearCuenta(usuario: String, contrasena: String) = guardarSesion(
        pedir(
            HttpMethod.Post, "/auth/v1/signup",
            json("email" to correo(usuario), "password" to contrasena, "data" to json("usuario" to usuario))
        )
    )

    suspend fun iniciarSesion(usuario: String, contrasena: String): Boolean = try {
        guardarSesion(
            pedir(HttpMethod.Post, "/auth/v1/token?grant_type=password", json("email" to correo(usuario), "password" to contrasena))
        )
        true
    } catch (e: ErrorServidor) {
        if (e.codigo == 400) false else throw e
    }

    fun cerrarSesion() {
        token = null
        refresco = null
    }

    private fun aJugador(o: JsonObject) = Jugador(o.largo("id"), o.texto("nombre"), o.entero("numero"), o.texto("posicion"))

    private fun aPartido(o: JsonObject) =
        Partido(o.largo("id"), o.texto("rival"), o.texto("fecha"), o.entero("set_actual"), o.getValue("terminado").jsonPrimitive.boolean)

    private fun aMarcador(o: JsonObject) = Marcador(o.entero("set_num"), o.entero("nosotros"), o.entero("rival"))

    private fun aEstadistica(o: JsonObject) = Estadistica(o.texto("tipo"), o.entero("total"), o.entero("puntos"), o.entero("errores"))

    suspend fun jugadores() = lista("/rest/v1/jugador?select=*&order=numero") { aJugador(it) }

    suspend fun jugador(id: Long) = lista("/rest/v1/jugador?select=*&id=eq.$id") { aJugador(it) }.first()

    suspend fun agregarJugador(nombre: String, numero: Int, posicion: String) {
        pedir(HttpMethod.Post, "/rest/v1/jugador", json("nombre" to nombre, "numero" to numero, "posicion" to posicion))
    }

    suspend fun estadisticas(jugadorId: Long) = lista("/rest/v1/estadistica?select=*&jugador_id=eq.$jugadorId") { aEstadistica(it) }

    suspend fun estadisticasPorJugador() =
        lista("/rest/v1/estadistica?select=*") { it.largo("jugador_id") to aEstadistica(it) }
            .groupBy({ it.first }, { it.second })

    suspend fun estadisticasPartido(partidoId: Long) =
        lista("/rest/v1/estadistica_partido?select=*&partido_id=eq.$partidoId") { it.largo("jugador_id") to aEstadistica(it) }
            .groupBy({ it.first }, { it.second })

    suspend fun partidos() = lista("/rest/v1/partido?select=*&order=id.desc") { aPartido(it) }

    suspend fun partido(id: Long) = lista("/rest/v1/partido?select=*&id=eq.$id") { aPartido(it) }.first()

    suspend fun crearPartido(rival: String): Long =
        Json.parseToJsonElement(pedir(HttpMethod.Post, "/rest/v1/partido", json("rival" to rival))).jsonArray[0].jsonObject.largo("id")

    suspend fun siguienteSet(id: Long) {
        pedir(HttpMethod.Post, "/rest/v1/rpc/siguiente_set", json("p_partido" to id))
    }

    suspend fun cambiarTerminado(id: Long, terminado: Boolean) {
        pedir(HttpMethod.Patch, "/rest/v1/partido?id=eq.$id", json("terminado" to terminado))
    }

    suspend fun registrar(partidoId: Long, numSet: Int, jugadorId: Long?, tipo: String, resultado: String) {
        pedir(
            HttpMethod.Post, "/rest/v1/jugada",
            json("partido_id" to partidoId, "set_num" to numSet, "jugador_id" to jugadorId, "tipo" to tipo, "resultado" to resultado)
        )
    }

    suspend fun deshacer(partidoId: Long) {
        pedir(HttpMethod.Post, "/rest/v1/rpc/deshacer", json("p_partido" to partidoId))
    }

    suspend fun marcador(partidoId: Long) = lista("/rest/v1/marcador?select=*&partido_id=eq.$partidoId&order=set_num") { aMarcador(it) }

    suspend fun marcadores() =
        lista("/rest/v1/marcador?select=*&order=set_num") { it.largo("partido_id") to aMarcador(it) }
            .groupBy({ it.first }, { it.second })

    suspend fun jugadas(partidoId: Long) = lista(
        "/rest/v1/jugada?select=tipo,resultado,jugador(nombre)&partido_id=eq.$partidoId&order=id.desc&limit=10"
    ) { o ->
        val j = o["jugador"]
        Jugada(if (j is JsonObject) j.texto("nombre") else null, o.texto("tipo"), o.texto("resultado"))
    }
}
