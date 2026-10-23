// Creo oggetto immutabile
export const CONFIG = Object.freeze({
    CUS_BASE_URL: "",      // Indirizzo del CUS
    STATE_POLL_INTERVAL_MS: 1000,               // Tempo di attesa fra un aggiornamento e la successiva richiesta
    MEASUREMENTS_POLL_INTERVAL_MS: 1000,        // Tempo di attesa fra un aggiornamento e la successiva richiesta
    REQUEST_TIMEOUT_MS: 3000                    // Timeout delle richieste
});
