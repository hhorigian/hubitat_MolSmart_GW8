/**
 *  MolSmart GW8 Driver - IR Universal Learning -  Universal
 *
 *  Copyright 2025 VH 
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
 *
 *            --- Driver para GW8 - IR Universal do iDoor ---
 *            V.1. 11/11/2025 
 */
metadata {
  definition (name: "MolSmart - GW8 - IR Universal(Learning)", namespace: "TRATO", author: "VH", vid: "generic-contact") {
        capability "Switch"
        capability "Thermostat"
        capability "Thermostat Cooling Setpoint"
        capability "Thermostat Setpoint"
        capability "Sensor"
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        capability "HealthCheck"   
        capability "PushableButton"
        capability "TV"  
        capability "Samsung TV"        
      
        command "Botao1"      
        command "Botao2"      
        command "Botao3"      
        command "Botao4"
        command "Botao5"   
        command "Botao6"
        command "Botao7"
        command "Botao8"  
        command "Botao9"
    	command "Botao10"
    	command "Botao11"
    	command "Botao12"
    	command "Botao13"
    	command "Botao14"
    	command "Botao15"
    	command "Botao16"
    	command "Botao17"
    	command "Botao18"
    	command "Botao19"
    	command "Botao20"
        command "recreateButtons"
        command "removeButtons"   
	    command "healthCheckNow"

    // NOVOS atributos de saúde/conectividade
    attribute "gw3Online", "ENUM", ONLINE_ENUM
    attribute "lastHealthAt", "STRING"
    attribute "healthLatencyMs", "NUMBER"

    // NOVO: versão do GW3 (6 caracteres após "Version: ")
    attribute "gw8Version", "STRING"      
      
      
  }
    
    
  }

    import groovy.transform.Field
    @Field static final String DRIVER = "by TRATO"
    @Field static final String USER_GUIDE = "https://github.com/hhorigian/hubitat_MolSmart_GW3_IR/tree/main/Universal"


    String fmtHelpInfo(String str) {
    String prefLink = "<a href='${USER_GUIDE}' target='_blank'>${str}<br><div style='font-size: 70%;'>${DRIVER}</div></a>"
    return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>"
    }


  preferences {
    	input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "molIPAddress", type: "text", title: "MolSmart GW8 IP Address", submitOnChange: true, required: true, defaultValue: "192.168.1.100" 
    	input name: "user", title:"Usuário", type: "string", required: true, defaultValue: "admin" 
	    input name: "password", title:"Senha", type: "string", required: true, defaultValue: "12345678" 
    	input name: "cId", title:"Control ID (pego no WebAdmin)", type: "string", required: true        
        input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "debugOutput", type: "bool", title: "Habilitar Log", defaultValue: false           
	    input name: "createButtonsOnSave", type: "bool", title: "Criar/atualizar Child Switches para botões ao salvar", defaultValue: false

	  //help guide
        input name: "UserGuide", type: "hidden", title: fmtHelpInfo("Manual do Driver") 

	input name: "BotaoOn", title:"ComandoOn-Botão(1)", type: "string" , defaultValue: "1"
	input name: "BotaoOff", title:"BotaoOff-Botão(2)", type: "string" , defaultValue: "2"
    input name: "Botao3", title:"Botao3-Botão(3)", type: "string" , defaultValue: "3"
	input name: "Botao4", title:"Botao4-Botão(4)", type: "string" , defaultValue: "4" 	
    input name: "Botao5", title:"Botao5-Botão(5)", type: "string" , defaultValue: "5" 
    input name: "Botao6", title:"Botao6-Botão(6)", type: "string" , defaultValue: "6"
    input name: "Botao7", title:"Botao7-Botão(7)", type: "string" , defaultValue: "7"
	input name: "Botao8", title:"Botao8-Botão(8)", type: "string" , defaultValue: "8"
	input name: "Botao9", title:"Botao9-Botão(9)", type: "string" , defaultValue: "9"
	input name: "Botao10", title:"Botao10-Botão(10)", type: "string" , defaultValue: "10"
	input name: "Botao11", title:"Botao11-Botão(11)", type: "string" , defaultValue: "11"
	input name: "Botao12", title:"Botao10-Botão(12)", type: "string" , defaultValue: "12"
	input name: "Botao13", title:"Botao10-Botão(13)", type: "string" , defaultValue: "13"
	input name: "Botao14", title:"Botao10-Botão(14)", type: "string" , defaultValue: "14"
	input name: "Botao15", title:"Botao10-Botão(15)", type: "string" , defaultValue: "15"
	input name: "Botao16", title:"Botao10-Botão(16)", type: "string" , defaultValue: "16"
	input name: "Botao17", title:"Botao10-Botão(17)", type: "string" , defaultValue: "17"
	input name: "Botao18", title:"Botao10-Botão(18)", type: "string" , defaultValue: "18"
	input name: "Botao19", title:"Botao10-Botão(19)", type: "string" , defaultValue: "19"
	input name: "Botao20", title:"Botao10-Botão(20)", type: "string" , defaultValue: "20"      

    // === NOVO: Health Check ===
    input name: "enableHealthCheck", type: "bool",   title: "Ativar verificação de online (HTTP /info)", defaultValue: true
    input name: "healthCheckMins",   type: "number", title: "Intervalo do health check (min)", defaultValue: 30, range: "1..1440"
      
       
  }   
  

def initialized()
{
    state.botaouniversal = ""
    log.debug "initialized()"
	  // Agenda health check se habilitado
	  if (enableHealthCheck) scheduleHealth()    
    
}

def refresh()
{
 log.info "Refresh"   
}
def installed()
{
   

    sendEvent(name:"numberOfButtons", value:20)     
    log.debug "installed()" 
    // Atributos novos default
    sendEvent(name:"gw3Online", value:"unknown")
    
}

def updated()
{
   
    sendEvent(name:"numberOfButtons", value:20)    
    log.debug "updated()"
	// Garante atributo
	if (!device.currentValue("gw3Online")) sendEvent(name:"gw3Online", value:"unknown")    
    AtualizaDadosGW3()
	if (logEnable) runIn(1800,logsOff)
    if (createButtonsOnSave) createOrUpdateChildButtons(true)    
    
    
}

def AtualizaDadosGW3() {
    state.currentip = settings.molIPAddress
    state.username = settings.user
    state.pwd = settings.password
    state.cId = settings.cId
    state.rcId = 61
    log.info "Dados do GW8 atualizados: " + state.currentip + " -- " + " -- " + state.channel  + " -- " + state.cId  
    
}

def on() {
    sendEvent(name: "status", value: "on", descriptionText: "Universal Remote Set to on", isStateChange: true)
     def ircode =  (settings.BotaoOn ?: "")
     EnviaComando(ircode)

}

def off() {
     sendEvent(name: "status", value: "off", descriptionText: "Universal Remote Set to off", isStateChange: true)
     def ircode =  (settings.BotaoOff ?: "")    
     EnviaComando(ircode)
         
}

//Case para los botones de push en el dashboard. 
def push(pushed) {
	logDebug("push: button = ${pushed}")
	if (pushed == null) {
		logWarn("push: pushed is null.  Input ignored")
		return
	}
	pushed = pushed.toInteger()
	switch(pushed) {
        case 1: Botao1(); break
        case 2: Botao2(); break
        case 3: Botao3(); break
		case 4 : Botao4(); break
		case 5 : Botao5(); break
        case 6 : Botao6(); break
        case 7 : Botao7(); break
        case 8 : Botao8(); break                
        case 9 : Botao9(); break    
        case 10 : Botao10(); break            
        case 11 : Botao10(); break            
        case 12 : Botao12(); break            
        case 13 : Botao13(); break            
        case 14 : Botao14(); break            
        case 15 : Botao15(); break            
        case 16 : Botao16(); break            
        case 17 : Botao17(); break            
        case 18 : Botao18(); break            
        case 19 : Botao19(); break            
        case 20 : Botao20(); break           
		default:
			logDebug("push: Botão inválido.")
			break
	}
}
		
//Botão #1 para dashboard
def Botao1(){
    sendEvent(name: "status", value: "Botao1")
    def ircode =  "1"
    EnviaComando(ircode)    
    state.botaouniversal = ircode
}

//Botão #1 para dashboard
def Botao2(){
    sendEvent(name: "status", value: "Botao2")
    def ircode =  "2"
    EnviaComando(ircode)    
    state.botaouniversal = ircode
}

//Botão #3 para dashboard
def Botao3(){
    sendEvent(name: "status", value: "Botao3")
    def ircode =  "3"
    EnviaComando(ircode)    
    state.botaouniversal = ircode
}


//Botão #4 para dashboard
def Botao4(){
    sendEvent(name: "status", value: "Botao4")
    def ircode =  (settings.Botao4 ?: "")
    EnviaComando(ircode)    
}

//Botão #5 para dashboard
def Botao5(){
    sendEvent(name: "status", value: "Botao5")
    def ircode =  (settings.Botao5 ?: "")
    EnviaComando(ircode)    
}

//Botão #6 para dashboard
def Botao6(){
    sendEvent(name: "status", value: "Botao6")
    def ircode =  (settings.Botao6 ?: "")
    EnviaComando(ircode)    
}


//Botão #7 para dashboard
def Botao7(){
    sendEvent(name: "status", value: "Botao7")
    def ircode =  (settings.Botao7 ?: "")
    EnviaComando(ircode)
}

//Botão #8 para dashboard
def Botao8(temperature){
    sendEvent(name: "status", value: "Botao8" )
    def ircode =  (settings.Botao8 ?: "")
    EnviaComando(ircode)
}

//Botão #9 para dashboard
def Botao9(){
    sendEvent(name: "status", value: "Botao9" )
    def ircode =  (settings.Botao9 ?: "")
    EnviaComando(ircode)
}

//Botão #10 para dashboard
def Botao10(){
    sendEvent(name: "status", value: "Botao10" )
    def ircode =  (settings.Botao10 ?: "")
    EnviaComando(ircode)
}

//Botão #10 para dashboard
def Botao11(){
    sendEvent(name: "status", value: "Botao11" )
    def ircode =  (settings.Botao11 ?: "")
    EnviaComando(ircode)
}

//Botão #10 para dashboard
def Botao12(){
    sendEvent(name: "status", value: "Botao12" )
    def ircode =  (settings.Botao12 ?: "")
    EnviaComando(ircode)
}

//Botão #10 para dashboard
def Botao13(){
    sendEvent(name: "status", value: "Botao13" )
    def ircode =  (settings.Botao13 ?: "")
    EnviaComando(ircode)
}

//Botão #10 para dashboard
def Botao14(){
    sendEvent(name: "status", value: "Botao14" )
    def ircode =  (settings.Botao14 ?: "")
    EnviaComando(ircode)
}

//Botão #10 para dashboard
def Botao15(){
    sendEvent(name: "status", value: "Botao15" )
    def ircode =  (settings.Botao15 ?: "")
    EnviaComando(ircode)
}

//Botão #10 para dashboard
def Botao16(){
    sendEvent(name: "status", value: "Botao16" )
    def ircode =  (settings.Botao16 ?: "")
    EnviaComando(ircode)
}

//Botão #10 para dashboard
def Botao17(){
    sendEvent(name: "status", value: "Botao17" )
    def ircode =  (settings.Botao17 ?: "")
    EnviaComando(ircode)
}

//Botão #10 para dashboard
def Botao18(){
    sendEvent(name: "status", value: "Botao18" )
    def ircode =  (settings.Botao18 ?: "")
    EnviaComando(ircode)
}

//Botão #10 para dashboard
def Botao19(){
    sendEvent(name: "status", value: "Botao19" )
    def ircode =  (settings.Botao19 ?: "")
    EnviaComando(ircode)
}

//Botão #10 para dashboard
def Botao20(){
    sendEvent(name: "status", value: "Botao20" )
    def ircode =  (settings.Botao20 ?: "")
    EnviaComando(ircode)
}



private String buildFullUrl(button) {
    def ip   = settings.molIPAddress
    def sn   = settings.user
    def vc   = settings.password
    def cid  = settings.cId
    def rcid = (settings.rcId ?: "61")
        
        return "http://${ip}/control" + "?cId=${cid}&pwd=${vc}&rcId=${rcid}&state=${button}&user=${sn}"	
    
}             




def EnviaComando(button) {
    //if (!pw) return
	
    settings.timeoutSec  = 7    
    String fullUrl = buildFullUrl(button)
    log.info "FullURL = " + fullUrl

    // params: give only a 'uri' so Hubitat won't rebuild/encode the query
    Map params = [ uri: fullUrl, timeout: (settings.timeoutSec ?: 7) as int ]
    log.info "Params = " + params
	
        try {
            asynchttpPost('gw3PostCallback', params, [cmd: button])
        } catch (e) {
            log.warn "${device.displayName} Async POST scheduling failed: ${e.message}"
    }
}

void gw3PostCallback(resp, data) {
    String cmd = data?.cmd
    try {
        if (resp?.status in 200..299) {
            logDebug "POST OK (async) cmd=${cmd} status=${resp?.status}"
             state.ultimamensagem =  "Resposta OK"

        } else {
            logWarn "POST error (async) status=${resp?.status} cmd=${cmd}"
            state.ultimamensagem =  "Erro no envio do comando"
            
        }
    } catch (e) {
        logWarn "Async callback exception: ${e.message} (cmd=${cmd})"
        state.errormessage = e.message
        
    }
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

    // === NOVO: extrair "Version: X" e publicar 6 chars em gw8Version ===
    try {
      String txt = body?.toString() ?: ""
      // procura linha iniciando com "Version:"
      def m = (txt =~ /(?im)^\s*Version:\s*([^\r\n]+)/)
      if (m.find()) {
        String verFull = (m.group(1) ?: "").trim()
        String ver6 = (verFull.length() >= 6) ? verFull.substring(0, 6) : verFull
        if (ver6) {
          sendEvent(name:"gw8Version", value: ver6, isStateChange:true)
          if (logEnable) log.debug "Versão detectada: '${verFull}' -> gw8Version='${ver6}'"
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





private logInfo(msg)  { if (settings?.txtEnable   != false) log.info  "${device.displayName} ${msg}" }
private logDebug(msg) { if (settings?.debugOutput == true)  log.debug "${device.displayName} ${msg}" }
private logWarn(msg)  { log.warn "${device.displayName} ${msg}" }

def logsOff() {
    log.warn 'logging disabled...'
    device.updateSetting('logInfo', [value:'false', type:'bool'])
    device.updateSetting('logWarn', [value:'false', type:'bool'])
    device.updateSetting('logDebug', [value:'false', type:'bool'])
    device.updateSetting('logTrace', [value:'false', type:'bool'])
}




/* ======================= CHILD SWITCHES (Botões como Switch momentâneo) ======================= */

@Field static final List<Map> UNIV_CHILD_BUTTON_DEFS = [
  [label:"Universal - On",  handler:"on"],
  [label:"Universal - Off", handler:"off"],
  [label:"Universal - Botão 1",  handler:"Botao1"],
  [label:"Universal - Botão 2",  handler:"Botao2"],
  [label:"Universal - Botão 3",  handler:"Botao3"],
  [label:"Universal - Botão 4",  handler:"Botao4"],
  [label:"Universal - Botão 5",  handler:"Botao5"],
  [label:"Universal - Botão 6",  handler:"Botao6"],
  [label:"Universal - Botão 7",  handler:"Botao7"],
  [label:"Universal - Botão 8",  handler:"Botao8"],
  [label:"Universal - Botão 9",  handler:"Botao9"],
  [label:"Universal - Botão 10", handler:"Botao10"],
  [label:"Universal - Botão 11", handler:"Botao11"],
  [label:"Universal - Botão 12", handler:"Botao12"],
  [label:"Universal - Botão 13", handler:"Botao13"],
  [label:"Universal - Botão 14", handler:"Botao14"],
  [label:"Universal - Botão 15", handler:"Botao15"],
  [label:"Universal - Botão 16", handler:"Botao16"],
  [label:"Universal - Botão 17", handler:"Botao17"],
  [label:"Universal - Botão 18", handler:"Botao18"],
  [label:"Universal - Botão 19", handler:"Botao19"],
  [label:"Universal - Botão 20", handler:"Botao20"]
]

// Alias para o comando pedido ("Create Childs")
def CreateChilds() { recreateButtons() }
// Comando utilitário para recriar via UI/Console
def recreateButtons() { createOrUpdateChildButtons(true) }

private void createOrUpdateChildButtons(Boolean removeExtras=false) {
  try { if (logEnable) log.debug "Criando/atualizando Child Switches (Universal)..." } catch (ignored) { }

  List<Map> defs = UNIV_CHILD_BUTTON_DEFS
  Set<String> keep = [] as Set
  defs.eachWithIndex { m, idx ->
    String dni = "${device.id}-UNIVBTN-${idx+1}"
    def child = getChildDevice(dni)
    String label = m.label as String
    if (!child) {
      try {
        child = addChildDevice("hubitat", "Generic Component Switch", dni,
          [name: label, label: label, isComponent: true])
        if (logEnable) log.debug "Child criado: ${label} (${dni})"
      } catch (e) {
        log.warn "Falha ao criar child '${label}': ${e.message}"
      }
    } else {
      try { if (child.label != label) child.setLabel(label) } catch (ignored) { }
    }
    if (child) {
      try {
        child.updateDataValue("handler", (m.handler as String))
        child.parse([[name:"switch", value:"off"]]) // estado inicial
      } catch (ignored) { }
      keep << dni
    }
  }

  // Remove filhos não previstos quando removeExtras = true
  if (removeExtras) {
    childDevices?.findAll { !(it.deviceNetworkId in keep) }?.each {
      try {
        deleteChildDevice(it.deviceNetworkId)
      } catch (ignored) { }
    }
  }
}

// Callbacks do Generic Component Switch
def componentOn(cd)  { handleChildPress(cd) }
def componentOff(cd) { /* momentary: ignorar o off manual */ }

private void handleChildPress(cd) {
  String handler = (cd?.getDataValue("handler") ?: "").trim()
  if (!handler) {
    log.warn "Child ${cd?.displayName} sem handler definido."
    return
  }
  try {
    this."${handler}"()
  } catch (MissingMethodException e) {
    log.warn "Método '${handler}' não encontrado. Verifique nomes dos handlers."
  } catch (e) {
    log.warn "Falha ao executar handler '${handler}': ${e.message}"
  }
  // auto-off em 1s para comportamento momentâneo
  runIn(1, "childOffSafe", [data:[dni: cd?.deviceNetworkId]])
}

def childOffSafe(data) {
  def child = getChildDevice(data?.dni as String)
  if (child) {
    try { child.parse([[name:"switch", value:"off"]]) } catch (ignored) { }
  }
}


/* ===== Remover todos os Child Switches criados para os botões da TV ===== */
def removeButtons() {
  try { if (logEnable) log.warn "Removendo todos os Child Switches de botões..." } catch (ignored) { }
  def toRemove = childDevices?.findAll { (it.deviceNetworkId ?: "").startsWith("${device.id}-UNIVBTN-") } ?: []
  Integer removed = 0
  toRemove.each { cd ->
    try {
      deleteChildDevice(cd.deviceNetworkId)
      removed++
    } catch (e) {
      log.warn "Falha ao remover child '${cd.displayName}': ${e.message}"
    }
  }
  if (logEnable) log.warn "Remoção concluída. Total removido: ${removed}"
}
