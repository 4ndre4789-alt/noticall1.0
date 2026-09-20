# Noticall 1.2 — Background Engine Fix

Questa build parte dalla 1.1 e modifica il tratto che gestisce l'automazione dopo una chiamata persa.

## Perché
L'SMS di prova funziona, quindi SIM, SEND_SMS e SmsManager sono operativi. Il problema era nel passaggio automatico e nel timer in background.

## Cosa cambia
- quando Noticall è ATTIVO parte un foreground service leggero con notifica persistente;
- il controllo del registro chiamate usa `BroadcastReceiver.goAsync()` anziché un secondo AlarmManager;
- dopo la chiamata persa, il messaggio viene messo in coda nel motore background;
- durante il breve ritardo viene mantenuto un PARTIAL_WAKE_LOCK con timeout;
- lo stesso `SmsEngine` già funzionante per l'SMS di prova esegue l'invio automatico;
- se Android impedisce di riavviare il servizio da background, fallback a invio immediato;
- log più precisi: MISSED -> SCHEDULED -> QUEUED -> SENDING -> SENT/ERROR.

## Test consigliato
1. Installa la nuova build sopra la precedente.
2. Apri Noticall, spegni e riaccendi Automazione una volta.
3. Metti ritardo 5 secondi e anti-spam 0 per il test.
4. Blocca lo schermo.
5. Chiama da un altro telefono e lascia terminare la chiamata senza rispondere.
6. Attendi 10-15 secondi e verifica l'SMS.
7. Apri Attività: la sequenza attesa è MISSED, SCHEDULED, QUEUED, SENDING, SENT.
