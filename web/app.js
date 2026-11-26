import * as db from "./db.js"
import {
  POSICIONES, TIPOS, RESULTADOS, nombre, eficacia, puntaje, mejorPosicion, calificacion, setCompleto,
  partidoDecidido, setsCerrados, resultadoSets, plural, filasReporte, porToque, textoReporte,
} from "./rendimiento.js"

const app = document.getElementById("app")
const esc = (t) => String(t).replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c])

const svg = (d) => `<svg viewBox="0 0 24 24" aria-hidden="true"><path d="${d}"/></svg>`
const ICONO = {
  atras: svg("M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"),
  salir: svg("M10.09 15.59L11.5 17l5-5-5-5-1.41 1.41L12.67 11H3v2h9.67l-2.58 2.59zM19 3H5c-1.11 0-2 .9-2 2v4h2V5h14v14H5v-4H3v4c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2z"),
  persona: svg("M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"),
  calendario: svg("M9 11H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2zm2-7h-1V2h-2v2H8V2H6v2H5c-1.11 0-1.99.9-1.99 2L3 20c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 16H5V9h14v11z"),
  compartir: svg("M18 16.08c-.76 0-1.44.3-1.96.77L8.91 12.7c.05-.23.09-.46.09-.7s-.04-.47-.09-.7l7.05-4.11c.54.5 1.25.81 2.04.81 1.66 0 3-1.34 3-3s-1.34-3-3-3-3 1.34-3 3c0 .24.04.47.09.7L8.04 9.81C7.5 9.31 6.79 9 6 9c-1.66 0-3 1.34-3 3s1.34 3 3 3c.79 0 1.5-.31 2.04-.81l7.12 4.16c-.05.21-.08.43-.08.65 0 1.61 1.31 2.92 2.92 2.92 1.61 0 2.92-1.31 2.92-2.92s-1.31-2.92-2.92-2.92z"),
}
const LOGO = `<svg class="logo" viewBox="0 0 100 100" aria-hidden="true">
  <circle cx="50" cy="50" r="42" fill="none" stroke="currentColor" stroke-width="6"/>
  <path d="M31,70 V60 a5,5 0 0 1 10,0 V70 Z M45,70 V48 a5,5 0 0 1 10,0 V70 Z M59,70 V36 a5,5 0 0 1 10,0 V70 Z" fill="currentColor"/>
  <path d="M27,72 H73" stroke="currentColor" stroke-width="4" stroke-linecap="round"/></svg>`

let vista = { nombre: "cargando" }
let carga = 0

function aviso(texto) {
  const a = document.getElementById("aviso")
  a.textContent = texto
  a.classList.add("visible")
  clearTimeout(aviso.t)
  aviso.t = setTimeout(() => a.classList.remove("visible"), 3500)
}

async function intentar(f) {
  try {
    return [true, await f()]
  } catch (e) {
    aviso(e instanceof db.ErrorServidor ? "No se pudo completar. Intenta de nuevo." : "Sin conexión. Revisa tu internet.")
    return [false]
  }
}

const CARGAS = {
  jugadores: () => Promise.all([db.jugadores(), db.estadisticasPorJugador()]),
  jugador: (v) => Promise.all([db.jugador(v.id), db.estadisticas(v.id)]),
  partidos: () => Promise.all([db.partidos(), db.marcadores(), db.jugadores()]),
  partido: (v) => Promise.all([db.partido(v.id), db.marcador(v.id), db.jugadas(v.id), db.jugadores()]),
  reporte: (v) => Promise.all([db.partido(v.id), db.marcador(v.id), db.jugadores(), db.estadisticasPartido(v.id)]),
}

async function ir(nombre, params = {}) {
  vista = { nombre, ...params }
  const id = ++carga
  pintar()
  window.scrollTo(0, 0)
  if (CARGAS[nombre]) {
    const [ok, datos] = await intentar(() => CARGAS[nombre](vista))
    if (id !== carga) return
    if (ok) vista.datos = datos
    else vista.fallo = true
    pintar()
  }
}

async function recargar() {
  const id = ++carga
  const [ok, datos] = await intentar(() => CARGAS[vista.nombre](vista))
  if (ok && id === carga) {
    vista.datos = datos
    pintar()
  }
}

async function enviar(f) {
  await intentar(f)
  await recargar()
}

function pintar() {
  app.innerHTML = PANTALLAS[vista.nombre](vista)
}

const encabezado = (titulo, atras, acciones = "") => `<header class="encabezado">
  ${atras ? `<button class="icono" data-accion="atras" aria-label="Atrás">${ICONO.atras}</button>` : ""}
  <h1>${esc(titulo)}</h1>${acciones}</header>`

const botonSalir = `<button class="icono" data-accion="salir" aria-label="Salir">${ICONO.salir}</button>`

const navegacion = (activa) => `<nav class="navegacion">
  <button data-accion="ir" data-vista="jugadores" class="${activa === "jugadores" ? "activa" : ""}">${ICONO.persona}<span>Jugadores</span></button>
  <button data-accion="ir" data-vista="partidos" class="${activa === "partidos" ? "activa" : ""}">${ICONO.calendario}<span>Partidos</span></button></nav>`

const cargando = (v) => v.fallo
  ? `<div class="vacio">No se pudieron cargar los datos.<br><button class="boton contorno" data-accion="reintentar">Reintentar</button></div>`
  : `<div class="cargando"><span class="giro"></span></div>`

const vacio = (texto) => `<p class="vacio">${texto}</p>`
const etiqueta = (texto) => `<span class="etiqueta">${esc(texto)}</span>`
const numero = (n) => `<span class="numero">${n}</span>`
const seccion = (texto) => `<h3 class="seccion">${texto}</h3>`
const colorPuntaje = (p) => (p == null ? "gris" : p >= 60 ? "verde" : p >= 40 ? "ambar" : "rojo")
const conteo = (e) => `${plural(e.total ?? e.toques, "toque")}, ${plural(e.puntos, "punto")}, ${plural(e.errores, "error")}`

const tarjetaEficacia = (e) => `<div class="tarjeta">
  <div class="fila-titulo"><b>${nombre(e.tipo)}</b><span class="porcentaje">${eficacia(e)}%</span></div>
  <div class="barra"><span style="width:${eficacia(e)}%"></span></div>
  <small>${conteo(e)}</small></div>`

const marcadorTarjeta = (actual, numSet, cerrados) => `<div class="tarjeta azul marcador">
  <div class="tablero">
    <div><small>NOSOTROS</small><b>${actual.nosotros}</b></div>
    <span class="pastilla">SET ${numSet}</span>
    <div><small>RIVAL</small><b>${actual.rival}</b></div></div>
  <p>Sets ganados ${resultadoSets(cerrados)}&nbsp;&nbsp; ${cerrados.map((m) => `${m.nosotros}-${m.rival}`).join("&nbsp; ")}</p></div>`

function opciones(lista) {
  return `<div class="opciones">${lista.map(([accion, valor, texto]) =>
    `<button class="boton tonal" data-accion="${accion}" data-valor="${esc(valor)}">${esc(texto)}</button>`).join("")}</div>`
}

const PANTALLAS = {
  cargando: () => cargando({}),

  login: () => `<main class="portada">
    ${LOGO}<h1 class="marca">Voley-itics</h1><p class="sub">Estadísticas para tu equipo</p>
    <form class="tarjeta formulario" data-form="login">
      <input name="usuario" placeholder="Usuario" autocomplete="username" autocapitalize="none" autocorrect="off" spellcheck="false" required>
      <input name="contrasena" type="password" placeholder="Contraseña" autocomplete="current-password" required>
      <p class="error" id="error"></p>
      <button class="boton primario">Entrar</button>
      <button type="button" class="boton contorno" data-accion="ir" data-vista="registro">Crear cuenta nueva</button>
    </form></main>`,

  registro: () => `${encabezado("Crear cuenta", true)}<main class="contenido">
    <p class="gris">Crea tu cuenta de entrenador para guardar a tu equipo y tus partidos.</p>
    <form class="formulario" data-form="registro">
      <input name="usuario" placeholder="Usuario" autocomplete="username" autocapitalize="none" autocorrect="off" spellcheck="false">
      <input name="contrasena" type="password" placeholder="Contraseña (mínimo 8)" autocomplete="new-password">
      <input name="confirmacion" type="password" placeholder="Confirmar contraseña" autocomplete="new-password">
      <p class="error" id="error"></p>
      <button class="boton primario">Crear cuenta</button>
    </form></main>`,

  jugadores(v) {
    let cuerpo = cargando(v)
    if (v.datos) {
      const [js, st] = v.datos
      const puntajes = Object.fromEntries(js.map((j) => [j.id, puntaje(j.posicion, st[j.id] ?? [])]))
      const conDatos = Object.values(puntajes).filter((p) => p != null)
      const promedio = conDatos.length ? Math.trunc(conDatos.reduce((a, b) => a + b, 0) / conDatos.length) : null
      cuerpo = js.length === 0 ? vacio("Aún no hay jugadores.") : `
        <div class="tarjeta azul equipo"><small>PUNTAJE DEL EQUIPO</small><b>${promedio ?? "-"}</b>
          <p>${promedio == null ? "Registra jugadas en un partido para ver puntajes" : `${calificacion(promedio)}, promedio de ${conDatos.length} jugadores con datos`}</p></div>
        ${[...js].sort((a, b) => (puntajes[b.id] ?? -1) - (puntajes[a.id] ?? -1)).map((j) => {
          const p = puntajes[j.id]
          return `<button class="tarjeta fila tocable" data-accion="ir" data-vista="jugador" data-id="${j.id}">
            ${numero(j.numero)}<span class="crece"><b>${esc(j.nombre)}</b>${etiqueta(nombre(j.posicion))}</span>
            <span class="derecha"><b class="grande ${colorPuntaje(p)}">${p ?? "-"}</b><small>${p == null ? "sin datos" : calificacion(p)}</small></span></button>`
        }).join("")}`
    }
    return `${encabezado("Jugadores", false, botonSalir)}<main class="contenido con-nav">
      <button class="boton primario" data-accion="abrir" data-dialogo="dlg-jugador">Agregar jugador</button>${cuerpo}</main>
      ${navegacion("jugadores")}
      <dialog id="dlg-jugador"><form data-form="nuevo-jugador" class="formulario">
        <h2>Nuevo jugador</h2>
        <input name="nombre" placeholder="Nombre" required>
        <input name="numero" placeholder="Número" inputmode="numeric" pattern="[0-9]{1,2}" required>
        <div class="radios">${POSICIONES.map((p, i) =>
          `<label><input type="radio" name="posicion" value="${p}" ${i === 0 ? "checked" : ""}> ${nombre(p)}</label>`).join("")}</div>
        <p class="error" id="error-jugador"></p>
        <div class="acciones-dialogo"><button type="button" class="boton texto" data-accion="cerrar">Cancelar</button>
          <button class="boton texto fuerte">Guardar</button></div></form></dialog>`
  },

  jugador(v) {
    if (!v.datos) return `${encabezado("Jugador", true)}<main class="contenido">${cargando(v)}</main>`
    const [j, stats] = v.datos
    const p = puntaje(j.posicion, stats)
    const toques = stats.reduce((t, e) => t + e.total, 0)
    const anillo = p == null ? `<p class="gris">Sin jugadas registradas</p>` : `
      <div class="anillo ${colorPuntaje(p)}"><svg viewBox="0 0 36 36"><circle class="pista" cx="18" cy="18" r="15.9"/>
        <circle class="avance" cx="18" cy="18" r="15.9" stroke-dasharray="${p} 100"/></svg>
        <div><b>${p}</b><small>de 100</small></div></div>
      <b class="calificacion">${calificacion(p)}</b>
      <p class="gris">${toques >= 10 ? `Mejor posición según datos: ${nombre(mejorPosicion(j.posicion, stats))}` : `Registra al menos 10 toques para sugerir posición (${toques})`}</p>`
    return `${encabezado(j.nombre, true)}<main class="contenido">
      <div class="tarjeta centrado"><div class="fila-chica"><span class="gris">#${j.numero}</span>${etiqueta(nombre(j.posicion))}</div>${anillo}</div>
      ${stats.length ? seccion("EFICACIA POR TOQUE") : ""}${stats.map(tarjetaEficacia).join("")}</main>`
  },

  partidos(v) {
    let cuerpo = cargando(v)
    let hayJugadores = false
    if (v.datos) {
      const [ps, ms, js] = v.datos
      hayJugadores = js.length > 0
      cuerpo = !hayJugadores ? vacio("Primero agrega jugadores") : ps.length === 0 ? vacio("Aún no hay partidos.") : ps.map((p) =>
        `<button class="tarjeta fila tocable" data-accion="ir" data-vista="partido" data-id="${p.id}">
          <span class="crece"><b>vs ${esc(p.rival)}</b><small class="gris">${p.fecha}</small>${etiqueta(p.terminado ? "Terminado" : "En juego")}</span>
          <b class="azul-texto grande-medio">Sets ${resultadoSets(setsCerrados(p, ms[p.id] ?? []))}</b></button>`).join("")
    }
    return `${encabezado("Partidos", false, botonSalir)}<main class="contenido con-nav">
      <button class="boton primario" data-accion="abrir" data-dialogo="dlg-partido" ${hayJugadores ? "" : "disabled"}>Nuevo partido</button>${cuerpo}</main>
      ${navegacion("partidos")}
      <dialog id="dlg-partido"><form data-form="nuevo-partido" class="formulario">
        <h2>Nuevo partido</h2><input name="rival" placeholder="Rival" required>
        <div class="acciones-dialogo"><button type="button" class="boton texto" data-accion="cerrar">Cancelar</button>
          <button class="boton texto fuerte">Crear</button></div></form></dialog>`
  },

  partido(v) {
    if (!v.datos) return `${encabezado("Partido", true)}<main class="contenido">${cargando(v)}</main>`
    const [p, marcador, jugadas, jugadores] = v.datos
    const setActual = marcador.find((m) => m.numSet === p.setActual) ?? { numSet: p.setActual, nosotros: 0, rival: 0 }
    const cerrados = setsCerrados(p, marcador)
    const j = jugadores.find((x) => x.id === v.jugadorSel)
    const t = v.tipoSel
    let cuerpo
    if (p.terminado) {
      cuerpo = `<p class="gris">Partido terminado. Puedes reabrirlo para seguir registrando.</p>
        <button class="boton primario" data-accion="reporte">Ver reporte del partido</button>
        <button class="boton contorno" data-accion="terminar" data-valor="false">Reabrir partido</button>`
    } else {
      const banner = !setCompleto(setActual) ? "" : `<div class="tarjeta clara">
        <b>Set ${p.setActual} completo: ${setActual.nosotros}-${setActual.rival}</b>
        ${partidoDecidido([...cerrados, setActual])
          ? `<button class="boton primario" data-accion="terminar" data-valor="true">Terminar partido</button>`
          : `<button class="boton primario" data-accion="abrir" data-dialogo="dlg-set">Cerrar set</button>`}</div>`
      let paso
      if (!j) {
        paso = `<h2 class="paso">¿Quién tocó el balón?</h2>
          ${jugadores.length ? "" : `<p class="gris">Primero agrega jugadores en la pestaña Jugadores</p>`}
          ${opciones(jugadores.map((x) => ["jugador", x.id, `#${x.numero} ${x.nombre}`]))}`
      } else if (!t) {
        paso = `<h2 class="paso">${esc(j.nombre)}: ¿qué hizo?</h2>${opciones(TIPOS.map((x) => ["tipo", x, nombre(x)]))}
          <button class="boton texto" data-accion="volver">Volver</button>`
      } else {
        const ayuda = { PUNTO: "anotamos", CONTINUA: "continúa la jugada", ERROR: "anota el rival" }
        paso = `<h2 class="paso">${esc(j.nombre)} (${nombre(t)}): ¿cómo terminó?</h2>
          ${RESULTADOS.map((r) => `<button class="boton resultado ${r.toLowerCase()}" data-accion="resultado" data-valor="${r}">
            <b>${nombre(r)}</b><small>${ayuda[r]}</small></button>`).join("")}
          <button class="boton texto" data-accion="volver">Volver</button>`
      }
      cuerpo = `${banner}${paso}<hr>
        <button class="boton contorno" data-accion="rival">Punto del rival</button>
        <div class="dos"><button class="boton contorno" data-accion="deshacer" ${marcador.some((m) => m.numSet === p.setActual) ? "" : "disabled"}>Deshacer</button>
          <button class="boton contorno" data-accion="abrir" data-dialogo="dlg-mas">Más</button></div>`
    }
    return `${encabezado(`vs ${p.rival}`, true)}<main class="contenido">
      ${marcadorTarjeta(setActual, p.setActual, cerrados)}${cuerpo}
      ${seccion("ÚLTIMAS JUGADAS")}
      ${jugadas.length === 0 ? `<p class="gris">Sin jugadas todavía</p>` : `<div class="tarjeta lista">${jugadas.map((x) =>
        `<p>${x.jugador == null ? "Punto del rival" : `${esc(x.jugador)}: ${nombre(x.tipo)}, ${nombre(x.resultado)}`}</p>`).join("")}</div>`}</main>
      <dialog id="dlg-mas" class="hoja"><div class="formulario">
        <button class="boton texto" data-accion="reporte">Ver reporte</button>
        <button class="boton texto" data-accion="abrir" data-dialogo="dlg-set">Cerrar set ${p.setActual}</button>
        <button class="boton texto" data-accion="terminar" data-valor="true">Terminar partido</button>
        <button class="boton texto gris" data-accion="cerrar">Cancelar</button></div></dialog>
      <dialog id="dlg-set"><div class="formulario"><h2>¿Cerrar el set ${p.setActual}?</h2>
        <p>Queda ${setActual.nosotros} - ${setActual.rival} y se abre el set ${p.setActual + 1}. Esto no se puede deshacer.</p>
        <div class="acciones-dialogo"><button class="boton texto" data-accion="cerrar">Cancelar</button>
          <button class="boton texto fuerte" data-accion="cerrar-set">Cerrar set</button></div></div></dialog>`
  },

  reporte(v) {
    if (!v.datos) return `${encabezado("Reporte", true)}<main class="contenido">${cargando(v)}</main>`
    const [p, marcador, jugadores, stats] = v.datos
    const filas = filasReporte(jugadores, stats)
    const cerrados = setsCerrados(p, marcador)
    const aFavor = marcador.reduce((t, m) => t + m.nosotros, 0)
    const enContra = marcador.reduce((t, m) => t + m.rival, 0)
    const errores = filas.reduce((t, f) => t + f.errores, 0)
    const toques = porToque(stats)
    const dato = (valor, titulo) => `<div class="tarjeta dato"><b>${valor}</b><small>${titulo}</small></div>`
    return `${encabezado(`Reporte vs ${p.rival}`, true, `<button class="icono" data-accion="compartir" aria-label="Compartir">${ICONO.compartir}</button>`)}
      <main class="contenido">
      <div class="tarjeta azul centrado resultado-final"><small>${p.terminado ? "RESULTADO FINAL" : "PARTIDO EN JUEGO"}</small>
        <b>${resultadoSets(cerrados)}</b><p>${marcador.map((m) => `${m.nosotros}-${m.rival}`).join("&nbsp;&nbsp; ") || "Sin puntos todavía"}</p></div>
      <div class="tres">${dato(aFavor, "A favor")}${dato(enContra, "En contra")}${dato(errores, "Errores propios")}</div>
      ${seccion("POR JUGADOR")}
      ${filas.length === 0 ? `<p class="gris">Sin jugadas registradas</p>` : filas.map((f) => `<div class="tarjeta fila">
        ${numero(f.jugador.numero)}<span class="crece"><b>${esc(f.jugador.nombre)}</b><small class="gris">${conteo(f)}</small></span>
        <b class="grande-medio ${colorPuntaje(f.puntaje)}">${f.puntaje ?? "-"}</b></div>`).join("")}
      ${toques.length ? seccion("POR TIPO DE TOQUE") : ""}${toques.map(tarjetaEficacia).join("")}</main>`
  },
}

const ATRAS = { registro: () => ir("login"), jugador: () => ir("jugadores"), partido: () => ir("partidos"), reporte: () => ir("partido", { id: vista.id }) }

const ACCIONES = {
  ir: (d) => ir(d.vista, d.id ? { id: Number(d.id) } : {}),
  atras: () => ATRAS[vista.nombre](),
  reintentar: () => ir(vista.nombre, { id: vista.id }),
  salir: () => { db.cerrarSesion(); ir("login") },
  abrir: (d) => { app.querySelector("dialog[open]")?.close(); document.getElementById(d.dialogo).showModal() },
  cerrar: (d, b) => b.closest("dialog").close(),
  jugador: (d) => { vista.jugadorSel = Number(d.valor); pintar() },
  tipo: (d) => { vista.tipoSel = d.valor; pintar() },
  volver: () => { if (vista.tipoSel) vista.tipoSel = null; else vista.jugadorSel = null; pintar() },
  resultado: (d) => {
    const { jugadorSel, tipoSel } = vista
    const numSet = vista.datos[0].setActual
    vista.jugadorSel = vista.tipoSel = null
    pintar()
    enviar(() => db.registrar(vista.id, numSet, jugadorSel, tipoSel, d.valor))
  },
  rival: () => enviar(() => db.registrar(vista.id, vista.datos[0].setActual, null, "RIVAL", "ERROR")),
  deshacer: () => enviar(() => db.deshacer(vista.id)),
  "cerrar-set": (d, b) => { b.closest("dialog").close(); enviar(() => db.siguienteSet(vista.id)) },
  terminar: (d, b) => { b.closest("dialog")?.close(); enviar(() => db.cambiarTerminado(vista.id, d.valor === "true")) },
  reporte: () => ir("reporte", { id: vista.id }),
  async compartir() {
    const [p, marcador, jugadores, stats] = vista.datos
    const texto = textoReporte(p, marcador, filasReporte(jugadores, stats))
    if (navigator.share) {
      try { await navigator.share({ title: "Reporte Voley-itics", text: texto }) } catch {}
    } else {
      await navigator.clipboard.writeText(texto)
      aviso("Reporte copiado")
    }
  },
}

function ocupado(form, texto) {
  const b = form.querySelector("button:not([type=button])")
  b.disabled = Boolean(texto)
  b.dataset.texto ??= b.textContent
  b.textContent = texto || b.dataset.texto
}

const FORMULARIOS = {
  async login(f, form) {
    ocupado(form, "Entrando...")
    const [ok, entro] = await intentar(() => db.iniciarSesion(f.get("usuario").trim(), f.get("contrasena")))
    ocupado(form, null)
    if (ok && entro) ir("jugadores")
    else if (ok) form.querySelector("#error").textContent = "Usuario o contraseña incorrectos"
  },
  async registro(f, form) {
    const usuario = f.get("usuario").trim()
    const contrasena = f.get("contrasena")
    const error = !usuario ? "Escribe un usuario"
      : !/^[A-Za-z0-9._-]+$/.test(usuario) ? "El usuario solo puede tener letras sin acentos, números, punto o guion"
      : contrasena.length < 8 ? "La contraseña necesita al menos 8 caracteres"
      : contrasena !== f.get("confirmacion") ? "Las contraseñas no coinciden" : ""
    form.querySelector("#error").textContent = error
    if (error) return
    ocupado(form, "Creando...")
    try {
      await db.crearCuenta(usuario, contrasena)
      ir("jugadores")
    } catch (e) {
      ocupado(form, null)
      form.querySelector("#error").textContent = e instanceof db.ErrorServidor && e.respuesta.includes("already")
        ? "Ese usuario ya existe" : "No se pudo crear la cuenta. Revisa tu internet."
    }
  },
  async "nuevo-jugador"(f, form) {
    ocupado(form, "Guardando...")
    const [ok] = await intentar(() => db.agregarJugador(f.get("nombre").trim(), Number(f.get("numero")), f.get("posicion")))
    ocupado(form, null)
    if (!ok) return void (form.querySelector("#error-jugador").textContent = "Número repetido o fuera de 0 a 99")
    form.closest("dialog").close()
    recargar()
  },
  async "nuevo-partido"(f, form) {
    ocupado(form, "Creando...")
    const [ok, id] = await intentar(() => db.crearPartido(f.get("rival").trim()))
    ocupado(form, null)
    if (ok) ir("partido", { id })
  },
}

app.addEventListener("click", (ev) => {
  const b = ev.target.closest("[data-accion]")
  if (b && !b.disabled) ACCIONES[b.dataset.accion](b.dataset, b)
})

app.addEventListener("submit", (ev) => {
  ev.preventDefault()
  FORMULARIOS[ev.target.dataset.form](new FormData(ev.target), ev.target)
})

if (db.haySesionGuardada()) {
  pintar()
  db.recuperarSesion().then((ok) => ir(ok ? "jugadores" : "login"))
} else {
  ir("login")
}
