/**
 *   MolSmart GW8 Driver - Versão Usando IR MolSmart Database. Versão para controles de TV. 
 *   You must create your remote control template, at http://ir.molsmart.com.br. Then you can import your remote control over by using just the sharing URL. 
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
 *            --- Driver para GW8 - IR - para TV ---
 *              V.1.0   11/11/2025 - V1 p
*
 *
 *
 */
metadata {
  definition (name: "MolSmart - GW8 - TV (irweb)", namespace: "TRATO", author: "VH", vid: "generic-contact") {
    	capability "TV"  
    	capability "SamsungTV"
        capability "Switch"  
        capability "Actuator"
        capability "PushableButton"
        capability "Variable"      
  
        attribute "channel", "number"
        attribute "volume", "number"
        attribute "movieMode", "string"
        attribute "power", "string"
        attribute "sound", "string"
        attribute "picture", "string"  
    	attribute "Controle", "string"  
    	attribute "TipoControle", "string" 
    	attribute "Formato", "string"       
      
command "GetRemoteDATA"
command "cleanvars"  
command "poweroff"
command "poweron"
command "mute"
command "source"
command "back"
command "menu"
command "hdmi"
command "hdmi"
command "left"
command "right"
command "up"
command "down"
command "confirm"
command "exit"
command "home"
command "channelUp"
command "channelDown"
command "volumeUp"
command "volumeDown"
command "num0"
command "num1"
command "num2"
command "num3"
command "num4"
command "num5"
command "num6"
command "num7"
command "num8"
command "num9" 
command "btnextra"
command "btnextra"
command "btnextra"
command "appAmazonPrime"
command "appYouTube"
command "appNetflix"
command "btnextra"
command "btnextra5"
command "btnextra6"
command "btnextra7"
command "btnAIRsend"
command "btnBIRsend"
command "btnCIRsend"
command "btnDIRsend"
command "playIRsend"
command "pauseIRsend"
command "nextIRsend"
command "guideIRsend"
command "infoIRsend" 
command "toolsIRsend" 
command "smarthubIRsend" 
command "previouschannelIRsend" 
command "backIRsend"	  
command "recreateButtons"
		  
  }
      
      


}

    import groovy.transform.Field
    @Field static final String DRIVER = "by TRATO"
    @Field static final String USER_GUIDE = "https://github.com/hhorigian/hubitat_MolSmart_GW8_IR/tree/main/TV"

    String fmtHelpInfo(String str) {
    String prefLink = "<a href='${USER_GUIDE}' target='_blank'>${str}<br><div style='font-size: 70%;'>${DRIVER}</div></a>"
    return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink}</div>"
    }


    @Field static final String DRIVER1 = "IR MolSmart"
    @Field static final String USER_GUIDE1 = "https://ir.molsmart.com.br/"

    String fmtHelpInfo1(String str) {
    String prefLink1 = "<a href='${USER_GUIDE1}' target='_blank'>${str}<br><div style='font-size: 70%;'>${DRIVER1}</div></a>"
    return "<div style='font-size: 160%; font-style: bold; padding: 2px 0px; text-align: center;'>${prefLink1}</div>"
    }



  preferences {
    	input name: "logEnable", type: "bool", title: "Enable debug logging", defaultValue: false
        input name: "molIPAddress", type: "text", title: "MolSmart GW8 IP Address", submitOnChange: true, required: true, defaultValue: "192.168.1.100" 
    	input name: "user", title:"Usuário", type: "string", required: true, defaultValue: "admin" 
	    input name: "password", title:"Senha", type: "string", required: true, defaultValue: "12345678" 
	    input name: "channel", title:"Canal Infravermelho (1-8). O Blaster é o 1", type: "string", required: true , defaultValue: "1"        
        input name: "repeatSendHEX", title:"Repeat for SendHex", type: "string", defaultValue: "1"   // REPEAT SEND PRONTO HEX
        //help guide
        input name: "UserGuide", type: "hidden", title: fmtHelpInfo("Manual do Driver") 
        input name: "SiteIR", type: "hidden", title: fmtHelpInfo1("Site IR MolSmart") 
        input name: "webserviceurl", title:"URL Do Controle Remoto", type: "string"
        input name: "enableHealthCheck", type: "bool",   title: "Ativar verificação de online (HTTP /info)", defaultValue: true
        input name: "healthCheckMins",   type: "number", title: "Intervalo do health check (min)", defaultValue: 30, range: "1..1440"
        input name: "createButtonsOnSave", type: "bool", title: "Criar/atualizar Child Switches para botões ao salvar", defaultValue: true

  }   
  

def initialized()
{
    state.currentip = ""  
    log.debug "initialized()"
    
}


def GetRemoteDATA()
{
  
    def params = [
        uri: webserviceurl,
        contentType: "application/json"
    ]
    try {
        httpGet(params) { resp ->
            if (resp.success) {                
                sendEvent(name: "GetRemoteData", value: "Sucess")
                //log.debug "RESULT = " + resp.data
      
    sendEvent(name: "Controle", value: resp.data.name)   
    sendEvent(name: "TipoControle", value: resp.data.type)   
    sendEvent(name: "Formato", value: resp.data.conversor)                   
                
    state.encoding = resp.data.conversor
    state.OFFIRsend  = resp.data.functions.function[0]
    state.OnIRsend  = resp.data.functions.function[1]
    state.muteIRsend  = resp.data.functions.function[2]             
    state.sourceIRsend  = resp.data.functions.function[3]     
    state.backIRsend  = resp.data.functions.function[4]     
    state.menuIRsend  = resp.data.functions.function[5]     
    state.hdmi1IRsend  = resp.data.functions.function[6]     
    state.hdmi2IRsend  = resp.data.functions.function[7]     
    state.leftIRsend  = resp.data.functions.function[8]     
    state.rightIRsend  = resp.data.functions.function[9]     
    state.upIRsend  = resp.data.functions.function[10]     
    state.downIRsend  = resp.data.functions.function[11]     
    state.confirmIRsend  = resp.data.functions.function[12]     
    state.exitIRsend  = resp.data.functions.function[13]     
    state.homeIRsend  = resp.data.functions.function[14]                 
    state.ChanUpIRsend  = resp.data.functions.function[15]
    state.ChanDownIRsend  = resp.data.functions.function[16]     
    state.VolUpIRsend  = resp.data.functions.function[17]     
    state.VolDownIRsend  = resp.data.functions.function[18]     
    state.num0IRsend  = resp.data.functions.function[19]     
    state.num1IRsend  = resp.data.functions.function[20]     
    state.num2IRsend  = resp.data.functions.function[21]     
    state.num3IRsend  = resp.data.functions.function[22]     
    state.num4IRsend  = resp.data.functions.function[23]     
    state.num5IRsend  = resp.data.functions.function[24]     
    state.num6IRsend  = resp.data.functions.function[25]     
    state.num7IRsend  = resp.data.functions.function[26]     
    state.num8IRsend  = resp.data.functions.function[27]     
    state.num9IRsend  = resp.data.functions.function[28]     
    state.btnextra1IRsend  = resp.data.functions.function[29]     
    state.btnextra2IRsend  = resp.data.functions.function[30]     
    state.btnextra3IRsend  = resp.data.functions.function[31]     
    state.amazonIRsend  = resp.data.functions.function[32]     
    state.youtubeIRsend  = resp.data.functions.function[33]     
    state.netflixIRsend  = resp.data.functions.function[34]     
    state.btnextra4IRsend  = resp.data.functions.function[35]     
    state.btnextra5IRsend  = resp.data.functions.function[36]     
    state.btnextra6IRsend  = resp.data.functions.function[37]     
    state.btnextra7IRsend  = resp.data.functions.function[38]   
    state.btnAIRsend  = resp.data.functions.function[39] 
    state.btnBIRsend  = resp.data.functions.function[40] 
    state.btnCIRsend  = resp.data.functions.function[41] 
    state.btnDIRsend  = resp.data.functions.function[42] 
    state.playIRsend  = resp.data.functions.function[43] 
    state.pauseIRsend  = resp.data.functions.function[44] 
    state.nextIRsend  = resp.data.functions.function[45] 
    state.guideIRsend  = resp.data.functions.function[46]                    
    state.infoIRsend   = resp.data.functions.function[47] 
    state.toolsIRsend  = resp.data.functions.function[48] 
    state.smarthubIRsend  = resp.data.functions.function[49] 
    state.previouschannelIRsend  = resp.data.functions.function[50] 
    state.backIRsend  = resp.data.functions.function[51]
             
    }
            
	}
    } catch (Exception e) {
        log.warn "Get Remote Control Info failed: ${e.message}"
    }    

}

def cleanvars()  //Usada para limpar todos os states e controles aprendidos. 
{
//state.remove()
  state.clear() 
  AtualizaDadosGW3()  
}

def installed()
{   
    log.debug "installed()"
}

def updated()
{ 
        if (!device.currentValue("gw3Online")) sendEvent(name:"gw3Online", value:"unknown")
    sendEvent(name:"numberOfButtons", value:52)    
    log.debug "updated()"
    AtualizaDadosGW3()  
	if (logEnable) runIn(1800,logsOff)
    

    if (createButtonsOnSave) createOrUpdateChildButtons(true)
    if (enableHealthCheck) scheduleHealth()
}

//Get Device info and set as state to use during driver.
def AtualizaDadosGW3() {
    state.currentip = settings.molIPAddress
    state.serialNum = settings.serialNum
    state.verifyCode = settings.verifyCode
    state.channel = settings.channel
    log.info "Dados do GW8 atualizados: " + state.currentip + " -- " + " -- " + state.channel 

}


//Basico on / off para Switch 
def on() {
     sendEvent(name: "switch", value: "on", isStateChange: true)
     def ircode =  state.OnIRsend   
     log.info "ircode = " + ircode
     EnviaComando(ircode)

}

def off() {
     sendEvent(name: "switch", value: "off", isStateChange: true)
     def ircode =  state.OFFIRsend    
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
        case 1 : poweron(); break
        case 2 : mute(); break
        case 3 : source(); break
        case 4 : back(); break
        case 5 : menu(); break
        case 6 : hdmi1(); break
        case 7 : hdmi2(); break                
        case 8 : left(); break
        case 9 : right(); break
        case 10: up(); break
        case 11: down(); break
        case 12: confirm(); break
        case 13: exit(); break
        case 14: home(); break
        case 18: channelUp(); break
        case 19: channelDown(); break
        case 21: volumeUp(); break
        case 22: volumeDown(); break
        case 23: num0(); break
        case 24: num1(); break
        case 25: num2(); break
        case 26: num3(); break
        case 27: num4(); break        
        case 28: num5(); break
        case 29: num6(); break
        case 30: num7(); break
        case 31: num8(); break            
        case 32: num9(); break   
        case 33: btnextra1(); break                
        case 34: btnextra2(); break
        case 35: btnextra3(); break
        case 38: appAmazonPrime(); break
        case 39: appYouTube(); break
        case 40: appNetflix(); break    
        case 41: btnextra4(); break    
        case 42: btnextra5(); break    
        case 43: btnextra6(); break    
        case 44: btnextra7(); break    
        case 45: btnAIRsend(); break    
        case 46: btnBIRsend(); break    
        case 47: btnCIRsend(); break    
        case 48: btnDIRsend(); break    
        case 49: playIRsend(); break    
        case 50: pauseIRsend(); break    
        case 51: nextIRsend(); break    
        case 52: guideIRsend(); break            
        case 53: infoIRsend(); break 
        case 54: toolsIRsend(); break 
        case 55: smarthubIRsend(); break 
        case 56: previouschannelIRsend(); break 
        case 57: backIRsend(); break  
        case 58: poweroff(); break
		
         
        default:
		logDebug("push: Botão inválido.")
		break
	}
}

//Botão #0 para dashboard
def poweroff(){
	sendEvent(name: "power", value: "off")
    def ircode =  state.OFFIRsend
    EnviaComando(ircode)    
}

//Botão #1 para dashboard
def poweron(){
	sendEvent(name: "power", value: "on")
    def ircode =  state.OnIRsend
    EnviaComando(ircode)    
}

//Botão #2 para dashboard
def mute(){
	sendEvent(name: "action", value: "mute")
    def ircode =  state.muteIRsend
    EnviaComando(ircode)    
}


//Botão #3 para dashboard
def source(){
	sendEvent(name: "action", value: "source")
    def ircode =  state.sourceIRsend
    EnviaComando(ircode)    
}

//Botão #4 para dashboard
def back(){
	sendEvent(name: "action", value: "back")
    def ircode = state.backIRsend
    EnviaComando(ircode)    
}

//Botão #5 para dashboard
def menu(){
	sendEvent(name: "action", value: "menu")
    def ircode =  state.menuIRsend
    EnviaComando(ircode)    
}


//Botão #6 para dashboard
def hdmi1(){
    sendEvent(name: "input", value: "hdmi1")
    def ircode =   state.hdmi1IRsend
    EnviaComando(ircode)
}

//Botão #7 para dashboard
def hdmi2(){
    sendEvent(name: "input", value: "hdmi2")
    def ircode =  state.hdmi2IRsend
    EnviaComando(ircode)
}



//Botão #8 para dashboard
def left(){
    sendEvent(name: "action", value: "left")
    def ircode =   state.leftIRsend
    EnviaComando(ircode)
}

//Botão #9 para dashboard
def right(){
    sendEvent(name: "action", value: "right")
     def ircode =  state.rightIRsend
    EnviaComando(ircode)
}



//Botão #10 para dashboard
def up(){
    sendEvent(name: "action", value: "up")
    def ircode =  state.upIRsend
    EnviaComando(ircode)
}

//Botão #11 para dashboard
def down(){
    sendEvent(name: "action", value: "down")
    def ircode =  state.downIRsend
    EnviaComando(ircode)
}

//Botão #12 para dashboard
def confirm(){
    sendEvent(name: "action", value: "confirm")
    def ircode =  state.confirmIRsend
    EnviaComando(ircode)
}


//Botão #13 para dashboard
def exit(){
	sendEvent(name: "action", value: "exit")
    def ircode =  state.exitIRsend
    EnviaComando(ircode)    
}



//Botão #14 para dashboard
def home(){
    sendEvent(name: "action", value: "home")
    def ircode =  state.homeIRsend
    EnviaComando(ircode)
}



//Botão #18 para dashboard
def channelUp(){
	sendEvent(name: "channel", value: "chup")
   def ircode =   state.ChanUpIRsend
    EnviaComando(ircode)    
}

//Botão #19 para dashboard
def channelDown(){
	sendEvent(name: "channel", value: "chdown")
    def ircode =  state.ChanDownIRsend
    EnviaComando(ircode)    
}

//Botão #21 para dashboard
def volumeUp(){
	sendEvent(name: "action", value: "volup")
    def ircode = state.VolUpIRsend
    EnviaComando(ircode)    
}

//Botão #22 para dashboard
def volumeDown(){
	sendEvent(name: "action", value: "voldown")
    def ircode = state.VolDownIRsend
    EnviaComando(ircode)    
}


//Botão #23 para dashboard
def num0(){
    sendEvent(name: "action", value: "num0")
    def ircode =  state.num0IRsend
    EnviaComando(ircode)
}

//Botão #24 para dashboard
def num1(){
    sendEvent(name: "action", value: "num1")
   def ircode =  state.num1IRsend
    EnviaComando(ircode)
}

//Botão #25 para dashboard
def num2(){
    sendEvent(name: "action", value: "num2")
    def ircode =  state.num2IRsend
    EnviaComando(ircode)
}


//Botão #26 para dashboard
def num3(){
    sendEvent(name: "action", value: "num3")
    def ircode =  state.num3IRsend
    EnviaComando(ircode)
}

//Botão #27 para dashboard
def num4(){
    sendEvent(name: "action", value: "num4")
    def ircode =  state.num4IRsend
    EnviaComando(ircode)
}

//Botão #28 para dashboard
def num5(){
    sendEvent(name: "action", value: "num5")
    def ircode =   state.num5IRsend
    EnviaComando(ircode)
}

//Botão #29 para dashboard
def num6(){
    sendEvent(name: "action", value: "num6")
    def ircode =  state.num6IRsend
    EnviaComando(ircode)
}


//Botão #30 para dashboard
def num7(){
    sendEvent(name: "action", value: "num7")
    def ircode =  state.num7IRsend
    EnviaComando(ircode)
}

//Botão #31 para dashboard
def num8(){
    sendEvent(name: "action", value: "num8")
    def ircode =  state.num8IRsend
    EnviaComando(ircode)
}

//Botão #32 para dashboard
def num9(){
    sendEvent(name: "action", value: "num9")
    def ircode = state.num9IRsend
    EnviaComando(ircode)
}

//Botão #33 para dashboard
def btnextra1(){
    sendEvent(name: "action", value: "confirm")
    def ircode =  state.btnextra1IRsend
    EnviaComando(ircode)
}

//Botão #34 para dashboard
def btnextra2(){
    sendEvent(name: "action", value: "btnextra2")
    def ircode =  state.btnextra2IRsend
    EnviaComando(ircode)
}

//Botão #35 para dashboard
def btnextra3(){
    sendEvent(name: "action", value: "btnextra3")
    def ircode =  state.btnextra3IRsend
    EnviaComando(ircode)
}

//Botão #38 para dashboard
def appAmazonPrime(){
    sendEvent(name: "input", value: "amazon")
    def ircode =   state.amazonIRsend
    EnviaComando(ircode)
}

//Botão #39 para dashboard
def appyoutube(){
    sendEvent(name: "input", value: "youtube")
   def ircode =  state.youtubeIRsend
    EnviaComando(ircode)
}


//Botão #40 para dashboard
def appnetflix(){
    sendEvent(name: "input", value: "netflix")
    def ircode =  state.netflixIRsend
    EnviaComando(ircode)
}

//Botão #41 para dashboard
def btnextra4(){
    sendEvent(name: "action", value: "btnextra4")
    def ircode =  state.btnextra4IRsend
    EnviaComando(ircode)
}

//Botão #40 para dashboard
def btnextra5(){
    sendEvent(name: "action", value: "btnextra5")
    def ircode =  state.btnextra5IRsend
    EnviaComando(ircode)
}


//Botão #40 para dashboard
def btnextra6(){
    sendEvent(name: "action", value: "btnextra6")
    def ircode =  state.btnextra6IRsend
    EnviaComando(ircode)
}

//Botão #44 para dashboard
def btnextra7(){
    sendEvent(name: "action", value: "btnextra7")
    def ircode =  state.btnextra7IRsend
    EnviaComando(ircode)
}

//Botão #45 para dashboard
def btnAIRsend(){
    sendEvent(name: "action", value: "btnAIRsend")
    def ircode =  state.btnAIRsend
    EnviaComando(ircode)
}

//Botão #46 para dashboard
def btnBIRsend(){
    sendEvent(name: "action", value: "btnBIRsend")
    def ircode =  state.btnBIRsend
    EnviaComando(ircode)
}

//Botão #47 para dashboard
def btnCIRsend(){
    sendEvent(name: "action", value: "btnCIRsend")
    def ircode =  state.btnCIRsend
    EnviaComando(ircode)
}

//Botão #48 para dashboard
def btnDIRsend(){
    sendEvent(name: "action", value: "btnDIRsend")
    def ircode =  state.btnDIRsend
    EnviaComando(ircode)
}

//Botão #49 para dashboard
def playIRsend(){
    sendEvent(name: "action", value: "playIRsend")
    def ircode =  state.playIRsend
    EnviaComando(ircode)
}

//Botão #50 para dashboard
def pauseIRsend(){
    sendEvent(name: "action", value: "pauseIRsend")
    def ircode =  state.pauseIRsend
    EnviaComando(ircode)
}

//Botão #51 para dashboard
def nextIRsend(){
    sendEvent(name: "action", value: "nextIRsend")
    def ircode =  state.nextIRsend
    EnviaComando(ircode)
}

//Botão #52 para dashboard
def guideIRsend(){
    sendEvent(name: "action", value: "guideIRsend")
    def ircode =  state.guideIRsend
    EnviaComando(ircode)
}

//Botão #53 para dashboard
def infoIRsend(){
    sendEvent(name: "action", value: "infoIRsend")
    def ircode =  state.infoIRsend
    EnviaComando(ircode)
}

//Botão #54 para dashboard
def toolsIRsend(){
    sendEvent(name: "action", value: "toolsIRsend")
    def ircode =  state.toolsIRsend
    EnviaComando(ircode)
}

//Botão #55 para dashboard
def smarthubIRsend(){
    sendEvent(name: "action", value: "smarthubIRsend")
    def ircode =  state.smarthubIRsend
    EnviaComando(ircode)
}

//Botão #56 para dashboard
def previouschannelIRsend(){
    sendEvent(name: "action", value: "previouschannelIRsend")
    def ircode =  state.previouschannelIRsend
    EnviaComando(ircode)
}

//Botão #57 para dashboard
def backIRsend(){
    sendEvent(name: "action", value: "backIRsend")
    def ircode =  state.backIRsend
    EnviaComando(ircode)
}

      

private String buildFullUrl(button) {
    def ip   = settings.molIPAddress
    def sn   = settings.user
    def vc   = settings.password
    def cid  = settings.cId
    def rcid = (settings.rcId ?: "61")
    def ch = state.channel
    def repeat = settings.repeatSendHEX 

    if (state.encoding == "sendir") {   //if the remote is SendIR(Global Cache) uses one URL, if it's HEX format, uses another URL.         
        return "http://${ip}/control?user=${sn}&pwd=${vc}&gc=${button}&c=${ch}"	        
    } else {
        return "http://${ip}/control?user=${sn}&pwd=${vc}&pronto=${button}&c=${ch}&r=${repeat}"	        
    }             
}


def EnviaComando(button) {
	
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


/* ======================= HEALTH CHECK (HTTP /info) ======================= */

def scheduleHealth() {
  Integer mins = Math.max(1, (healthCheckMins ?: 5) as int)
  unschedule("healthPoll")
  runIn(2, "healthPoll")                 // dispara logo
  runEveryXMinutes(mins, "healthPoll")   // agenda recorrente
}

def healthCheckNow() { healthPoll() }

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

  if (st && st >= 200 && st <= 299 && (body?.toString()?.contains("MolSmart Device Info") || body?.toString()?.contains("Version:"))) {
    if (device.currentValue("gw3Online") != "online") sendEvent(name:"gw3Online", value:"online", isStateChange:true)
    sendEvent(name:"healthLatencyMs", value: dt as Long)
    sendEvent(name:"lastHealthAt", value: stamp)

    // Extrair "Version: X" e publicar 6 chars em gw3Version
    try {
      String txt = body?.toString() ?: ""
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
    if (device.currentValue("gw3Online") != "offline") sendEvent(name:"gw3Online", value:"offline", isStateChange:true)
    sendEvent(name:"healthLatencyMs", value: null)
    sendEvent(name:"lastHealthAt", value: stamp)
    if (logEnable) log.warn "Health FAIL (status=${st})"
  }
}


/* ======================= CHILD SWITCHES (Botões como Switch momentâneo) ======================= */

import groovy.transform.Field

@Field static final List<Map> TV_CHILD_BUTTON_DEFS = [
  [label:"TV - Power On",            handler:"poweron"],
  [label:"TV - Power Off",           handler:"poweroff"],
  [label:"TV - Mute",                handler:"mute"],
  [label:"TV - Source",              handler:"source"],
  [label:"TV - Back",                handler:"back"],
  [label:"TV - Menu",                handler:"menu"],
  [label:"TV - HDMI 1",              handler:"hdmi1"],
  [label:"TV - HDMI 2",              handler:"hdmi2"],
  [label:"TV - Left",                handler:"left"],
  [label:"TV - Right",               handler:"right"],
  [label:"TV - Up",                  handler:"up"],
  [label:"TV - Down",                handler:"down"],
  [label:"TV - OK/Confirm",          handler:"confirm"],
  [label:"TV - Exit",                handler:"exit"],
  [label:"TV - Home",                handler:"home"],
  [label:"TV - Channel Up",          handler:"channelUp"],
  [label:"TV - Channel Down",        handler:"channelDown"],
  [label:"TV - Volume Up",           handler:"volumeUp"],
  [label:"TV - Volume Down",         handler:"volumeDown"],
  [label:"TV - 0",                   handler:"num0"],
  [label:"TV - 1",                   handler:"num1"],
  [label:"TV - 2",                   handler:"num2"],
  [label:"TV - 3",                   handler:"num3"],
  [label:"TV - 4",                   handler:"num4"],
  [label:"TV - 5",                   handler:"num5"],
  [label:"TV - 6",                   handler:"num6"],
  [label:"TV - 7",                   handler:"num7"],
  [label:"TV - 8",                   handler:"num8"],
  [label:"TV - 9",                   handler:"num9"],
  [label:"TV - Amazon Prime",        handler:"appAmazonPrime"],
  [label:"TV - YouTube",             handler:"appyoutube"],
  [label:"TV - Netflix",             handler:"appnetflix"],
  [label:"TV - Extra 1",             handler:"btnextra1"],
  [label:"TV - Extra 2",             handler:"btnextra2"],
  [label:"TV - Extra 3",             handler:"btnextra3"],
  [label:"TV - Extra 4",             handler:"btnextra4"],
  [label:"TV - Extra 5",             handler:"btnextra5"],
  [label:"TV - Extra 6",             handler:"btnextra6"],
  [label:"TV - Extra 7",             handler:"btnextra7"],
  [label:"TV - A IR",                handler:"btnAIRsend"],
  [label:"TV - B IR",                handler:"btnBIRsend"],
  [label:"TV - C IR",                handler:"btnCIRsend"],
  [label:"TV - D IR",                handler:"btnDIRsend"],
  [label:"TV - Play",                handler:"playIRsend"],
  [label:"TV - Pause",               handler:"pauseIRsend"],
  [label:"TV - Next",                handler:"nextIRsend"],
  [label:"TV - Guide",               handler:"guideIRsend"],
  [label:"TV - Info",                handler:"infoIRsend"],
  [label:"TV - Tools",               handler:"toolsIRsend"],
  [label:"TV - SmartHub",            handler:"smarthubIRsend"],
  [label:"TV - Previous Channel",    handler:"previouschannelIRsend"],
  [label:"TV - Back (IR)",           handler:"backIRsend"]
]

command "recreateButtons"

def recreateButtons() { createOrUpdateChildButtons(true) }


private void createOrUpdateChildButtons(Boolean removeExtras=false) {
  try { if (logEnable) log.debug "Criando/atualizando Child Switches para botões da TV..." } catch (ignored) { }

  // Sempre cria todos os childs definidos na lista (handlers inexistentes serão tratados ao acionar)
  List<Map> defs = TV_CHILD_BUTTON_DEFS
  if (logEnable) log.debug "Total de childs previstos: ${defs?.size()}"

  Set<String> keep = [] as Set
  defs.eachWithIndex { m, idx ->
    String dni = "${device.id}-TVBTN-${idx+1}"
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
        child.parse([[name:"switch", value:"off"]])
      } catch (ignored) { }
      keep << dni
    }
  }

  if (removeExtras) {
    childDevices?.findAll { !(it.deviceNetworkId in keep) }?.each {
      try { deleteChildDevice(it.deviceNetworkId) } catch (ignored) { }
    }
  }
}


// Component callbacks (Generic Component Switch)
def componentOn(cd)  { handleChildPress(cd) }
def componentOff(cd) { /* momentary: ignorar */ }

private void handleChildPress(cd) {
  String handler = cd?.getDataValue("handler") ?: ""
  if (!handler) { log.warn "Child ${cd?.displayName} sem handler definido."; return }
  try {
    this."${handler}"()
  } catch (MissingMethodException e) {
    log.warn "Método '${handler}' não encontrado. Verifique nomes dos handlers."
  } catch (e) {
    log.warn "Falha ao executar handler '${handler}': ${e.message}"
  }
  runIn(1, "childOffSafe", [data:[dni: cd?.deviceNetworkId]])
}

def childOffSafe(data) {
  def child = getChildDevice(data?.dni as String)
  if (child) {
    try { child.parse([[name:"switch", value:"off"]]) } catch (ignored) { }
  }
}