/**
 *  MolSmart GW8 Driver - RF - Cortinas, Luzes RF, Controle tudo via RF.
 *  (Com CHILD BUTTONS momentâneos: Subir / Parar / Descer)
 *
 *  Copyright 2025 VH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   +++  Versões ++++
 *        1.0 - 11/11/2025 - V1
 *        1.1 - 26/1/2026 - Fixed Online Status for GW8. Added Version number to driver. Added Memory used in GW8 to status.
 *        1.2 - 27/1/2026 - Added Command: refreshRemoteList, List GW8 Remote Controls.
 *        1.3 - 19/2/2026 - Fixed 100% open / Closed without waiting for percentage. Force Open and Force Close.
 *        1.4 - 11/3/2026 - Added PostCallback to know if command was sent successfull via http. Changed names for childs autocreate pickup label name.
 *        1.5 - 11/3/2026 - Added Open/Close commands for Vetra compat.
 *        1.6 - 20/5/2026 - Fixed event storm: Groovy falsy-zero bug in sendPosition guard, tick() now stops when not moving, sendShadeEventForPos deduplicates via currentValue.
 *        1.7 - 22/5/2026 - Simplificado: removido slider/posição/tempo. Up/Open=1, Stop=2, Down/Close=3.
 *        1.8 - 18/6/2026 - Added stopPositionChange command for Vetra compat.

 */

import groovy.transform.Field

@Field static final List<String> ONLINE_ENUM = ["online","offline","unknown"]
@Field static final String DRIVER_VERSION = "1.7"


metadata {
  definition (name: "MolSmart - GW8 - RF", namespace: "TRATO", author: "VH", vid: "generic-contact") {
    capability "Sensor"
    capability "Actuator"
    capability "Contact Sensor"
    capability "PushableButton"

    command "up"
    command "down"
    command "stop"
    command "Up"
    command "Down"
    command "Stop"
    command "open"
    command "close"
    command "stopPositionChange"

    command "healthCheckNow"
    command "recreateButtons"

    attribute "currentstatus", "string"
    attribute "status", "string"

    attribute "gw3Online", "ENUM", ONLINE_ENUM
    attribute "lastHealthAt", "STRING"
    attribute "healthLatencyMs", "NUMBER"

    attribute "gw8Online", "STRING"
    attribute "gw8StoragePct", "NUMBER"
    attribute "gw8StoragePctText", "STRING"

    attribute "lastResponseCode", "NUMBER"
    attribute "lastHttpResult", "STRING"

    command "refreshRemoteList"

    attribute "gw8RemoteCount", "NUMBER"
    attribute "gw8RemoteList", "STRING"
    attribute "gw8RemoteJson", "STRING"
  }

  preferences {
    input name: "molIPAddress", type: "text",   title: "MolSmart IP",                  required: true, defaultValue: "192.168.1.100"
    input name: "user",         type: "string", title: "Usuário",                      required: true, defaultValue: "admin"
    input name: "password",     type: "string", title: "Senha",                        required: true, defaultValue: "12345678"
    input name: "cId",          type: "string", title: "Control ID (pego no WebAdmin)", required: true
    input name: "logEnable",    type: "bool",   title: "Enable debug logging",          defaultValue: false

    input name: "enableHealthCheck", type: "bool",   title: "Ativar verificação de online (HTTP /info)", defaultValue: true
    input name: "healthCheckMins",   type: "number", title: "Intervalo do health check (min)",           defaultValue: 30, range: "1..1440"

    input name: "createButtonsOnSave", type: "bool", title: "Criar/atualizar Child Buttons ao salvar", defaultValue: true
  }
}


/* ======================= Setup ======================= */

def installed() {
  sendEvent(name: "numberOfButtons", value: 4)
  sendEvent(name: "status", value: "stop")
  sendEvent(name: "gw8Online", value: "unknown")
  sendEvent(name: "driverVersion", value: DRIVER_VERSION)
  initialize()
}

def updated() {
  sendEvent(name: "numberOfButtons", value: 4)
  if (!device.currentValue("gw8Online")) sendEvent(name: "gw8Online", value: "unknown")
  sendEvent(name: "driverVersion", value: DRIVER_VERSION)
  refreshRemoteList()
  initialize()
  if (logEnable) runIn(1800, logsOff)
}

private initialize() {
  unschedule()
  state.currentip  = settings.molIPAddress
  state.username   = settings.user
  state.pwd        = settings.password
  state.cId        = settings.cId
  state.rcId       = 51
  if (logEnable) log.debug "Init -> ip=${state.currentip} cId=${state.cId}"

  if (createButtonsOnSave) createOrUpdateChildButtons(true)
  if (enableHealthCheck)   scheduleHealth()
}


/* ======================= Comandos ======================= */

def up()    { EnviaComando(1) }
def Up()    { EnviaComando(1) }
def open()  { EnviaComando(1) }

def stopPositionChange() {
     stop()
}


def stop()  { EnviaComando(2) }
def Stop()  { EnviaComando(2) }

def down()  { EnviaComando(3) }
def Down()  { EnviaComando(3) }
def close() { EnviaComando(3) }

def push(number) {
  sendEvent(name: "pushed", value: number, isStateChange: true)
  log.info "Enviado o botão " + number
  EnviaComando(number)
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

def EnviaComando(button) {
  settings.timeoutSec = 7
  String fullUrl = buildFullUrl(button)
  if (logEnable) log.info "FullURL = ${fullUrl}"
  Map params = [ uri: fullUrl, timeout: (settings.timeoutSec ?: 7) as int ]
  try {
    asynchttpPost('gw8PostCallback', params, [cmd: button])
    String tempStatus = (button == 1) ? "up" : (button == 2 ? "stop" : (button == 3 ? "down" : "paused"))
    sendEvent(name: "status", value: tempStatus)
    sendEvent(name: "currentstatus", value: tempStatus)
  } catch (e) {
    log.warn "${device.displayName} Async POST scheduling failed: ${e.message}"
  }
}

void gw8PostCallback(resp, data) {
  String cmd = data?.cmd
  Integer code = resp?.status as Integer
  try {
    if (code in 200..299) {
      logDebug "POST OK cmd=${cmd} status=${code}"
      sendEvent(name: "lastResponseCode", value: code)
      sendEvent(name: "lastHttpResult", value: "${code} OK")
      state.ultimamensagem = "Resposta OK (${code})"
    } else {
      logWarn "POST ERROR cmd=${cmd} status=${code}"
      sendEvent(name: "lastResponseCode", value: code ?: 0)
      sendEvent(name: "lastHttpResult", value: "${code} ERROR")
      state.ultimamensagem = "Erro HTTP (${code})"
    }
  } catch (e) {
    logWarn "Async callback exception: ${e.message}"
    sendEvent(name: "lastResponseCode", value: -1)
    sendEvent(name: "lastHttpResult", value: "EXCEPTION")
    state.errormessage = e.message
  }
}


/* ======================= HEALTH CHECK HTTP (/info) ======================= */

private void scheduleHealth() {
  Integer mins = Math.max(1, (healthCheckMins ?: 5) as int)
  unschedule("healthPoll")
  runIn(2, "healthPoll")
  runEveryXMinutes(mins, "healthPoll")
}

private void runEveryXMinutes(Integer mins, String handler) {
  state.healthEveryMins = mins
  runIn(mins * 60, "healthReschedule")
}

def healthReschedule() {
  Integer mins = (state.healthEveryMins ?: (healthCheckMins ?: 5)) as int
  runIn(mins * 60, "healthReschedule")
  healthPoll()
}

def healthPoll() {
  if (!enableHealthCheck) return
  String ip = (settings.molIPAddress ?: "").trim()
  if (!ip) return
  String uri = "http://${ip}/info?type=1"
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
    if (device.currentValue("gw8Online") != "online") sendEvent(name: "gw8Online", value: "online", isStateChange: true)
    sendEvent(name: "healthLatencyMs", value: dt as Long)
    sendEvent(name: "lastHealthAt", value: stamp)

    try {
      String txt = body?.toString() ?: ""
      def m = (txt =~ /(?im)^\s*Version:\s*([^\r\n]+)/)
      if (m.find()) {
        String verFull = (m.group(1) ?: "").trim()
        String ver6 = (verFull.length() >= 6) ? verFull.substring(0, 6) : verFull
        if (ver6) {
          sendEvent(name: "gw8Version", value: ver6, isStateChange: true)
          if (logEnable) log.debug "Versão detectada: '${verFull}' -> gw8Version='${ver6}'"
        }
      }
    } catch (e) {
      if (logEnable) log.warn "Falha ao extrair versão: ${e.message}"
    }

    try {
      String txt2 = body?.toString() ?: ""
      def ms = (txt2 =~ /(?im)^\s*Remote storage:\s*(\d+)\s*\/\s*(\d+)/)
      if (ms.find()) {
        BigDecimal used  = (ms.group(1) as BigDecimal)
        BigDecimal total = (ms.group(2) as BigDecimal)
        if (total > 0) {
          BigDecimal pct1 = ((used * 100G) / total).setScale(1, BigDecimal.ROUND_HALF_UP)
          sendEvent(name: "gw8StoragePctText", value: "${pct1} %", isStateChange: true)
          if (logEnable) log.debug "Memoria Utilizada: ${used}/${total} -> ${pct1}%"
        }
      }
    } catch (e) {
      if (logEnable) log.warn "Falha ao extrair Remote storage: ${e.message}"
    }

    if (logEnable) log.debug "Health OK in ${dt} ms"
  } else {
    if (device.currentValue("gw8Online") != "offline") sendEvent(name: "gw8Online", value: "offline", isStateChange: true)
    sendEvent(name: "healthLatencyMs", value: null)
    sendEvent(name: "lastHealthAt", value: stamp)
    if (logEnable) log.warn "Health FAIL (status=${st})"
  }
}

def healthCheckNow() { healthPoll() }


/* ======================= CHILD BUTTONS (Subir / Parar / Descer) ======================= */

@Field static final List<Map> CHILD_BUTTON_DEFS = [
  [prefix: "Subir Cortina",  cmd: 1],
  [prefix: "Parar Cortina",  cmd: 2],
  [prefix: "Descer Cortina", cmd: 3]
]

private String buildChildLabel(String prefix) {
  String parentLabel = device?.getLabel() ?: device?.getName() ?: "GW8"
  return "${prefix} ${parentLabel}".trim()
}

def recreateButtons() { createOrUpdateChildButtons(true) }

private void createOrUpdateChildButtons(Boolean removeExtras = false) {
  if (logEnable) log.debug "Criando/atualizando Child Buttons..."
  Set<String> keep = []
  CHILD_BUTTON_DEFS.eachWithIndex { m, idx ->
    String dni = "${device.id}-BTN-${idx + 1}"
    String childLabel = buildChildLabel(m.prefix as String)
    def child = getChildDevice(dni)
    if (!child) {
      child = addChildDevice("hubitat", "Generic Component Switch", dni,
        [name: childLabel, label: childLabel, isComponent: true])
      if (logEnable) log.debug "Child criado: ${child?.displayName}"
    } else {
      if (child.label != childLabel) child.setLabel(childLabel)
    }
    child.updateDataValue("cmd", (m.cmd as Integer).toString())
    try { child.parse([[name: "switch", value: "off"]]) } catch (ignored) {}
    keep << dni
  }
  if (removeExtras) {
    childDevices?.findAll { !(it.deviceNetworkId in keep) }?.each {
      if (logEnable) log.warn "Removendo child extra: ${it.displayName}"
      deleteChildDevice(it.deviceNetworkId)
    }
  }
}

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
  EnviaComando(cmd)
  runIn(1, "childOffSafe", [data: [dni: cd.deviceNetworkId]])
}

def childOffSafe(data) {
  def child = getChildDevice(data?.dni as String)
  if (child) {
    try { child.parse([[name: "switch", value: "off"]]) } catch (ignored) {}
  }
}


/* ======================= Remote List ======================= */

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def refreshRemoteList() {
  try {
    def params = [
      uri: "http://${molIPAddress}/remoteData",
      requestContentType: "application/json",
      contentType: "application/json",
      body: JsonOutput.toJson([type: 2]),
      timeout: 10
    ]
    asynchttpPost("remoteListCB", params)
  } catch (e) {
    if (logEnable) log.warn "refreshRemoteList() falhou: ${e.message}"
  }
}

def remoteListCB(resp, data) {
  try {
    if (resp?.status != 200) {
      if (logEnable) log.warn "remoteListCB HTTP ${resp?.status}"
      return
    }
    def parsed = new JsonSlurper().parseText(resp.data ?: "[]")
    List<Map> remotes = []
    if (parsed instanceof List) {
      remotes = parsed.findAll { r -> r?.rcId == 51 }.collect { r ->
        [id: r?.id, name: (r?.name ?: "")]
      }
    }
    sendEvent(name: "gw8RemoteCount", value: remotes.size(), isStateChange: true)
    sendEvent(name: "gw8RemoteJson",  value: JsonOutput.toJson(remotes), isStateChange: true)
    state.gw8Remotes = remotes
  } catch (e) {
    if (logEnable) log.warn "remoteListCB parse falhou: ${e.message}"
  }
}


/* ======================= Util ======================= */

def logsOff() {
  log.warn 'logging disabled...'
  device.updateSetting('logEnable', [value: 'false', type: 'bool'])
}

private logDebug(msg) { if (settings?.debugOutput == true) log.debug "${device.displayName} ${msg}" }
private logWarn(msg)  { log.warn "${device.displayName} ${msg}" }
