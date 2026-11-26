const SUPABASE_URL = "https://xxizifpofjjeimrnxfaw.supabase.co"
const SUPABASE_KEY = "sb_publishable_TAN0KzAz4RsL09PQdpBj2Q_h2xAvYhA"

export class ErrorServidor extends Error {
  constructor(codigo, respuesta) {
    super(`Error ${codigo}: ${respuesta}`)
    this.codigo = codigo
    this.respuesta = respuesta
  }
}

let token = null
let refresco = null
try { refresco = localStorage.getItem("refresco") } catch {}

async function pedir(metodo, ruta, cuerpo, reintentar = true) {
  const encabezados = { apikey: SUPABASE_KEY, "Content-Type": "application/json", Prefer: "return=representation" }
  if (token) encabezados.Authorization = `Bearer ${token}`
  const r = await fetch(SUPABASE_URL + ruta, {
    method: metodo,
    headers: encabezados,
    body: cuerpo ? JSON.stringify(cuerpo) : undefined,
    signal: AbortSignal.timeout(10000),
  })
  const texto = await r.text()
  if (r.status === 401 && reintentar && refresco) {
    await refrescar()
    return pedir(metodo, ruta, cuerpo, false)
  }
  if (!r.ok) throw new ErrorServidor(r.status, texto)
  return texto ? JSON.parse(texto) : null
}

function guardarSesion(s) {
  token = s.access_token
  refresco = s.refresh_token
  try { localStorage.setItem("refresco", refresco) } catch {}
}

async function refrescar() {
  const r = refresco
  token = null
  refresco = null
  guardarSesion(await pedir("POST", "/auth/v1/token?grant_type=refresh_token", { refresh_token: r }, false))
}

const correo = (usuario) => `${usuario.toLowerCase()}@voley-itics.app`

export const haySesionGuardada = () => Boolean(refresco)

export async function recuperarSesion() {
  try {
    await refrescar()
    return true
  } catch {
    cerrarSesion()
    return false
  }
}

export async function crearCuenta(usuario, contrasena) {
  guardarSesion(await pedir("POST", "/auth/v1/signup", { email: correo(usuario), password: contrasena, data: { usuario } }))
}

export async function iniciarSesion(usuario, contrasena) {
  try {
    guardarSesion(await pedir("POST", "/auth/v1/token?grant_type=password", { email: correo(usuario), password: contrasena }))
    return true
  } catch (e) {
    if (e instanceof ErrorServidor && e.codigo === 400) return false
    throw e
  }
}

export function cerrarSesion() {
  token = null
  refresco = null
  try { localStorage.removeItem("refresco") } catch {}
}

const aJugador = (o) => ({ id: o.id, nombre: o.nombre, numero: o.numero, posicion: o.posicion })
const aPartido = (o) => ({ id: o.id, rival: o.rival, fecha: o.fecha, setActual: o.set_actual, terminado: o.terminado })
const aMarcador = (o) => ({ numSet: o.set_num, nosotros: o.nosotros, rival: o.rival })
const aEstadistica = (o) => ({ tipo: o.tipo, total: o.total, puntos: o.puntos, errores: o.errores })

function agrupar(filas, clave, convertir) {
  const g = {}
  for (const f of filas) (g[f[clave]] ??= []).push(convertir(f))
  return g
}

export const jugadores = async () => (await pedir("GET", "/rest/v1/jugador?select=*&order=numero")).map(aJugador)

export const jugador = async (id) => aJugador((await pedir("GET", `/rest/v1/jugador?select=*&id=eq.${id}`))[0])

export const agregarJugador = (nombre, numero, posicion) => pedir("POST", "/rest/v1/jugador", { nombre, numero, posicion })

export const estadisticas = async (jugadorId) =>
  (await pedir("GET", `/rest/v1/estadistica?select=*&jugador_id=eq.${jugadorId}`)).map(aEstadistica)

export const estadisticasPorJugador = async () => agrupar(await pedir("GET", "/rest/v1/estadistica?select=*"), "jugador_id", aEstadistica)

export const estadisticasPartido = async (partidoId) =>
  agrupar(await pedir("GET", `/rest/v1/estadistica_partido?select=*&partido_id=eq.${partidoId}`), "jugador_id", aEstadistica)

export const partidos = async () => (await pedir("GET", "/rest/v1/partido?select=*&order=id.desc")).map(aPartido)

export const partido = async (id) => aPartido((await pedir("GET", `/rest/v1/partido?select=*&id=eq.${id}`))[0])

export const crearPartido = async (rival) => (await pedir("POST", "/rest/v1/partido", { rival }))[0].id

export const siguienteSet = (id) => pedir("POST", "/rest/v1/rpc/siguiente_set", { p_partido: id })

export const cambiarTerminado = (id, terminado) => pedir("PATCH", `/rest/v1/partido?id=eq.${id}`, { terminado })

export const registrar = (partidoId, numSet, jugadorId, tipo, resultado) =>
  pedir("POST", "/rest/v1/jugada", { partido_id: partidoId, set_num: numSet, jugador_id: jugadorId, tipo, resultado })

export const deshacer = (partidoId) => pedir("POST", "/rest/v1/rpc/deshacer", { p_partido: partidoId })

export const marcador = async (partidoId) =>
  (await pedir("GET", `/rest/v1/marcador?select=*&partido_id=eq.${partidoId}&order=set_num`)).map(aMarcador)

export const marcadores = async () => agrupar(await pedir("GET", "/rest/v1/marcador?select=*&order=set_num"), "partido_id", aMarcador)

export const jugadas = async (partidoId) =>
  (await pedir("GET", `/rest/v1/jugada?select=tipo,resultado,jugador(nombre)&partido_id=eq.${partidoId}&order=id.desc&limit=10`))
    .map((o) => ({ jugador: o.jugador?.nombre ?? null, tipo: o.tipo, resultado: o.resultado }))
