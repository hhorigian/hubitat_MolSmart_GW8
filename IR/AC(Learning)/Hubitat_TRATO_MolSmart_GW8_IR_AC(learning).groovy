
/**
 *  MolSmart GW8 Driver - AC - GW8 (usando os códigos e controles do GW8 Salvos)
 *
 *  Copyright 2024 VH 
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 *            --- Driver para GW8 - AC - Salvos no GW8 (biblioteca ou learning)
 *            V.1.0 25/03/2026 - V1
 *            1.2 - 6/5/2026 - Added IR Channel number to Preferences Input and to HTTP command

 */


metadata {
  definition (name: "MolSmart - GW8 - AC (learning)", namespace: "TRATO", author: "VH", vid: "generic-contact") {
        capability "Actuator"
        capability "Sensor"
        capability "Temperature Measurement"
        capability "Thermostat"

        attribute "supportedThermostatFanModes", "JSON_OBJECT"
        attribute "supportedThermostatModes", "JSON_OBJECT"
        attribute "hysteresis", "NUMBER"
        attribute "lastResponseCode", "NUMBER"
        attribute "lastHttpResult", "STRING"

        command "setTemperature", ["NUMBER"]
        command "setThermostatOperatingState", ["ENUM"]
        command "setThermostatSetpoint", ["NUMBER"]
        command "setSupportedThermostatFanModes", ["JSON_OBJECT"]
        command "setSupportedThermostatModes", ["JSON_OBJECT"]
        command "setCoolingSetpoint", ["NUMBER"]
        command "initialize"
        command "cleanvars"
        command "healthCheckNow"
        command "on"
        command "off"
        command "stop"

        // Atributos de saúde/conectividade
        attribute "gw8Online", "STRING"
        attribute "lastHealthAt", "STRING"
        attribute "healthLatencyMs", "NUMBER"
        attribute "gw8Version", "STRING"
  }
}

    import groovy.transform.Field
    import groovy.json.JsonOutput

    @Field static final String DRIVER   = "by TRATO"
    @Field static final String USER_GUIDE = "https://github.com/hhorigian/hubitat_MolSmart_GW8/tree/main/AC/Idoor"

    String fmtHelpInfo(String str) {
        String prefLink = "<a href='${USER_GUIDE}' target='_blank'>${str}<br><div style='font-size: 70%;'>${DRIVER}</div></a>"
        return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>"
    }


preferences {
    input name: "molIPAddress",  type: "text",   title: "MolSmart GW8 IP Address",           submitOnChange: true, required: true, defaultValue: "192.168.1.100"
    input name: "user",          type: "string", title: "Usuário (admin)",                    required: true, defaultValue: "admin"
    input name: "password",      type: "string", title: "Senha",                              required: true, defaultValue: "12345678"
    input name: "cId",           type: "string", title: "Control ID (salvo no GW8)",          required: true
    input name: "defaultTemp",   type: "number", title: "Temperatura padrão (°C)",            defaultValue: 24, range: "16..30"
    input name: "channel", title:"Canal IR", type: "string", required: true
    
    input name: "logEnable",     type: "bool",   title: "Enable debug logging",               defaultValue: false
    input name: "timeoutSec",    type: "number", title: "HTTP timeout (segundos)",            defaultValue: 7, range: "3..30"
    // Health Check
    input name: "enableHealthCheck", type: "bool",   title: "Ativar verificação de online (HTTP /info)", defaultValue: true
    input name: "healthCheckMins",   type: "number", title: "Intervalo do health check (min)",           defaultValue: 30, range: "1..1440"
    // Help
    input name: "UserGuide", type: "hidden", title: fmtHelpInfo("Manual do Driver")
}


/* ══════════════════════════════════════════════════════════════
   LIFECYCLE
   ══════════════════════════════════════════════════════════════ */

def installed() {
    log.warn "installed..."
    initialize()
    sendEvent(name: "gw8Online", value: "unknown")
}

def updated() {
    log.debug "updated()"
    AtualizaDadosgw8()
    initialize()
    if (!device.currentValue("gw8Online")) sendEvent(name: "gw8Online", value: "unknown")
}

def initialize() {
    log.debug "initialize()"
    if (state?.lastRunningMode == null) {
        sendEvent(name: "temperature",        value: convertTemperatureIfNeeded(68.0, "F", 1))
        sendEvent(name: "thermostatSetpoint", value: convertTemperatureIfNeeded(68.0, "F", 1))
        sendEvent(name: "coolingSetpoint",    value: "24", descriptionText: "coolingSetpoint set to 24")
        sendEvent(name: "heatingSetpoint",    value: "24", descriptionText: "heatingSetpoint set to 24")
        state.lastRunningMode = "cool"
        updateDataValue("lastRunningMode", "cool")
        setThermostatOperatingState("idle")
        setSupportedThermostatFanModes(JsonOutput.toJson(["auto", "high", "mid", "low"]))
        setSupportedThermostatModes(JsonOutput.toJson(["auto", "cool", "heat", "dry", "fan", "off"]))
        off()
        fanAuto()
    }
    sendEvent(name: "hysteresis", value: (hysteresis ?: 0.5).toBigDecimal())
    if (enableHealthCheck) scheduleHealth()
}

def cleanvars() {
    state.clear()
    AtualizaDadosgw8()
}

def AtualizaDadosgw8() {
    state.currentip  = settings.molIPAddress
    state.user       = settings.user
    state.password   = settings.password
    state.cId        = settings.cId
    state.channel    = settings.channel
    log.info "Dados GW8 atualizados: ${state.currentip} | user=${state.user} | cId=${state.cId} | channel =${state.channel}"
}


/* ══════════════════════════════════════════════════════════════
   URL BUILDER  ← NÚCLEO DO DRIVER
   
   Parâmetros do Map (p):
     pw  : 1 = ligado, 0 = desligado
     md  : -1=off, 0=auto, 1=cool, 2=heat, 3=dry, 4=fan
     t   : temperatura (16..30)
     s   : fan speed — 0=auto,1=min,2=low,3=med,4=high,5=max
     v   : swing  — 0=fechado, 1=aberto
   ══════════════════════════════════════════════════════════════ */

private String buildFullUrl(Map p) {
    def ip  = settings.molIPAddress
    def cid = settings.cId
    def usr = settings.user
    def pwd = settings.password
    def chn = settings.channel

    // Valores padrão para parâmetros não informados
    def pw  = (p.pw  != null) ? p.pw  : 1
    def md  = (p.md  != null) ? p.md  : 0
    def t   = (p.t   != null) ? p.t   : (settings.defaultTemp ?: 24)
    def s   = (p.s   != null) ? p.s   : 0
    def v   = (p.v   != null) ? p.v   : 0

    // rcId = 52 é fixo para AC; state=2 é o modo de envio de comando completo do GW8
    String url = "http://${ip}/control" +
                 "?cId=${cid}&rcId=52&state=2" +
                 "&t=${t}&pw=${pw}&md=${md}&s=${s}&v=${v}" +
                 "&tp=0&type=1&p=0&m=0&c=${chn}" +
                 "&user=${usr}&pwd=${pwd}"

    logDebug "buildFullUrl: ${url}"
    return url
}


/* ══════════════════════════════════════════════════════════════
   COMANDOS PRINCIPAIS
   ══════════════════════════════════════════════════════════════ */

// Temperatura atual de setpoint (usada internamente)
private int currentTemp() {
    def t = device.currentValue("coolingSetpoint")
    return t ? t.toInteger() : (settings.defaultTemp ?: 24)
}

def on() {
    sendEvent(name: "thermostatMode", value: "on", isStateChange: true)
    EnviaComando([pw: 1, md: 1, t: currentTemp(), s: 0, v: 0])
    log.info "AC ligado (cool, temp=${currentTemp()})"
}

def off() {
    sendEvent(name: "thermostatMode",         value: "off",  isStateChange: true)
    sendEvent(name: "thermostatOperatingState", value: "idle")
    EnviaComando([pw: 0, md: -1, t: currentTemp(), s: 0, v: 0])
    log.info "AC desligado"
}

def stop() {    
    off()    
}

def auto() {
    sendEvent(name: "thermostatMode", value: "auto")
    EnviaComando([pw: 1, md: 0, t: currentTemp(), s: 0, v: 0])
    log.info "Modo: auto"
}

def cool() {
    sendEvent(name: "thermostatMode", value: "cool")
    EnviaComando([pw: 1, md: 1, t: currentTemp(), s: 0, v: 0])
    log.info "Modo: cool"
}

def heat() {
    sendEvent(name: "thermostatMode", value: "heat")
    EnviaComando([pw: 1, md: 2, t: currentTemp(), s: 0, v: 0])
    log.info "Modo: heat"
}

def dry() {
    sendEvent(name: "thermostatMode", value: "dry")
    EnviaComando([pw: 1, md: 3, t: currentTemp(), s: 0, v: 0])
    log.info "Modo: dry"
}

def fan() {
    sendEvent(name: "thermostatMode", value: "fan")
    EnviaComando([pw: 1, md: 4, t: currentTemp(), s: 0, v: 0])
    log.info "Modo: fan"
}


/* ══════════════════════════════════════════════════════════════
   TEMPERATURA
   ══════════════════════════════════════════════════════════════ */

def setCoolingSetpoint(temperature) {
    def t = temperature.toInteger()
    sendEvent(name: "coolingSetpoint",    value: t, unit: "°C")
    sendEvent(name: "thermostatSetpoint", value: t, unit: "°C")
    EnviaComando([pw: 1, md: 1, t: t, s: 0, v: 0])
    log.info "setCoolingSetpoint: ${t}°C"
}

def setHeatingSetpoint(temperature) {
    def t = temperature.toInteger()
    sendEvent(name: "heatingSetpoint",    value: t, unit: "°C")
    sendEvent(name: "thermostatSetpoint", value: t, unit: "°C")
    EnviaComando([pw: 1, md: 2, t: t, s: 0, v: 0])
    log.info "setHeatingSetpoint: ${t}°C"
}

def setThermostatSetpoint(temperature) {
    def t = temperature.toInteger()
    sendEvent(name: "thermostatSetpoint", value: t, unit: "°C")
    // Determina modo pelo lastRunningMode
    def md = (state.lastRunningMode == "heat") ? 2 : 1
    EnviaComando([pw: 1, md: md, t: t, s: 0, v: 0])
    log.info "setThermostatSetpoint: ${t}°C"
}


/* ══════════════════════════════════════════════════════════════
   MODO DO TERMOSTATO (via setThermostatMode)
   ══════════════════════════════════════════════════════════════ */

def setThermostatMode(modo) {
    sendEvent(name: "thermostatMode", value: modo)
    switch (modo) {
        case "auto" : EnviaComando([pw: 1, md:  0, t: currentTemp(), s: 0, v: 0]); break
        case "cool" : EnviaComando([pw: 1, md:  1, t: currentTemp(), s: 0, v: 0]); break
        case "heat" : EnviaComando([pw: 1, md:  2, t: currentTemp(), s: 0, v: 0]); break
        case "dry"  : EnviaComando([pw: 1, md:  3, t: currentTemp(), s: 0, v: 0]); break
        case "fan"  : EnviaComando([pw: 1, md:  4, t: currentTemp(), s: 0, v: 0]); break
        case "off"  : off(); break
        default: logDebug("setThermostatMode: modo inválido '${modo}'")
    }
    log.info "setThermostatMode: ${modo}"
}

def setThermostatOperatingState(operatingState) {
    logDebug "setThermostatOperatingState(${operatingState})"
    updateSetpoints(null, null, null, operatingState)
    sendEvent(name: "thermostatOperatingState", value: operatingState,
              descriptionText: getDescriptionText("thermostatOperatingState set to ${operatingState}"))
}


/* ══════════════════════════════════════════════════════════════
   FAN MODE
   ══════════════════════════════════════════════════════════════ */

def fanAuto()      { setThermostatFanMode("auto") }
def fanOn()        { setThermostatFanMode("on")   }
def fanCirculate() { setThermostatFanMode("circulate") }
def fanLow()       { setThermostatFanMode("low")  }
def fanMed()       { setThermostatFanMode("mid")  }
def fanHigh()      { setThermostatFanMode("high") }

def setThermostatFanMode(modo) {
    sendEvent(name: "thermostatFanMode", value: modo)
    def s
    switch (modo) {
        case "auto"      : s = 0; break
        case "low"       : s = 2; break
        case "mid"       : s = 3; break
        case "high"      : s = 5; break
        case "circulate" : s = 1; break
        case "on"        : s = 3; break   // "on" = medium
        default          : s = 0
    }
    EnviaComando([pw: 1, md: (state.lastRunningMode == "heat" ? 2 : 1), t: currentTemp(), s: s, v: 0])
    log.info "setThermostatFanMode: ${modo} (s=${s})"
}


/* ══════════════════════════════════════════════════════════════
   PUSH (dashboard buttons)
   ══════════════════════════════════════════════════════════════ */

def push(pushed) {
    logDebug("push: button = ${pushed}")
    if (pushed == null) { logWarn("push: null. Ignorado"); return }
    switch (pushed.toInteger()) {
        case 1  : on();           break
        case 2  : off();          break
        case 3  : auto();         break
        case 4  : heat();         break
        case 5  : cool();         break
        case 6  : fan();          break
        case 7  : dry();          break
        case 8  : fanAuto();      break
        case 9  : fanOn();        break
        case 10 : fanCirculate(); break
        case 13 : fanAuto();      break
        case 14 : fanLow();       break
        case 15 : fanMed();       break
        case 16 : fanHigh();      break
        default : logDebug("push: botão inválido (${pushed})")
    }
}


/* ══════════════════════════════════════════════════════════════
   HTTP — ENVIO
   ══════════════════════════════════════════════════════════════ */

def EnviaComando(Map p) {
    String fullUrl = buildFullUrl(p)
    log.info "EnviaComando URL: ${fullUrl}"
    Map params = [ uri: fullUrl, timeout: (settings.timeoutSec ?: 7) as int ]
    try {
        asynchttpPost('gw8PostCallback', params, [url: fullUrl])
    } catch (e) {
        log.warn "${device.displayName} asynchttpPost falhou: ${e.message}"
    }
}

void gw8PostCallback(resp, data) {
    Integer code = resp?.status as Integer
    try {
        if (code in 200..299) {
            logDebug "POST OK status=${code}"
            sendEvent(name: "lastResponseCode", value: code)
            sendEvent(name: "lastHttpResult",   value: "${code} OK")
        } else {
            logWarn "POST ERROR status=${code}"
            sendEvent(name: "lastResponseCode", value: code ?: 0)
            sendEvent(name: "lastHttpResult",   value: "${code} ERROR")
        }
    } catch (e) {
        logWarn "Callback exception: ${e.message}"
        sendEvent(name: "lastResponseCode", value: -1)
        sendEvent(name: "lastHttpResult",   value: "EXCEPTION")
    }
}


/* ══════════════════════════════════════════════════════════════
   HEALTH CHECK
   ══════════════════════════════════════════════════════════════ */

private void scheduleHealth() {
    Integer mins = Math.max(1, (healthCheckMins ?: 5) as int)
    unschedule("healthPoll")
    runIn(2, "healthPoll")
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
    Long started = now()
    Map params = [ uri: "http://${ip}/info?user=${settings.user}&pwd=${settings.password}", timeout: 5 ]
    try {
        asynchttpGet('healthPollCB', params, [t0: started])
        log.info params
    } catch (e) {
        if (logEnable) log.warn "healthPoll falhou: ${e.message}"
    }
}

void healthPollCB(resp, data) {
    String body = ""
    Integer st  = null
    try { st = resp?.status as Integer; body = resp?.getData() ?: "" } catch (ignored) {}
    String stamp = new Date().format("yyyy-MM-dd HH:mm:ss")
    Long dt = (now() - (data?.t0 ?: now()))

    if (st && st >= 200 && st <= 299 && body?.toString()?.contains("MolSmart Device Info")) {
        if (device.currentValue("gw8Online") != "online") sendEvent(name: "gw8Online", value: "online", isStateChange: true)
        sendEvent(name: "healthLatencyMs", value: dt as Long)
        sendEvent(name: "lastHealthAt",    value: stamp)
        try {
            def m = (body =~ /(?im)^\s*Version:\s*([^\r\n]+)/)
            if (m.find()) {
                String ver = (m.group(1) ?: "").trim()
                String ver6 = (ver.length() >= 6) ? ver.substring(0, 6) : ver
                if (ver6) sendEvent(name: "gw8Version", value: ver6, isStateChange: true)
            }
        } catch (e) { if (logEnable) log.warn "Falha ao extrair versão: ${e.message}" }
        if (logEnable) log.debug "Health OK ${dt}ms"
    } else {
        if (device.currentValue("gw8Online") != "offline") sendEvent(name: "gw8Online", value: "offline", isStateChange: true)
        sendEvent(name: "healthLatencyMs", value: null)
        sendEvent(name: "lastHealthAt",    value: stamp)
        if (logEnable) log.warn "Health FAIL (status=${st})"
    }
}

def healthCheckNow() { healthPoll() }


/* ══════════════════════════════════════════════════════════════
   THERMOSTAT SETPOINTS HELPER (copiado do original)
   ══════════════════════════════════════════════════════════════ */

private updateSetpoints(sp = null, hsp = null, csp = null, operatingState = null) {
    if (operatingState in ["off"]) return
    if (hsp == null) hsp = device.currentValue("heatingSetpoint", true)
    if (csp == null) csp = device.currentValue("coolingSetpoint", true)
    if (sp  == null) sp  = device.currentValue("thermostatSetpoint", true)
    if (operatingState == null) operatingState = state.lastRunningMode

    def hspChange = isStateChange(device, "heatingSetpoint",    hsp.toString())
    def cspChange = isStateChange(device, "coolingSetpoint",    csp.toString())
    def spChange  = isStateChange(device, "thermostatSetpoint", sp.toString())
    def osChange  = operatingState != state.lastRunningMode

    def newOS
    def unit = "°${location.temperatureScale}"
    switch (operatingState) {
        case ["pending heat", "heating", "heat"]:
            newOS = "heat"
            if (spChange)             { hspChange = true; hsp = sp }
            else if (hspChange || osChange) { spChange = true; sp = hsp }
            if (csp - 2 < hsp)        { csp = hsp + 2; cspChange = true }
            break
        case ["pending cool", "cooling", "cool"]:
            newOS = "cool"
            if (spChange)             { cspChange = true; csp = sp }
            else if (cspChange || osChange) { spChange = true; sp = csp }
            if (hsp + 2 > csp)        { hsp = csp - 2; hspChange = true }
            break
        default: return
    }

    if (hspChange) sendEvent(name: "heatingSetpoint",    value: hsp, unit: unit, stateChange: true)
    if (cspChange) sendEvent(name: "coolingSetpoint",    value: csp, unit: unit, stateChange: true)
    if (spChange)  sendEvent(name: "thermostatSetpoint", value: sp,  unit: unit, stateChange: true)

    state.lastRunningMode = newOS
    updateDataValue("lastRunningMode", newOS)
}

def setSupportedThermostatFanModes(fanModes) {
    sendEvent(name: "supportedThermostatFanModes", value: fanModes,
              descriptionText: getDescriptionText("supportedThermostatFanModes set to ${fanModes}"))
}

def setSupportedThermostatModes(modes) {
    sendEvent(name: "supportedThermostatModes", value: modes,
              descriptionText: getDescriptionText("supportedThermostatModes set to ${modes}"))
}

def setTemperature(temp) {
    sendEvent(name: "temperature", value: temp, unit: "°C")
}


/* ══════════════════════════════════════════════════════════════
   HELPERS
   ══════════════════════════════════════════════════════════════ */

private logInfo(msg)  { if (settings?.logEnable) log.info  "${device.displayName} ${msg}" }
private logDebug(msg) { if (settings?.logEnable) log.debug "${device.displayName} ${msg}" }
private logWarn(msg)  { log.warn "${device.displayName} ${msg}" }

private getDescriptionText(msg) {
    def txt = "${device.displayName} ${msg}"
    if (settings?.logEnable) log.info txt
    return txt
}

def logsOff() {
    log.warn "logging disabled..."
    device.updateSetting("logEnable", [value: "false", type: "bool"])
}
