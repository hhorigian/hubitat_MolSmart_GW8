# hubitat_MolSmart_GW8
Driver for MolSmart GW8 IR/RF Gateway

## TecnoSimples — atualização / update

### Português

Este driver é mantido pela **TecnoSimples** a partir do código original de Vartan Horigian (VH / TRATO), sob a licença Apache 2.0. O nome, o namespace `TRATO` e os IDs do pacote são os mesmos de sempre: quem instalou por aqui recebe a versão nova pelo **HPM → Update**, sem trocar o Type de nenhum device.

**Como atualizar**
1. Atualize o próprio HPM antes (Hubitat Package Manager 1.9.16 ou mais novo).
2. HPM → **Update** → marque este pacote → aguarde a página dizer "complete" antes de sair dela.
3. Cortinas RF: abra cada instância do app **GW8 Remote Importer - Cortina RF** e clique **Done**. O DNI das cortinas migra (deixa de ter o IP) e o quadro "DNIs" mostra os conflitos, se houver. Não remova o app: isso apaga as cortinas que ele criou.

**Como saber de onde veio o driver:** o atributo `driverVersion` do device mostra `TS-3.0.0-VH` quando o código foi baixado deste repositório (`TS-3.0.0`, sem `-VH`, é o mesmo código pelo repositório da TecnoSimples).

**Atenção**
- **Agora é um pacote só:** o pacote `MolSmart - GW8 - AC (learning)` traz o RF e o app como componentes opcionais. **Quem já tem o pacote `MolSmart GW8-RF` não deve marcar o RF no pacote do AC**: o HPM instala uma segunda cópia do driver e do app (medido em bancada). Continue atualizando o RF pelo pacote `MolSmart GW8-RF`, que aponta para os mesmos arquivos novos.
- Instalação nova: instale o pacote do AC e, se usa cortinas, marque o RF e o app nele (no Install ou depois, em Modify).
- O AC anuncia `auto`, `cool` e `off` a Alexa e Google (`heat`, `dry` e `fan` deixam de ser anunciados).
- Cortinas: o `push 4` (PROG do motor) deixa de ser enviado; regras que o usavam deixam de agir. Senha errada aparece em `lastHttpResult`.

### English

This driver is maintained by **TecnoSimples**, based on the original code by Vartan Horigian (VH / TRATO), under the Apache 2.0 license. The name, the `TRATO` namespace and the package IDs are unchanged: if you installed from here, you get the new version through **HPM → Update**, without changing any device's Type.

**How to update**
1. Update HPM itself first (Hubitat Package Manager 1.9.16 or newer).
2. HPM → **Update** → tick this package → wait for the page to say "complete" before leaving it.
3. RF blinds: open each instance of the **GW8 Remote Importer - Cortina RF** app and click **Done**. The blinds' DNI migrates (it no longer contains the IP) and the "DNIs" panel shows any conflicts. Do not remove the app: that deletes the blinds it created.

**Where did the driver come from:** the device's `driverVersion` attribute shows `TS-3.0.0-VH` when the code was downloaded from this repository (`TS-3.0.0`, without `-VH`, is the same code from the TecnoSimples repository).

**Heads-up**
- **It is now a single package:** `MolSmart - GW8 - AC (learning)` carries RF and the app as optional components. **If you already have the `MolSmart GW8-RF` package, do not tick RF in the AC package**: HPM installs a second copy of the driver and the app (measured on the bench). Keep updating RF through the `MolSmart GW8-RF` package, which points to the same new files.
- New install: install the AC package and, if you use blinds, tick RF and the app in it (at Install or later, via Modify).
- The AC announces `auto`, `cool` and `off` to Alexa and Google (`heat`, `dry` and `fan` are no longer announced).
- Blinds: `push 4` (the motor's PROG) is no longer sent; rules that used it stop acting. A wrong password shows in `lastHttpResult`.
