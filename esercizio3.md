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
                UNCONNECTED: riguarda la comunicazine TMS->CUS, se si interrompe decido cosa fare...
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
        .livello del TMS L1 < L <= L2 per più di tempo T1: valvola WCS al 50% (semiaperta)
        .livello del TMS > L2: valvola immediatamente aperta al 100%, rimane così aperta finche il livello non torna < L2
    -PERDITA OMUNICAZIONE TMS CON MQTT:
        .il sistema entra nello stato UNCONNECTED. -> invia il messaggio a WCS per lcd.... invia il messaggio alla Dashboard HTTP...
            #TODO: bisogna decidere cosa far fare nello stato UNCONNECTED E MOTIVARE LA SCELTA...
    -modalità MANUALE:
        viene controllata localmente nell WCS tramite un potenzionemtro
        controllata da remoto nel CUS tramite dashboard HTTP (Dashboard->CUS->WCS)
            -si aprono diverse strade per il controllo del motore:
                motore sincornizza il movimento con il potenziometro: potensiometro veloce -> motore veloce
                potenziometro sceglie la posizione -> il motore raggiunge la posizione con un suo tempo: si sceglie un tempo che non renda troppo brusca la chiusura o apertura che in un caso realistico, data la pressione dell'acuqa, potrebbe romperlo più facilmente
                potenziometro a soglia: ci sono 3 soglie 0%, 50%, 100%, tramite queste soglie si seguono le soglie dei gradi del motore:
                    questo potrebbe essere utile anche per prevenire eventuali letture sballate del potenziometro che rischiano di non far chiudere o aprire del tutto la porta
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
    LINGUAGGIO: html + php + css

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
    cosa debba fare esattamente la valvola nello stato UNCONNECTED; ->secondo me dovrebbe rimanere aperta la valolva
    chi abbia la precedenza in modalità manuale tra potenziometro e dashboard; ->per me varia: se la dashboard risulta unconnected passa il controllo al WCS
    come gestire due richieste contemporanee di cambio modalità; -> bisogna scegliere chi ha la pirorità...
    cosa succeda, dopo un’apertura al 100%, quando il livello scende sotto L₂ ma rimane sopra L₁; in pratica cosa fare quando trona sotto ad l2 (aprire al 50% o lasicare aperto al 100% finche non scende a livello < l1 )
    quali messaggi e formati usare per MQTT, seriale e HTTP;
    linguaggi e framework per CUS e dashboard.


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
                            





