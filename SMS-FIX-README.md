# Noticall 1.1 - SMS Fix

Questa revisione corregge due punti della build precedente:

1. usa una SIM concreta (default SMS, oppure prima SIM attiva) invece di affidarsi sempre a SmsManager.getDefault();
2. usa AlarmManager.setAndAllowWhileIdle() per i passaggi ritardati, così il flusso può proseguire anche con schermo spento/Doze;
3. gestisce SMS multipart;
4. registra nello storico il vero esito dell'invio e il motivo dell'errore.

## Test consigliato

1. Installa la nuova build.
2. Apri Noticall e verifica che dica Permessi operativi.
3. Invia prima un SMS DI PROVA.
4. Controlla Attività: deve comparire `SMS di prova inviato` oppure un errore preciso.
5. Se il test manuale funziona, attiva l'automazione, imposta 5-10 secondi di ritardo, blocca lo schermo e fai una chiamata persa da un altro telefono.
6. Controlla Attività per la sequenza MISSED -> SCHEDULED -> SENT.

Nota: SEND_SMS è un permesso hard-restricted su Android moderno. Per test sideload può essere necessario installare/consentire il permesso con un installer che lo allowlisti o usare ADB con le opzioni appropriate.
