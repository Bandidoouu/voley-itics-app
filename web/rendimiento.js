export const PESOS = {
  ARMADOR: { ARMADO: 50, DEFENSA: 20, SAQUE: 15, BLOQUEO: 10, ATAQUE: 5 },
  OPUESTO: { ATAQUE: 50, BLOQUEO: 20, SAQUE: 15, DEFENSA: 15 },
  PUNTA: { ATAQUE: 40, RECEPCION: 30, DEFENSA: 15, SAQUE: 10, BLOQUEO: 5 },
  CENTRAL: { BLOQUEO: 40, ATAQUE: 35, SAQUE: 15, DEFENSA: 10 },
  LIBERO: { RECEPCION: 50, DEFENSA: 40, ARMADO: 10 },
}
export const POSICIONES = Object.keys(PESOS)
export const TIPOS = ["SAQUE", "RECEPCION", "ARMADO", "ATAQUE", "BLOQUEO", "DEFENSA"]
export const RESULTADOS = ["PUNTO", "CONTINUA", "ERROR"]
const TERMINALES = new Set(["SAQUE", "ATAQUE", "BLOQUEO"])

const NOMBRES = {
  SAQUE: "Saque", RECEPCION: "Recepción", ARMADO: "Colocación",
  ATAQUE: "Ataque", BLOQUEO: "Bloqueo", DEFENSA: "Defensa",
  PUNTO: "Punto", CONTINUA: "Sigue", ERROR: "Error",
  ARMADOR: "Colocador", OPUESTO: "Opuesto", PUNTA: "Punta",
  CENTRAL: "Central", LIBERO: "Líbero",
}
export const nombre = (codigo) => NOMBRES[codigo] ?? codigo

const suma = (lista, f) => lista.reduce((t, x) => t + f(x), 0)

export function eficacia(e) {
  const continuas = e.total - e.puntos - e.errores
  const valorContinua = TERMINALES.has(e.tipo) ? 0.5 : 1
  return Math.trunc(((e.puntos + continuas * valorContinua) * 100) / e.total)
}

export function puntaje(posicion, stats) {
  const pesos = PESOS[posicion]
  const usadas = stats.filter((e) => e.tipo in pesos)
  if (usadas.length === 0) return null
  return Math.trunc(suma(usadas, (e) => eficacia(e) * pesos[e.tipo]) / suma(usadas, (e) => pesos[e.tipo]))
}

export function mejorPosicion(posicion, stats) {
  let mejor = posicion
  for (const p of [posicion, ...POSICIONES]) {
    if ((puntaje(p, stats) ?? -1) > (puntaje(mejor, stats) ?? -1)) mejor = p
  }
  return mejor
}

export function calificacion(p) {
  if (p >= 80) return "Excelente"
  if (p >= 60) return "Bien"
  if (p >= 40) return "Regular"
  return "Bajo"
}

export const setCompleto = (m) =>
  Math.max(m.nosotros, m.rival) >= (m.numSet >= 5 ? 15 : 25) && Math.abs(m.nosotros - m.rival) >= 2

const ganados = (sets) => sets.filter((m) => m.nosotros > m.rival).length
const perdidos = (sets) => sets.filter((m) => m.rival > m.nosotros).length

export const partidoDecidido = (sets) => ganados(sets) >= 3 || perdidos(sets) >= 3

export const setsCerrados = (p, marcador) => marcador.filter((m) => m.numSet < p.setActual || p.terminado)

export const resultadoSets = (sets) => `${ganados(sets)} - ${perdidos(sets)}`

export const plural = (n, palabra) => `${n} ${palabra}` + (n === 1 ? "" : palabra.endsWith("r") ? "es" : "s")

export function filasReporte(jugadores, stats) {
  return jugadores
    .filter((j) => stats[j.id])
    .map((j) => {
      const s = stats[j.id]
      return {
        jugador: j,
        toques: suma(s, (e) => e.total),
        puntos: suma(s, (e) => e.puntos),
        errores: suma(s, (e) => e.errores),
        puntaje: puntaje(j.posicion, s),
      }
    })
    .sort((a, b) => (b.puntaje ?? -1) - (a.puntaje ?? -1))
}

export function porToque(stats) {
  const grupos = {}
  for (const e of Object.values(stats).flat()) (grupos[e.tipo] ??= []).push(e)
  return Object.entries(grupos)
    .map(([tipo, l]) => ({ tipo, total: suma(l, (e) => e.total), puntos: suma(l, (e) => e.puntos), errores: suma(l, (e) => e.errores) }))
    .sort((a, b) => TIPOS.indexOf(a.tipo) - TIPOS.indexOf(b.tipo))
}

export function textoReporte(p, marcador, filas) {
  const lineas = [
    "Reporte Voley-itics",
    `vs ${p.rival} (${p.fecha})`,
    `Sets: ${resultadoSets(setsCerrados(p, marcador))}`,
    "Parciales: " + marcador.map((m) => `${m.nosotros}-${m.rival}`).join(", "),
    `Puntos a favor: ${suma(marcador, (m) => m.nosotros)}. En contra: ${suma(marcador, (m) => m.rival)} (${suma(filas, (f) => f.errores)} por errores propios).`,
    "",
    "Jugadores:",
    ...filas.map((f) =>
      `#${f.jugador.numero} ${f.jugador.nombre}: ${plural(f.toques, "toque")}, ${plural(f.puntos, "punto")}, ${plural(f.errores, "error")}, puntaje ${f.puntaje ?? "-"}`),
  ]
  return lineas.join("\n").trim()
}
