import { test } from "node:test"
import assert from "node:assert/strict"
import { eficacia, puntaje, mejorPosicion, setCompleto, partidoDecidido, plural, filasReporte, porToque, textoReporte } from "./rendimiento.js"

const ataque = { tipo: "ATAQUE", total: 10, puntos: 4, errores: 2 }
const defensa = { tipo: "DEFENSA", total: 10, puntos: 0, errores: 1 }

test("eficacia terminal y continua", () => {
  assert.equal(eficacia(ataque), 60)
  assert.equal(eficacia(defensa), 90)
})

test("puntaje ponderado por posicion", () => {
  assert.equal(puntaje("PUNTA", [ataque, defensa]), 68)
  assert.equal(puntaje("LIBERO", [ataque]), null)
  assert.equal(mejorPosicion("PUNTA", [ataque, defensa]), "LIBERO")
  assert.equal(mejorPosicion("LIBERO", [defensa]), "LIBERO")
})

test("set completo a 25 o 15 con ventaja de 2", () => {
  assert.ok(setCompleto({ numSet: 1, nosotros: 25, rival: 23 }))
  assert.ok(!setCompleto({ numSet: 1, nosotros: 25, rival: 24 }))
  assert.ok(setCompleto({ numSet: 1, nosotros: 24, rival: 26 }))
  assert.ok(setCompleto({ numSet: 5, nosotros: 15, rival: 13 }))
  assert.ok(!setCompleto({ numSet: 1, nosotros: 15, rival: 13 }))
})

test("partido decidido con 3 sets", () => {
  const g = { numSet: 1, nosotros: 25, rival: 20 }
  const p = { numSet: 2, nosotros: 20, rival: 25 }
  assert.ok(!partidoDecidido([g, g, p, p]))
  assert.ok(partidoDecidido([g, p, g, g]))
  assert.ok(partidoDecidido([p, p, p]))
})

test("plural en español", () => {
  assert.equal(plural(1, "toque"), "1 toque")
  assert.equal(plural(3, "toque"), "3 toques")
  assert.equal(plural(0, "error"), "0 errores")
  assert.equal(plural(1, "error"), "1 error")
})

test("reporte por partido", () => {
  const ana = { id: 1, nombre: "Ana", numero: 4, posicion: "PUNTA" }
  const luz = { id: 2, nombre: "Luz", numero: 7, posicion: "LIBERO" }
  const stats = { 1: [ataque, defensa], 2: [defensa] }
  const filas = filasReporte([ana, luz, { id: 3, nombre: "Sin datos", numero: 9, posicion: "CENTRAL" }], stats)
  assert.deepEqual(filas.map((f) => f.jugador.nombre), ["Luz", "Ana"])
  assert.equal(filas[1].toques, 20)
  assert.equal(filas[1].errores, 3)
  assert.deepEqual(porToque(stats).map((e) => e.tipo), ["ATAQUE", "DEFENSA"])
  assert.equal(porToque(stats)[1].total, 20)

  const partido = { id: 1, rival: "Tigres", fecha: "2026-11-25", setActual: 3, terminado: true }
  const texto = textoReporte(partido, [{ numSet: 1, nosotros: 25, rival: 20 }, { numSet: 2, nosotros: 20, rival: 25 }, { numSet: 3, nosotros: 15, rival: 10 }], filas)
  assert.ok(texto.includes("Sets: 2 - 1"))
  assert.ok(texto.includes("Puntos a favor: 60. En contra: 55 (4 por errores propios)."))
  assert.ok(texto.includes("#4 Ana: 20 toques, 4 puntos, 3 errores, puntaje 68"))
})
