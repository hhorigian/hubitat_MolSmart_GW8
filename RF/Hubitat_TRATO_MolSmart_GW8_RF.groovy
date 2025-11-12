/**
 *  MolSmart GW8 Driver - RF - Cortinas, Luzes RF, Controle tudo via RF. 
 *  (Versão estendida com CHILD BUTTONS momentâneos: Subir / Parar / Descer)
 *
 *  Copyright 2025 VH 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable lawkkk or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 * 
 *  No primeiro uso, rode open até 100% e cronometre; preencha openTimeMs.
 *  Rode close até 0% e cronometre; preencha closeTimeMs.
 *  Ajuste tickMs (400 ms é bom) e tolerance (3–5%).
 *  Use o slider — o driver envia subir/descer, espera o tempo calculado e manda Parar sozinho.
 *
 *
 *   +++  Versões ++++
 *        1.0 - 11/11/2025 - V1
 */

import groovy.transform.Field

@Field static final List<String> ONLINE_ENUM = ["online","offline","unknown"]

metadata {
  definition (name: "MolSmart - GW8 - RF", namespace: "TRATO", author: "VH", vid: "generic-contact") {
    capability "Sensor"
    capability "Actuator"

    // Mantidos do driver original (não retirar)
    capability "Contact Sensor"
    capability "PushableButton"
    capability "WindowBlind"

    // Adicionados para o slider de cortina (mantidos do arquivo enviado)
    capability "Window Shade"              // open, close, pause/stop, setPosition, start/stopPositionChange

    // Comandos legados (mantidos)
    command "Up"
    command "Down"
    command "Stop"

    // NOVOS comandos (sob demanda)
    command "healthCheckNow"

    // NOVO: recriar child buttons
    command "recreateButtons"

    // Atributos auxiliares (mantidos)
    attribute "currentstatus", "string"
    attribute "status", "string"
    attribute "position", "NUMBER"
    attribute "moving", "ENUM", ["up","down","stopped"]

    // NOVOS atributos de saúde/conectividade
    attribute "gw3Online", "ENUM", ONLINE_ENUM
    attribute "lastHealthAt", "STRING"
    attribute "healthLatencyMs", "NUMBER"

    // NOVO: versão do GW3 (6 caracteres após "Version: ")
    attribute "gw3Version", "STRING"
  }

  preferences {
    // === Gateway (mantidos exatamente como no seu driver) ===
    input name: "molIPAddress", type: "text",   title: "MolSmart IP",                  required: true, defaultValue: "192.168.1.100"
	input name: "user", title:"Usuário", type: "string", required: true, defaultValue: "admin" 
    input name: "password", title:"Senha", type: "string", required: true, defaultValue: "12345678" 
	input name: "cId", title:"Control ID (pego no WebAdmin)", type: "string", required: true        
    input name: "logEnable",    type: "bool",   title: "Enable debug logging",         defaultValue: false

    // === Temporização/estimativa para slider (mantidos) ===
    input name: "openTimeMs",   type: "number", title: "Tempo para ABRIR 0→100 (ms)",  defaultValue: 12000, required: true
    input name: "closeTimeMs",  type: "number", title: "Tempo para FECHAR 100→0 (ms)", defaultValue: 12000, required: true
    input name: "settleMs",     type: "number", title: "Tempo extra após PARAR (ms)",  defaultValue: 150,   required: true
    input name: "tickMs",       type: "number", title: "Intervalo de atualização (ms)",defaultValue: 400,   required: true
    input name: "tolerance",    type: "number", title: "Tolerância de posição (%)",    defaultValue: 3,     required: true
    input name: "invertOpenClose", type: "bool", title: "Inverter sentido (Open/Close)", defaultValue: false

    // === NOVO: Health Check ===
    input name: "enableHealthCheck", type: "bool",   title: "Ativar verificação de online (HTTP /info)", defaultValue: true
    input name: "healthCheckMins",   type: "number", title: "Intervalo do health check (min)", defaultValue: 30, range: "1..1440"

    // === NOVO: Child Buttons ===
    input name: "createButtonsOnSave", type: "bool", title: "Criar/atualizar Child Buttons ao salvar", defaultValue: true
  }
}

/* ======================= Setup / Estado ======================= */
def installed() {
  sendEvent(name:"numberOfButtons", value:4)
  sendEvent(name:"status", value:"stop")
  state.rcId = 51
  // Atributos novos default
  sendEvent(name:"gw3Online", value:"unknown")
  initialize()
}

def updated() {
  sendEvent(name:"numberOfButtons", value:4)
  state.rcId = 51
  // Garante atributo
  if (!device.currentValue("gw3Online")) sendEvent(name:"gw3Online", value:"unknown")
  initialize()
  if (logEnable) runIn(1800, logsOff)
}

private initialize() {
  unschedule()
  state.currentip   = settings.molIPAddress
  state.username = settings.user
  state.pwd = settings.password
  state.cId         = settings.cId
  state.rcId        = 51
  state.lastKnownPos = (state.lastKnownPos == null) ? 0 : clamp(state.lastKnownPos as int, 0, 100)
  state.targetPos    = state.targetPos ?: state.lastKnownPos
  state.moving       = "stopped"
  sendEvent(name:"position", value: state.lastKnownPos as int, isStateChange:true)
  sendShadeEventForPos(state.lastKnownPos as int)
  sendEvent(name:"moving", value:"stopped", isStateChange:true)
  if (logEnable) log.debug "Init -> ip=${state.currentip} sn=${state.serialNum} cId=${state.cId} pos=${state.lastKnownPos}"

  // Criar/atualizar child buttons
  if (createButtonsOnSave) createOrUpdateChildButtons(true)

  // Agenda health check se habilitado
  if (enableHealthCheck) scheduleHealth()
}

/* ======================= Comandos Legados (mantidos) ======================= */
def Up()   { EnviaComando(1); trackStart("up") }

def Down() { EnviaComando(3); trackStart("down") }

def Stop() { EnviaComando(2); finalizeStop(estimateNow()) }

/* ======================= Capabilities de Shade (mantidos) ======================= */

def open()                { moveTo(100) }

def close()               { moveTo(0) }

def pause()               { stopPositionChange() }

def stopPositionChange()  {
  if (state.moving in ["up","down"]) {
    EnviaComando(2)
    runIn(calcSec((settleMs ?: 150) as int), "onManualStopSettle")
  } else {
    sendEvent(name:"moving", value:"stopped")
    sendShadeEventForPos(state.lastKnownPos as int ?: 0)
  }
}

def startPositionChange(direction) {
  def dir = (direction in ["open","opening","up"]) ? "up" : "down"
  EnviaComando(dir == "up" ? 1 : 3)
  trackStart(dir)
}

def setPosition(Number pos) { moveTo(clamp((pos as int), 0, 100)) }

/* ======================= Lógica de Tempo/Slider (mantidos) ======================= */
private moveTo(Integer target) {
  Integer current = estimateNow()
  Integer tol = (tolerance ?: 3) as int
  if (Math.abs(target - current) <= tol) {
    if (logEnable) log.debug "Dentro da tolerância (current=${current}, target=${target})"
    finalizeStop(target)
    return
  }
  String dir = (target > current) ? "up" : "down"
  Integer totalMs = (dir == "up" ? (openTimeMs ?: 12000) : (closeTimeMs ?: 12000)) as int
  BigDecimal fraction = (Math.abs(target - current) / 100.0)
  Integer runMs = Math.max(50, (int)Math.round(totalMs * fraction))
  if (logEnable) log.debug "moveTo: current=${current}, target=${target}, dir=${dir}, runMs=${runMs}"
  sendPositionNow(current) // evita salto visual
  EnviaComando(dir == "up" ? 1 : 3)
  trackStart(dir)
  state.targetPos = target
  state.pendingStop = true
  runIn(calcSec(runMs), "onMoveTimeout")
}

private trackStart(String dir) {
  state.moving = dir
  state.moveStartEpoch = now()
  state.moveStartPos   = estimateNow()
  sendEvent(name:"moving", value: dir, isStateChange:true)
  sendEvent(name:"windowShade", value: (dir == "up" ? "opening" : "closing"), isStateChange:true)
  sendPositionNow(state.moveStartPos as int)
  scheduleTick()
}

private tick() {
  def est = estimateNow()
  sendPosition(est)
  scheduleTick()
}

private scheduleTick() {
  unschedule("tick")
  Integer t = Math.max(200, (tickMs ?: 400) as int)
  runIn(calcSec(t), "tick")
}

private Integer estimateNow() {
  if (!(state.moving in ["up","down"])) return (state.lastKnownPos ?: 0) as int
  if (!state.moveStartEpoch || state.moveStartPos == null) return (state.lastKnownPos ?: 0) as int
  Long elapsed = now() - (state.moveStartEpoch as Long)
  Integer totalMs = (state.moving == "up" ? (openTimeMs ?: 12000) : (closeTimeMs ?: 12000)) as int
  if (totalMs <= 0) return (state.lastKnownPos ?: 0) as int
  BigDecimal deltaPct = (elapsed / (totalMs as BigDecimal)) * 100.0
  Integer est = (state.moving == "up")
      ? Math.min(100, Math.round((state.moveStartPos as int) + deltaPct) as int)
      : Math.max(0,   Math.round((state.moveStartPos as int) - deltaPct) as int)
  if (state.targetPos != null) {
    Integer tgt = state.targetPos as int
    if (state.moving == "up")   est = Math.min(est, tgt)
    if (state.moving == "down") est = Math.max(est, tgt)
  }
  return est as int
}

private sendPosition(Integer pos) {
  pos = clamp(pos as int, 0, 100)
  if (pos != (state.lastKnownPos ?: -1)) {
    state.lastKnownPos = pos
    sendEvent(name:"position", value: pos)
    sendShadeEventForPos(pos)
    if (logEnable) log.debug "tick -> pos=${pos}"
  }
}

private sendPositionNow(Integer pos) {
  pos = clamp(pos as int, 0, 100)
  state.lastKnownPos = pos
  sendEvent(name:"position", value: pos, isStateChange:true)
  sendShadeEventForPos(pos)
  if (logEnable) log.debug "sendPositionNow -> pos=${pos}"
}

private finalizeStop(Integer finalPos) {
  unschedule("tick")
  Integer pos = clamp(finalPos as int, 0, 100)
  state.lastKnownPos = pos
  state.moving = "stopped"
  sendEvent(name:"position", value: pos, isStateChange:true)
  sendEvent(name:"moving", value:"stopped", isStateChange:true)
  sendShadeEventForPos(pos)
  if (logEnable) log.debug "finalizeStop -> pos=${pos}"
}

private sendShadeEventForPos(Integer pos) {
  String shade = (pos <= 0) ? "closed" : (pos >= 100 ? "open" : "partially open")
  sendEvent(name:"windowShade", value: shade, isStateChange:true)
}

/* ======================= Envio HTTP — MANTIDO ======================= */
private String buildFullUrl(button) {
  def ip   = settings.molIPAddress
  def sn   = settings.user
  def vc   = settings.password
  def cid  = settings.cId
  def rcid = (settings.rcId ?: "51")

  return "http://${ip}/control" + "?cId=${cid}&pwd=${vc}&rcId=${rcid}&state=${button}&user=${sn}"	

    
}

def push(number) {
    sendEvent(name:"pushed", value:number, isStateChange: true)
    log.info "Enviado o botão " + number  
    EnviaComando(number)
}


def EnviaComando(button) {
  settings.timeoutSec = 7
  String fullUrl = buildFullUrl(button)
  if (logEnable) log.info "FullURL = ${fullUrl}"
  Map params = [ uri: fullUrl, timeout: (settings.timeoutSec ?: 7) as int ]
  try {
    asynchttpPost('gw3PostCallback', params, [cmd: button])
    String tempStatus = (button == 1) ? "up" : (button == 2 ? "stop" : (button == 3 ? "down" : "paused"))
    sendEvent(name: "status", value: tempStatus)
    sendEvent(name: "currentstatus", value: tempStatus)
  } catch (e) {
    log.warn "${device.displayName} Async POST scheduling failed: ${e.message}"
  }
}

void gw3PostCallback(resp, data) {
  String cmd = "${data?.cmd}"
  try {
    if (resp?.status in 200..299) {
      if (logEnable) log.debug "POST OK (async) cmd=${cmd} status=${resp?.status}"
      state.ultimamensagem = "Resposta OK"
    } else {
      log.warn "POST error (async) status=${resp?.status} cmd=${cmd}"
      state.ultimamensagem = "Erro no envio do comando"
    }
  } catch (e) {
    log.warn "Async callback exception: ${e.message} (cmd=${cmd})"
    state.errormessage = e.message
  }
}

/* ======= Callbacks para agendamento em segundos (compat sem runInMillis) ======= */

def onMoveTimeout() {
  EnviaComando(2)
  runIn(calcSec((settleMs ?: 150) as int), "onSettleTimeout")
}


def onSettleTimeout() {
  Integer tgt = (state.targetPos != null) ? (state.targetPos as Integer) : estimateNow()
  finalizeStop(tgt)
}


def onManualStopSettle() {
  finalizeStop(estimateNow())
}

private Integer calcSec(Integer ms) {
  Integer s = Math.round((ms ?: 0) / 1000.0) as Integer
  return Math.max(1, s)
}

/* ======================= HEALTH CHECK HTTP (/info) ======================= */

private void scheduleHealth() {
  Integer mins = Math.max(1, (healthCheckMins ?: 5) as int)
  unschedule("healthPoll")
  // Primeiro dispara agora, depois agenda em minutos
  runIn(2, "healthPoll")
  runEveryXMinutes(mins, "healthPoll")
}

private void runEveryXMinutes(Integer mins, String handler) {
  // Helper para intervalos arbitrários (Hubitat tem runEvery5/10/30, aqui simulamos)
  // Reagenda com runIn a cada ciclo
  state.healthEveryMins = mins
  runIn( mins * 60, "healthReschedule" )
}

def healthReschedule() {
  Integer mins = (state.healthEveryMins ?: (healthCheckMins ?: 5)) as int
  runIn( mins * 60, "healthReschedule" )
  healthPoll()
}


def healthPoll() {
  if (!enableHealthCheck) return
  String ip = (settings.molIPAddress ?: "").trim()
  if (!ip) return
  String uri = "http://${ip}/info"
  Long started = now()
  Map params = [ uri: uri, timeout: 5 ]
  try {
    asynchttpGet('healthPollCB', params, [t0: started, uri: uri])
  } catch (e) {
    if (logEnable) log.warn "healthPoll schedule failed: ${e.message}"
  }
}

void healthPollCB(resp, data) {
  String body = ""
  Integer st = null
  try {
    st = resp?.status as Integer
    body = resp?.getData() ?: ""
  } catch (ignored) { }
  String stamp = new Date().format("yyyy-MM-dd HH:mm:ss")
  Long t0 = (data?.t0 ?: now())
  Long dt = (now() - t0)

  if (st && st >= 200 && st <= 299 && body?.toString()?.contains("MolSmart Device Info")) {
    // Online
    if (device.currentValue("gw3Online") != "online") sendEvent(name:"gw3Online", value:"online", isStateChange:true)
    sendEvent(name:"healthLatencyMs", value: dt as Long)
    sendEvent(name:"lastHealthAt", value: stamp)

    // === NOVO: extrair "Version: X" e publicar 6 chars em gw3Version ===
    try {
      String txt = body?.toString() ?: ""
      // procura linha iniciando com "Version:"
      def m = (txt =~ /(?im)^\s*Version:\s*([^\r\n]+)/)
      if (m.find()) {
        String verFull = (m.group(1) ?: "").trim()
        String ver6 = (verFull.length() >= 6) ? verFull.substring(0, 6) : verFull
        if (ver6) {
          sendEvent(name:"gw3Version", value: ver6, isStateChange:true)
          if (logEnable) log.debug "Versão detectada: '${verFull}' -> gw3Version='${ver6}'"
        }
      } else if (logEnable) {
        log.debug "Versão não encontrada no corpo do /info."
      }
    } catch (e) {
      if (logEnable) log.warn "Falha ao extrair versão: ${e.message}"
    }

    if (logEnable) log.debug "Health OK in ${dt} ms"
  } else {
    // Offline
    if (device.currentValue("gw3Online") != "offline") sendEvent(name:"gw3Online", value:"offline", isStateChange:true)
    sendEvent(name:"healthLatencyMs", value: null)
    sendEvent(name:"lastHealthAt", value: stamp)
    if (logEnable) log.warn "Health FAIL (status=${st})"
  }
}


def healthCheckNow() { healthPoll() }

/* ======================= CHILD BUTTONS (Subir / Parar / Descer) ======================= */

@Field static final List<Map> CHILD_BUTTON_DEFS = [
  [label:"Cortina - Subir",  cmd:1],
  [label:"Cortina - Parar",  cmd:2],
  [label:"Cortina - Descer", cmd:3]
]

/**
 * Cria/atualiza 3 children do tipo "Generic Component Switch".
 * Ao ligar (componentOn), o parent envia o comando RF correspondente e volta o child para OFF (momentâneo).
 */

def recreateButtons() { createOrUpdateChildButtons(true) }

private void createOrUpdateChildButtons(Boolean removeExtras=false) {
  if (logEnable) log.debug "Criando/atualizando Child Buttons..."
  Set<String> keep = []
  CHILD_BUTTON_DEFS.eachWithIndex { m, idx ->
    String dni = "${device.id}-BTN-${idx+1}"
    def child = getChildDevice(dni)
    if (!child) {
      child = addChildDevice("hubitat", "Generic Component Switch", dni,
        [name: m.label, label: m.label, isComponent: true])
      if (logEnable) log.debug "Child criado: ${child?.displayName}"
    } else {
      if (child.label != (m.label as String)) child.setLabel(m.label as String)
    }
    child.updateDataValue("cmd", (m.cmd as Integer).toString())
    // Garantir estado OFF visual
    try { child.parse([[name:"switch", value:"off"]]) } catch (ignored) {}
    keep << dni
  }

  if (removeExtras) {
    childDevices?.findAll { !(it.deviceNetworkId in keep) }?.each {
      if (logEnable) log.warn "Removendo child extra: ${it.displayName}"
      deleteChildDevice(it.deviceNetworkId)
    }
  }
}

// Callbacks do Generic Component Switch

def componentOn(cd)  { handleChildPress(cd) }

def componentOff(cd) { /* ignorar */ }

private void handleChildPress(cd) {
  String cmdStr = cd.getDataValue("cmd") ?: ""
  if (!cmdStr) {
    log.warn "Child ${cd.displayName} sem cmd."
    return
  }
  Integer cmd = cmdStr as Integer
  if (logEnable) log.info "Child '${cd.displayName}' acionado -> cmd=${cmd}"

  // Dispara comando mantendo compatibilidade com o driver
  EnviaComando(cmd)
  if (cmd == 1) trackStart("up")
  else if (cmd == 3) trackStart("down")
  else finalizeStop(estimateNow())

  // Volta para OFF como momentary
  runIn(1, "childOffSafe", [data:[dni: cd.deviceNetworkId]])
}


def childOffSafe(data) {
  def child = getChildDevice(data?.dni as String)
  if (child) {
    try { child.parse([[name:"switch", value:"off"]]) } catch (ignored) {}
  }
}

/* ======================= Util ======================= */
private Integer clamp(int v, int lo, int hi) { Math.max(lo, Math.min(hi, v)) }


def logsOff() {
  log.warn 'logging disabled...'
  device.updateSetting('logEnable', [value:'false', type:'bool'])
}
