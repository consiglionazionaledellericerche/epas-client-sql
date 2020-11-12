# ePAS client - timbratura da database SQL 

## Introduzione

Questo software è parte del progetto [ePAS](https://epas.projects.iit.cnr.it),
il sistema di rilevazione e gestione delle presenze sviluppato dal 
[CNR](https://www.cnr.it).

Questo client si occupa di leggere da un database SQL la tabella contenente 
le timbrature, di effettuare il mapping tra le colonne del database 
ed i campi JSON attesi da ePAS e di inviare le timbrature via REST al server di 
ePAS.

Il software è distribuito come immagine [Docker](https://docker.com) 
configurata per accedere a scadenza regolare al database contenente le 
timbrature ed inviare via REST le nuove timbrature al server di ePAS.
 
Nell'immagine docker sono presenti anche i *cron* che servono per re-inviare 
notte tempo le timbrature che non è stato possibile inviare ad ePAS durante le 
letture giornaliere delle timbrature.

## Requisiti

Consigliamo di creare una nuova macchina virtuale che contenga solamente il 
client che si interfaccerà con il database SQL delle timbrature della vostra 
sede e che dovrà inviare al server centralizzato di ePAS le timbrature 
opportunamente normalizzate perchè il server possa correttamente interpretarle
ed assegnarle al dipendente.

### Requisiti Hardware

- CPU: 1 core è sufficiente
- Memoria: 1 GB di ram
- Storage: 10GB di Hard Disk 
  - 10GB consentono di contenere i vari file di log e le eventuali timbrature da 
    reinviare con tranquillità

### Requisiti Software

- Sistema operativo supportato: Ubuntu 18.04 LTS o superiori
- [Docker](https://docker.com) installato
- [Docker-compose](https://docs.docker.com/compose/) installato

### Altri requisiti

- aver configurato una sorgente timbratura su ePAS per la propria sede
- accesso alla porta TCP del database server
- conoscere il nome del database e nome e struttura della tabella contenente le
  timbrature
- aver configurato un utente con i diritti di lettura sulla tabella contenente 
  le timbrature

## Installazione e configurazione via docker / docker-compose

L'installazione e configurazione consigliata per il deploy è quella via 
Docker / Docker-compose.

I passi da compiere sono:
 - creare un utente *epas* (o con altro nome a vostro piacimento)
 - aggiungere l'utente epas al gruppo *docker* 
   (altrimenti non può usare i comandi docker) 
   `$ sudo adduser epas docker`
 - creare una directory *epas-client* nella home dell'utente epas 
   (es. */home/epas/epas-client*)
 - creare una directory *data* nella cartella creata al punto precedente 
   (es. */home/epas/epas-client/data*)
 - copiare il file [docker-compose.yml](docker-compose.yml) ed il file 
   [.env](.env) nella directory */home/epas/epas-client*
 - configurare il [.env](.env) seguendo i commenti già presenti nei file di 
   esempio, oppure consultando il paragrafo successivo
 - gli ulteriori parametri configurabili nel file *docker-compose.yml* sono da
   configurare solo in casi eccezionali

## Parametri di configurazione

| Parametro               | Descrizione                                                                                                                                                                                                                                                                            | Obbligatorio |                           Default                          |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:------------:|:----------------------------------------------------------:|
|                         |                                                                                                                       *PARAMETRI PER L'INVIO TIMBRATURE AD EPAS*                                                                                                                       |              |                                                            |
| SERVER_HOST             | Host del server di ePAS. Esempio: epas.devel.iit.cnr.it                                                                                                                                                                                                                                |      SI      |                       Nessun default                       |
| SERVER_PROTOCOL         | Protocollo per l'invio delle timbrature al server di ePAS. Possibili valori: *http*, *https*                                                                                                                                                                                           |      NO      |                            https                           |
| SERVER_PORT             | Porta del server di ePAS. Solitamente la porta per le comunicazioni via https è la _443_                                                                                                                                                                                               |      NO      |                             443                            |
| SERVER_USER             | Utente della "sorgente timbrature" di ePAS.                                                                                                                                                                                                                                            |      SI      |                       Nessun default                       |
| SERVER_PASSWORD         | Password della "sorgente timbrature" di ePAS                                                                                                                                                                                                                                           |      SI      |                       Nessun default                       |
|                         |                                                                                                                                                                                                                                                                                        |              |                                                            |
|                         |                                                                                                                         *PARAMETRI PER ACCESSO AL DATABASE SQL*                                                                                                                        |              |                                                            |
| DB_TYPE                 | Tipologia di database server. Tipologie supportate: mysql, mssql.                                                                                                                                                                                                                      |      SI      |                       Nessun default                       |
| DB_HOST                 | Indirizzo IP o hostname del server SQL                                                                                                                                                                                                                                                 |      SI      |                       Nessun default                       |
| DB_PORT                 | Porta tcp per l'accesso al server SQL                                                                                                                                                                                                                                                  |      SI      |                       Nessun default                       |
| DB_NAME                 | Nome del database                                                                                                                                                                                                                                                                      |      NO      |              vuoto (possibile solo per mssql)              |
| DB_USER                 | Utente per l'accesso al database                                                                                                                                                                                                                                                       |      SI      |                       Nessun default                       |
| DB_PASSWORD             | Password per l'accesso al database                                                                                                                                                                                                                                                     |      SI      |                       Nessun default                       |
|                         |                                                                                                                                                                                                                                                                                        |              |                                                            |
|                         |                                                                                                                    *PER MAPPING COLONNE DB E CAMPI TIMBRATURA EPAS*                                                                                                                    |              |                                                            |
|                         |                                                                                                                                                                                                                                                                                        |              |                                                            |
| DB_FIELDS_ID_TIMBRATURA | Campo del DB corrispondente ad un ID univoco incrementale. Utilizzato per tenere traccia dell'ultima timbratura inviata dal client.                                                                                                                                                    |      SI      |                       Nessun default                       |
| DB_FIELDS_BADGE         | Campo del DB corrispondente al numero di badge.                                                                                                                                                                                                                                        |      SI      |                       Nessun default                       |
| DB_FIELDS_VERSO         | Campo del DB corrispondente al verso. Vedi anche il parametro DB_FIELDS_VERSO_ENTRATA.                                                                                                                                                                                                 |      SI      |                       Nessun default                       |
| DB_FIELDS_VERSO_ENTRATA | Valore corrispondente al verso di entrata, per esempio "0" o "E"                                                                                                                                                                                                                       |      SI      |                       Nessun default                       |
| DB_FIELDS_DATAORA       | Campo del DB corrispondente alla data e ora della timbratura                                                                                                                                                                                                                           |      SI      |                       Nessun default                       |
| DB_FIELDS_DATA          | Campo del DB corrispondente alla data della timbratura (utilizzato congiuntamente al parametro DB_FIELDS_ORA è alternativo all'utilizzo del campo DB_FIELDS_DATAORA).                                                                                                                  |      NO      |                       Nessun default                       |
| DB_FIELDS_ORA           | Campo del DB corrispondente all'ora della timbratura (utilizzato congiuntamente al parametro DB_FIELDS_ORA è alternativo all'utilizzo del campo DB_FIELDS_DATAORA).                                                                                                                    |      NO      |                       Nessun default                       |
| DB_FIELDS_LETTORE       | Campo del DB corrispondente al lettore.                                                                                                                                                                                                                                                |      NO      |                       Nessun default                       |
|                         |                                                                                                                                                                                                                                                                                        |              |                                                            |
|                         |                                                                                                                   *Parametri generici generalmente da non impostare*                                                                                                                   |              |                                                            |
| DAYS_BEFORE             | Il numero di giorni a partire da oggi dal quale iniziare il download delle timbrature e assenze.                                                                                                                                                                                       | NO           | 30                                                         |
| STAMPINGS_CRON          | Crono che definisce ogni quanto vengono inviate le timbrature. utilizzare il formato richiesto dal crontab. Riferimenti -> https://en.wikipedia.org/wiki/Cron#Examples                                                                                                                 | NO           | \*/8 6-23 \* \* \* (ogni 8 minuti dalle 6 alle 23)         |
| CRON_RANDOM_SLEEP       | Secondi di sleep random massimo prima di lanciare (via cron) il client per le timbrature. Questo tempo random serve per evitare che tutte le timbrature arrivino contemporaneamente al server di ePAS.                                                                                 | NO           | 180                                                        |
| BAD_STAMPINGS_CRON      | Cron che definisce l'invio di tutte le timbrature non inviate correttamente a epas (badge non trovato o altri problemi).                                                                                                                                                               | NO           | -0 1 \* \* \* (all'una di notte)                           |
| MAX_BAD_STAMPING_DAYS   | Tutte le timbrature con problemi, più vecchie di questo numero di giorni dal momento dell'esecuzione, vengono buttate via.                                                                                                                                                             | NO           | 10                                                         |
| RETRY_CODES             | Identifica i codici HTTP di risposta da parte di ePAS all'inserimento di una timbratura per cui è opportuno che  la timbratura venga re-inviata al server per un nuovo tentativo di inserimento. Specificare i valori separati da virgola che comportano un re-invio delle timbrature. | NO           | 401, 404, 500, 501, 502, 503, 504, 505, 506, 507, 508, 509 |


### Verifica della configurazione docker-compose

Una volta configurato il file *docker-compose.yml* ed il file *.env* è possibile 
verificare che la configurazione *docker-compose.yml* sia corretta con il comando 
`$ docker-compose config`.

### Avvio del client

Se la configurazione è corretta per avviare il client lanciare il comando 
`$ docker-compose up -d`.

Tramite il *docker-compose.yml* il container del client è già configurato per 
riavviarsi automaticamente in caso di riavvio della macchina virtuale ospitante
il client.

## Informazioni ed utilità per il mantenimento ed il troubleshooting

La directory */home/epas/epas-client/data* contiene:
 - la directory **log** con i log del client
 - il file *lastRequest.txt* che contiene l'id e la data ed ora dell'ultima 
   timbratura prelevata dal client
- il file *stampsInTrouble.json* (potrebbe non esserci o essere vuoto) che
  contiene le eventuali timbrature da re-inviare con il cron notturno

In caso di necessità è possibile _retrodatare_ il prelevamento delle timbrature
modificando opportunamente il file *info/lastRequest.txt*.

Per verificare che il container docker sia attivo, da dentro la directory
*/home/epas/epas-client*, utilizzare il comando `$ docker-compose ps`.

Per riavviare il servizio utilizzare il comando `$ docker-compose restart`.

Se volete entrare nel container per verificarne il contenuto potete utilizzare
il comando `$ docker-compose exec client bash`.

Se, senza entrare nel container, volete lanciare un'acquisizione immediata delle
timbrature potete utilizzare il comando 
`$ docker-compose exec client /client/stampings.sh`.
