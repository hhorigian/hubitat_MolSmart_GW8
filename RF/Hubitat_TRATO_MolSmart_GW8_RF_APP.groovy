/**
 *  GW8 Remote Importer (Cortina RF rcId 51)
 *  - Descobre controles no GW8 via POST /remoteData {"type":2}
 *  - Filtra rcId 51 (Door/Curtain RF)
 *  - Cria um child device por controle usando o driver de cortina existente
 *
 *  27.1.2026 - V1.0 - Beta. 
 */

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

definition(
    name: "GW8 Remote Importer - Cortina RF",
    namespace: "TRATO",
    author: "TRATO",
    description: "Importa controles RF (rcId 51) do MolSmart GW8 e cria devices de cortina para cada um. Ele vai usar os nomes já colocados no GW8.",
    category: "Convenience",
    iconUrl: "",
    iconX2Url: "",
    singleInstance: false
)

preferences {
    page(name: "pageMain")
    page(name: "pageDiscover")
    page(name: "pageCreate")
    page(name: "pageTools")
}

def pageMain() {
    dynamicPage(name: "pageMain", title: "GW8 Remote Importer (Cortina RF)", install: true, uninstall: true) {
        section("GW8 (usado para discovery)") {
            input "gw8ip", "text", title: "IP do GW8", required: true, submitOnChange: true
            input "gw8Port", "number", title: "Porta (default 80)", required: false, defaultValue: 80
        }

        section("Credenciais do GW8 (Opcionais. Default vazio)") {
            input "gw8User", "text", title: "Usuário", required: true, defaultValue: "admin", submitOnChange: true
            input "gw8Pass", "password", title: "Senha", required: true, defaultValue: "12345678", submitOnChange: true
        }

        section("Driver de Cortina (já existente)") {
            input "childNamespace", "text", title: "Namespace do Driver", required: true, defaultValue: "TRATO"
            input "childTypeName", "text", title: "Driver Name (TypeName) exatamente como no Hubitat", required: true, defaultValue: "MolSmart - GW8 - RF"
            paragraph "Dica: abra o driver da cortina em Drivers Code e copie o campo definition(name: \"...\") (Name)."
        }

        section("Ações") {
            href(name: "toDiscover", page: "pageDiscover", title: "1) Buscar controles no GW8", description: "Faz POST /remoteData e filtra rcId 51")
            href(name: "toCreate", page: "pageCreate", title: "2) Criar/Atualizar devices selecionados", description: "Cria um child device por controle e preenche cId/IP/login/senha")
            href(name: "toTools", page: "pageTools", title: "Ferramentas", description: "Limpar cache / Remover filhos")
        }

        section("Status") {
            def count = (state?.rfCurtains instanceof List) ? state.rfCurtains.size() : 0
            paragraph "Encontrados (rcId 51): ${count}"
            if (state?.lastDiscoverAt) paragraph "Última busca: ${state.lastDiscoverAt}"
            if (state?.lastError) paragraph "Último erro: ${state.lastError}"
            if (state?.lastCreateResult) paragraph "Último resultado criação:\n${state.lastCreateResult}"
        }
    }
}

def pageDiscover() {
    dynamicPage(name: "pageDiscover", title: "Buscar controles RF", install: false, uninstall: false) {
        section("Buscar agora") {
            if (!gw8ip) {
                paragraph "Defina o IP do GW8 na tela anterior."
                return
            }

            paragraph "Ao abrir esta página, vou buscar os controles no GW8…"

            discoverRemotes()

            def remotes = (state?.rfCurtains instanceof List) ? state.rfCurtains : []
            if (remotes) {
                paragraph "Controles encontrados (rcId 51): ${remotes.size()}"
                remotes.take(50).each { r ->
                    paragraph "CID ${r.id} — ${r.name}"
                }
                if (remotes.size() > 50) paragraph "(mostrando apenas os 50 primeiros)"
            } else {
                paragraph "Nenhum controle rcId 51 encontrado (ou aguardando resposta)."
            }
        }
    }
}

def pageCreate() {
    dynamicPage(name: "pageCreate", title: "Criar devices de cortina (seleção)", install: false, uninstall: false) {
        def remotes = (state?.rfCurtains instanceof List) ? state.rfCurtains : []

        section("Selecione as cortinas para criar/atualizar") {
            if (!remotes) {
                paragraph "Nenhum controle carregado. Vá em 'Buscar controles' primeiro."
                return
            }

            Map options = [:]
            remotes.each { r ->
                options["${r.id}"] = "CID ${r.id} — ${r.name}"
            }

            input "selectedCids", "enum",
                title: "Cortinas",
                required: false,
                multiple: true,
                options: options,
                submitOnChange: true

            input "forceUpdateData", "bool",
                title: "Forçar atualizar settings do child (IP/usuário/senha/cId) mesmo se já existe",
                defaultValue: true,
                submitOnChange: true
        }

        section("Criar agora") {
            input "btnCreateChildren", "button", title: "Criar/Atualizar devices selecionados"

            if (selectedCids) {
                paragraph "Selecionados: ${selectedCids.size()}"
            } else {
                paragraph "Selecione pelo menos uma cortina acima e clique no botão."
            }

            if (state?.lastCreateResult) {
                paragraph state.lastCreateResult
            }
        }

        section("Filhos existentes (criados por este App)") {
            def kids = getChildDevices()
            if (kids) {
                kids.each { cd ->
                    paragraph "${cd.displayName} (DNI: ${cd.deviceNetworkId}) | molIPAddress=${safeSetting(cd,'molIPAddress')} | cId=${safeSetting(cd,'cId')}"
                }
            } else {
                paragraph "Nenhum child device criado ainda."
            }
        }
    }
}

/** ✅ ALTERAÇÃO SOMENTE AQUI: Tools agora tem botões reais */
def pageTools() {
    dynamicPage(name: "pageTools", title: "Ferramentas", install: false, uninstall: false) {
        section("Ferramentas") {
            input "btnClearCache", "button", title: "Limpar lista encontrada (state.rfCurtains)"
            input "btnDeleteAllChildren", "button", title: "APAGAR TODOS os child devices criados por este App"

            if (state?.toolsResult) {
                paragraph state.toolsResult
            } else {
                paragraph "Use os botões acima para executar as ações."
            }
        }
    }
}

/** Handler do botão do Hubitat */
def appButtonHandler(String btn) {
    if (btn == "btnCreateChildren") {
        if (selectedCids) {
            state.lastCreateResult = createSelectedChildren(selectedCids as List<String>)
        } else {
            state.lastCreateResult = "Selecione pelo menos uma cortina antes de criar."
        }
        return
    }

    // ✅ NOVOS: Tools
    if (btn == "btnClearCache") {
        state.rfCurtains = []
        state.lastError = null
        state.toolsResult = "Lista encontrada foi limpa."
        return
    }

    if (btn == "btnDeleteAllChildren") {
        deleteAllChildren()
        state.toolsResult = "Child devices removidos."
        return
    }
}

def installed() { initialize() }
def updated() { initialize() }

def initialize() {
    // sem schedules por padrão
}

private String baseUrl() {
    def port = gw8Port ?: 80
    return "http://${gw8ip}:${port}"
}

private void discoverRemotes() {
    try {
        state.lastError = null

        def params = [
            uri: "${baseUrl()}/remoteData",
            requestContentType: "application/json",
            contentType: "application/json",
            body: JsonOutput.toJson([type: 2]),
            timeout: 10
        ]

        asynchttpPost("discoverRemotesCB", params)
        state.lastDiscoverAt = new Date().format("yyyy-MM-dd HH:mm:ss", location.timeZone)

    } catch (e) {
        state.lastError = "discoverRemotes() erro: ${e.message}"
        log.warn state.lastError
    }
}

def discoverRemotesCB(resp, data) {
    try {
        if (resp?.status != 200) {
            state.lastError = "GW8 HTTP ${resp?.status}"
            log.warn state.lastError
            return
        }

        def txt = resp?.data ?: "[]"
        def parsed = new JsonSlurper().parseText(txt)

        List<Map> all = []
        if (parsed instanceof List) {
            all = parsed.collect { r ->
                [ id: r?.id, name: (r?.name ?: ""), rcId: r?.rcId ]
            }.findAll { it.id != null }
        }

        def rfCurtains = all.findAll { it.rcId == 51 }
        rfCurtains.sort { a, b -> (a.name ?: "") <=> (b.name ?: "") }

        state.rfCurtains = rfCurtains

    } catch (e) {
        state.lastError = "discoverRemotesCB parse erro: ${e.message}"
        log.warn state.lastError
    }
}

private void applyCurtainDriverSettings(dev, String cidStr) {
    dev.updateSetting("molIPAddress", [value: "${gw8ip}", type: "text"])
    dev.updateSetting("user",        [value: "${gw8User}", type: "string"])
    dev.updateSetting("password",    [value: "${gw8Pass}", type: "string"])
    dev.updateSetting("cId",         [value: "${cidStr}",  type: "string"])
}

private String createSelectedChildren(List<String> cids) {
    if (!gw8ip) return "Erro: IP do GW8 não definido."
    if (!gw8User || !gw8Pass) return "Erro: Usuário/Senha não definido."
    if (!childNamespace || !childTypeName) return "Erro: Namespace/Driver Name não definido."

    def remotes = (state?.rfCurtains instanceof List) ? state.rfCurtains : []
    Map byCid = [:]
    remotes.each { r -> byCid["${r.id}"] = r }

    int created = 0
    int updated = 0
    int skipped = 0
    List<String> errors = []

    cids.each { cidStr ->
        def r = byCid[cidStr]
        if (!r) { skipped++; return }

        String dni = "GW8-${gw8ip}-CID-${cidStr}".toString()
        def existing = getChildDevice(dni)

        try {
            if (!existing) {
                def child = addChildDevice(
                    childNamespace,
                    childTypeName,
                    dni,
                    [label: (r.name ?: "GW8 Cortina ${cidStr}"), name: (r.name ?: "GW8 Cortina ${cidStr}"), isComponent: false]
                )

                applyCurtainDriverSettings(child, cidStr)

                child.updateDataValue("rcId", "51")
                child.updateDataValue("remoteName", "${r.name ?: ''}")

                created++
            } else {
                if (forceUpdateData != false) {
                    applyCurtainDriverSettings(existing, cidStr)

                    existing.updateDataValue("rcId", "51")
                    existing.updateDataValue("remoteName", "${r.name ?: ''}")

                    updated++
                } else {
                    skipped++
                }
            }
        } catch (ex) {
            errors << "CID ${cidStr}: ${ex.message}"
            log.warn "Erro criando/atualizando CID ${cidStr}: ${ex.message}"
        }
    }

    String msg = "Criados: ${created} | Atualizados: ${updated} | Ignorados: ${skipped}"
    if (errors) msg += "\nErros:\n- " + errors.join("\n- ")
    return msg
}

private String safeSetting(dev, String name) {
    try {
        def v = dev?.getSetting(name)
        return (v != null) ? "${v}" : ""
    } catch (e) {
        return ""
    }
}

private void deleteAllChildren() {
    getChildDevices()?.each { cd ->
        try {
            deleteChildDevice(cd.deviceNetworkId)
        } catch (e) {
            log.warn "Falha ao apagar child ${cd.deviceNetworkId}: ${e.message}"
        }
    }
}
