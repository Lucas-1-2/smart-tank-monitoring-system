TANK MONITORING SUBSYSTEM: ###TMS###
    -scheda ESP32
    Funzione principale:
        -fornisce al CUS (controller principale), informazioni riguardo alla cisterna
        -preleva informazioni tramite sensore sonar
        -utilizzo sonar
        -comunica con CUS con protocollo MQTT
    Comunicazioni possibili:
        -Misurazione continua livello idrico ad una frequenza F
        -Mostrare lo stato della connessione con CUS:
            .Accensione led verde: comunicazione funzionante
            .Accensiione led rosso: comunicazione non funzinante, problemi invio o di rete...
    Logica:
        -logica interna programmata tramite una o più macchine a stati finiti
        -primo scheduler taks based:
            .task monitoraggio idrico e frequenzaS
            .task gestione connessione e invio dati (con annessa gestione led)

WATER CHANNEL SUBSYSTEM: ###WCS###
    -scheda Arduino Uno
    Funzione principale:
        -gestisce l'apertura della valvola tramite i messaggi che passato da TMS->CUS->WCS
        -in modalità AUTOMATICA attende messaggi dalla cus prima di agire
        -in modalità MANUALE apre la valvola in base alla posizione di un potenziometro
            .il cambio da MANUALE a AUTOMATICO avviene grazie ad un PULSANTE flag...(si può pensare di farlo lavorare in fronte di discesa,salita...)
            .la valvola è rappresentata da un servo motore i messaggi inviati dal cus sono percentuali
            0% - 0 gradi 50% - 45 gradi - 100% 90 gradi
        -display lcd: 
            .mostra la percentuale di apertura della valvola (gradi del motore in percentuale)
            .mostra lo stato attuale: AUTOMATIC, MANUAL, 
                UNCONNECTED: stato ricevuto dal CUS dopo T2 senza misure valide; valvola comandata al 5%.
        -Logica:
            task based con scheduler

CONTROL UNIT SUBSYSTEM: ###CUS##
    -progrmma back end su pc
    Funzione principale:
        -riceve informazioni dal TMS tramite MQTT
        -decide quando aprire la valvola dalla dashboard HTTP
            .comunicandolo poi al WCS tramite seriale
        -fornisce i dati raccolti da TMS e WCS a una dashboard in HTTP
    -AVVIO: modalità AUTOMATICA.
        .livello del TMS L<= a L1: valvola WCS allo 0% (chiusa)
        .livello del TMS L1 < L <= L2 senza mantenimento dello stato HIGH: conserva l'apertura precedente per T1, poi valvola al 50% (semiaperta)
        .livello del TMS > L2: valvola immediatamente aperta al 100%, rimane così aperta finche il livello non torna < L2
        .a L = L2 mantiene HIGH solo se era già attivo; raggiungere L2 dal basso non attiva l'apertura immediata al 100%.
        .dopo HIGH, scendere nella fascia L1 < L < L2 avvia T1: mantiene il 100% durante l'attesa, poi passa al 50%; tornare esattamente a L2 non riattiva HIGH.
        .un reset della politica automatica azzera anche il mantenimento di HIGH.
    -ASSENZA DI MISURE VALIDE DAL TMS (T2):
        .se non arriva una misura valida per almeno T2 (default 10000 ms), il CUS entra in UNCONNECTED.
        .il controllo avviene al tick periodico del CUS (default 100 ms); il timeout iniziale parte dall'avvio.
        .il CUS imposta il livello corrente a null, la valvola al 5% e azzera la politica automatica.
        .il 5% e' l'apertura minima fissa adottata dal software in assenza di misure valide.
        .il nuovo stato viene inviato al WCS tramite seriale ed esposto alla dashboard tramite HTTP.
        .i comandi di cambio modalita' e apertura sono rifiutati, anche se prima il sistema era in MANUAL.
        .NO_ECHO viene salvato nello storico come null e non rinnova T2; azzera inoltre l'attesa T1 in AUTOMATIC.
        .i messaggi MQTT malformati vengono scartati: non entrano nello storico e non rinnovano T2.
        .alla prima misura valida il sistema torna sempre in AUTOMATIC e applica la politica al livello ricevuto.
        .UNCONNECTED indica assenza di misure valide, quindi include anche NO_ECHO continui con MQTT funzionante.
        .il collegamento seriale via cavo e' assunto stabile: non sono previsti heartbeat o conferme di attuazione.
    -modalità MANUALE:
        viene controllata localmente nell WCS tramite un potenzionemtro
        controllata da remoto nel CUS tramite dashboard HTTP (Dashboard->CUS->WCS)
            -controllo implementato del potenziometro:
                all'ingresso in MANUAL la posizione fisica diventa il riferimento, senza inviare comandi.
                i movimenti successivi inviano richieste al CUS se la differenza dal riferimento è almeno di 5 punti percentuali.
                raggiungere 0% o 100% invia una richiesta anche con una differenza inferiore, purché il valore sia diverso dal riferimento.
                dopo l'invio il riferimento viene aggiornato: restare fermi su un estremo non genera richieste ripetute.
                il servo viene aggiornato soltanto dopo la ricezione dello stato dal CUS; non è implementata una rampa di movimento.
    -Linguaggio:
        si può usare programmazione oggetti java... non è necessaria interfaccia grafica poichè se ne occupa la dashboard

DASHBARD SUBSYSTEM: ###DBS###
    -applicazione web, interfaccia grafica
        comunica esclufsivamente con il CUS
        il CUS contiene le sue logiche di controllo...
    MOSTRA:
        un grafico che rappresenta le ultime N misurazioni del livello dell'acqua
        attuale perchentuale dell'apertura della valvola
        stato del sistema:
                AUTOMATIC; (Stato WCS)
                MANUAL; (Stato WCS)
                UNCONNECTED; (Stato comunicazione TMS->CUS)
                NOT AVAILABLE. (Stato comunicazione DBS -> CUS)
    COMANDI:
        pulsante virtuale per passare da MANUAL ad AUTOMATICA
        controllo grafico per reglare l'apertura della valvola quanto il sistema è in MANUAL
    LINGUAGGI: HTML + CSS + JavaScript (ES modules), con Chart.js per il grafico.
    COMUNICAZIONE: Fetch API verso le API HTTP/JSON del CUS Java; non viene utilizzato PHP.

Organizzazione Cartelle:
    assignment-03/
        ├── TMS/
        ├── CUS/
        ├── WCS/
        ├── DBS/
        └── doc/
            └── report.md con:  una descrizione ad alto livello del comportamento dei sottosistemi;
                                le macchine a stati finiti;
                                lo schema dei collegamenti o la rappresentazione breadboard;
                                il link a un breve video dimostrativo del sistema.

PUNTI NON SPECIFICATI NELLA CONSEGNA CHE BISOGNA DECIDERE:
    i valori di F, L₁, L₂, T₁, T₂ e N;
    gestione UNCONNECTED definita nel software: valvola al 5%, comandi rifiutati, ripristino in AUTOMATIC alla prima misura valida.
    chi abbia la precedenza in modalità manuale tra potenziometro e dashboard; ->per me varia: se la dashboard risulta unconnected passa il controllo al WCS
    come gestire due richieste contemporanee di cambio modalità; -> bisogna scegliere chi ha la pirorità...
    gestione L2 definita: HIGH entra solo sopra L2 e resta attivo fino a scendere sotto L2; nella fascia intermedia attende T1 mantenendo il 100%, poi passa al 50%.
    quali messaggi e formati usare per MQTT, seriale e HTTP;
    tecnologie definite: CUS Java 17 con Vert.x, Paho e jSerialComm; DBS HTML/CSS/JavaScript con Fetch API e Chart.js.


FONTI TROVATE:
    per questo ultimo esercizio per programmazione ESP:
        guardare es: 10
        PROTOCOLLO MQTT:
            guardare es: 11 e 12

    per programmazione java controller principale:
        guardare es: 11 e 12

    per programmazione dashboard il prof usa xml:
        guardare es: 11 e 12
    
    per proggrammazione asincroa con observer, quindi niente tick:
        guardare es: 9, altrimenti si fa con scheduler con vecchio esercizio es 8
                            





