/*
==============================================================================
VOICESCOPE / THRENOSCOPE - SESSION ESAUSTIVA (COMMENTATA IN ITALIANO)
------------------------------------------------------------------------------
Obiettivo:
- Fornire un file "laboratorio" per live coding, con blocchi indipendenti.
- Coprire le API principali di ~drones: droni, accordi, satelliti, macchine,
  automazioni, salvataggio stati/score, utility GUI e chiusura sicura.

Come usarlo:
1) Esegui i blocchi uno alla volta (uno script alla volta, non tutto insieme).
2) Ascolta il risultato e modifica i parametri live.
3) Usa i blocchi finali di cleanup quando vuoi chiudere.
==============================================================================
*/

// 00) AVVIO SISTEMA + RESET + DEFAULT SICURI
(
s.waitForBoot({
	// Avvia ThrenoScope in stereo (2 canali), finestra grafica visibile, fondamentale "A".
	~ts = ThrenoScope.new(2, \displayWin, "A");

	// Pulisce eventuali residui di sessioni precedenti.
	~drones.killAll(2);      // fade-out globale in 2 secondi
	~drones.killMachines;    // ferma tutte le machine attive

	// Impostazione musicale globale iniziale.
	~drones.tuning_(\just);  // intonazione naturale
	~drones.scale_(\minor);  // scala minore

	// Envelope globale per i nuovi droni: [attack, release].
	~drones.env_([8, 12]);

	// Overlay grafici della GUI.
	~drones.drawSpeakers_(true);   // mostra posizione speaker
	~drones.drawHarmonics_(true);  // mostra cerchi armonici
	~drones.drawScale_(false);     // nasconde overlay scala
	~drones.drawOctaves_(false);   // nasconde overlay ottave

	"Sessione esaustiva pronta.".postln;
});
)


// 01) RICOGNIZIONE RAPIDA (cosa e disponibile nel sistema)
(
// Liste utili per capire subito il materiale disponibile.
~drones.tunings;   // intonazioni disponibili
~drones.scales;    // scale disponibili
~drones.chords;    // dizionario accordi disponibile
~drones.methods;   // metodi controller ~drones
)

(
// Stato runtime attuale: nomi e strutture caricate.
~drones.state;
~drones.names;
)


// 02) CREAZIONE DRONI BASE (voci singole)
(
// Drone "root": base armonica principale.
~root = ~drones.createDrone(\saw, 1,
	harmonics: 4,   // numero armoniche
	amp: 0.08,      // ampiezza prudente per live
	speed: 14,      // velocita di rotazione
	length: 160,    // ampiezza dell'arco
	angle: 20,      // posizione iniziale
	degree: 1,      // grado della scala
	ratio: 1,       // rapporto di tuning
	env: [8, 10],   // envelope locale
	octave: 1,      // ottava
	name: \root     // nome accessibile come ~root
);

// Drone "shadow": componente rumorosa/scura.
~shadow = ~drones.createDrone(\noise, 0.5,
	harmonics: 2, amp: 0.06, speed: -11, length: 220, angle: 195,
	degree: 5, ratio: 1, env: [6, 12], octave: 1, name: \shadow
);

// Drone "air": componente acuta/leggera.
~air = ~drones.createDrone(\sine, 2,
	harmonics: 1, amp: 0.04, speed: 7, length: 90, angle: 310,
	degree: 9, ratio: 1, env: [10, 14], octave: 2, name: \air
);
)


// 03) PARAMETRI DRONE (controllo completo su una singola voce)
(
// Dominio intonazione/altezza.
~root.tonic_(1.5, 6);   // moltiplica la fondamentale in 6 secondi
~root.ratio_(7, 6);     // imposta indice rapporto nella tuning corrente
~root.degree_(4, 4);    // imposta grado scala
~root.note_(\C, 4);     // nota simbolica (A, Bb, Cs, ecc.)
~root.octave_(2, 5);    // imposta ottava
~root.transpose_(2, 5); // trasposizione per step di tuning
~root.interval_(1, 5);  // trasposizione per intervallo di scala

// Varianti relative (offset rispetto al valore attuale).
~root.relTonic_(0.25, 4);
~root.relRatio_(1, 4);
~root.relDegree_(1, 4);
~root.relOctave_(1, 6);
)

(
// Dominio timbrico e dinamico.
~shadow.harmonics_(6, 8);   // aumenta complessita armonica
~shadow.resonance_(2.3, 8); // enfatizza risonanza
~shadow.amp_(0.05, 6);      // ampiezza assoluta
~shadow.relAmp_(0.15, 6);   // ampiezza relativa (+15%)

// Dominio geometrico/movimento.
~shadow.speed_(18);          // velocita assoluta
~shadow.relSpeed_(-0.2);     // velocita relativa (-20%)
~shadow.angle_(120);         // angolo assoluto in gradi
~shadow.rotation_(pi * 0.35);// rotazione in radianti
~shadow.length_(140);        // lunghezza arco
~shadow.relLength_(-0.2);    // lunghezza relativa
)

(
// Cambio tipo synth, colori GUI, tuning e scala per il singolo drone.
~air.type_(\tri, 2);
~air.fillColor_(Color(0.2, 0.5, 0.9, 0.5));
~air.strokeColor_(Color(0.8, 0.9, 1.0, 0.8));
~air.tuning_(\pythagorean, 8);
~air.scale_(\dorian);
)

(
// Metodi utilita (debug/monitoring/esplorazione).
~root.state;        // stato leggibile
~root.params;       // parametri in formato compatto
~root.methods;      // elenco metodi drone
~root.playRatios(0.15, false);  // percorre rapporti
~root.playDegrees(0.15, false); // percorre gradi
~root.playScale(0.15, false);   // percorre scala
)


// 04) CONTROLLO GLOBALE (~drones)
(
// Applica operazioni a tutti i droni attivi.
~drones.amp_(0.07, 6);         // amp globale assoluta
~drones.relativeAmp_(0.1, 6);  // +10% su ciascun drone
~drones.pushEnv_([6, 16]);     // aggiorna env anche ai droni gia vivi
~drones.set(\oscamp, 0.85);    // set diretto parametri synth
)

(
// Toggle degli overlay grafici.
~drones.drawHarmonics_(false);
~drones.drawScale_(true);
~drones.drawOctaves_(true);
~drones.reDraw; // ridisegna interfaccia
)


// 05) PANORAMICA TIPI SONORI (audizione rapida)
(
// Dimostrazione: crea un drone per ciascun tipo documentato.
~types = [\saw, \sine, \tri, \cub, \pulse, \formant, \eliane, \noise, \klank, \gendy, \pad, \atom];

~typeDemo = Routine({
	~types.do({ |tp, i|
		var name = ("tp_" ++ i).asSymbol;
		~drones.createDrone(tp, 1,
			harmonics: rrand(1, 6),
			amp: 0.03,
			speed: rrand(-20, 20),
			length: rrand(40, 160),
			angle: (i * 30) % 360,
			degree: 1 + (i % 7),
			ratio: 1 + (i % 5),
			env: [1, 2],
			octave: 1,
			name: name
		);
		0.35.wait;
	});
	4.wait;
	~drones.killAll(4); // spegne demo in fade
}).play;
)


// 06) ACCORDI (DroneChord API)
(
// Accordo pad morbido.
~padChord = ~drones.createChord(\pad, \minor9,
	tonic: 1, harmonics: 4, amp: 0.05, speed: 12, length: 150, angle: 60,
	degree: 1, ratio: 1, env: [8, 10], octave: 2, name: \padChord
);

// Accordo metallico con array di rapporti custom.
~metalChord = ~drones.createChord(\saw, [1, 5, 8, 12],
	tonic: 2, harmonics: 3, amp: 0.04, speed: -15, length: 110, angle: 210,
	degree: 3, ratio: 1, env: [5, 8], octave: 1, name: \metalChord
);
)

(
// Copertura metodi principali accordo.
~padChord.changeChord(\major7sus4, 8, 0);
~padChord.chord_(\minor11, 8, 0);
~padChord.degree_(4, 6, true);
~padChord.ratio_(6, 6, true);
~padChord.note_(\F, 6, false);
~padChord.tonic_(1.5, 8);
~padChord.relTonic_(0.2, 6);
~padChord.relDegree_(1, 6, true);
~padChord.relRatio_(1, 6, false);
~padChord.octave_(2, 6);
~padChord.relOctave_(1, 6);
~padChord.transpose_(2, 6);
~padChord.interval_(1, 6);
~padChord.harmonics_(7, 8);
~padChord.resonance_(2.0, 6);
~padChord.amp_(0.04, 5);
~padChord.relAmp_(0.1, 5);
~padChord.speed_(9);
~padChord.angle_(120);
~padChord.length_(170);
~padChord.type_(\tri);
~padChord.scale_(\dorian);
~padChord.tuning_(\just, 6);
)


// 07) SATELLITI (DroneSatellites API)
(
// Campo di particelle ritmiche/spettrali.
~swarm = ~drones.createSatellites(\pulse,
	ratios: [1, 2, 3, 5, 7, 8, 11],
	tonic: 2,
	harmonics: 2,
	amp: 0.02,
	speed: { rrand(-22, 22) }, // funzione random per velocita
	length: 35,
	angle: 0,
	num: 16,
	spread: 2.4,
	env: [2, 4],
	octave: 2,
	name: \swarm
);
)

(
// Copertura metodi principali satelliti.
~swarm.num_(22);
~swarm.spread_(3.2);
~swarm.changeChord(\minor7th, 6, 0);
~swarm.chord_(\major9, 8, 0);
~swarm.scale_(\minor, 6, 0);
~swarm.degree_(5, 6);
~swarm.relDegree_(1, 6);
~swarm.ratio_(6, 6);
~swarm.relRatio_(1, 6);
~swarm.tonic_(1.5, 7);
~swarm.relTonic_(0.2, 7);
~swarm.octave_(3, 6);
~swarm.relOctave_(-1, 6);
~swarm.transpose_(2, 6);
~swarm.interval_(1, 6);
~swarm.harmonics_(5, 8);
~swarm.resonance_(1.8, 6);
~swarm.amp_(0.018, 5);
~swarm.relAmp_(0.15, 5);
~swarm.speed_(16);
~swarm.angle_(180);
~swarm.length_(55);
~swarm.type_(\tri);
~swarm.tuning_(\just, 6);
)


// 08) AUTOMAZIONE (recParam / addFunc / stopParam)
(
// recParam: crea automazioni random interne su range specificati.
~root.recParam(\harmonics, 2, 8, 0);
~root.recParam(\amp, 0.02, 0.08, 3);
~padChord.recParam(\degree, 1, 7, 0);
~swarm.recParam(\amp, 0.01, 0.03, 3);
)

(
// addFunc: task personalizzato deterministico (nome + funzione + periodo).
~root.addFunc(\rootWarp, {
	~root.angle_((~root.angle.raddeg + 25) % 360);
	~root.relDegree_([1, -1, 2, 0].choose, 1.5);
	~root.harmonics_([2, 3, 5, 7].choose, 2);
}, 2);
)

(
// Stop pulito delle automazioni.
~root.stopParam(\harmonics);
~root.stopParam(\amp);
~padChord.stopParam(\degree);
~swarm.stopParam(\amp);
~root.stopParam(\rootWarp);
)


// 09) MACCHINE (DroneMachine API)
(
// Le machine alterano parametri per un tempo limitato e poi ripristinano.
~drones.createMachine(\amp, \all, 20, 1, 3, 1, \machAmpAll);
~drones.createMachine(\harmonics, \all, 20, 1, 4, 1, \machHarmAll);

// Target specifico: usa il nome interno reale dell'oggetto.
if(~swarm.notNil) {
	~drones.createMachine(\scale, ~swarm.name, 16, 1, 3, 2, \machSwarmScale);
};
if(~padChord.notNil) {
	~drones.createMachine(\chord, ~padChord.name, 16, 1, 4, 2, \machPadChord);
};
)

(
// Interrompi machine specifica o tutte.
~drones.killMachine(\machAmpAll);
~drones.killMachines;
)


// 10) CLUSTER INTERPRETATI (interpret API)
(
// Raggruppa le creazioni dentro un blocco "interpretato".
~drones.interpret({
	~drones.createDrone(\sine, 1, amp: 0.03, harmonics: 2, speed: 11, length: 90, angle: 30, name: \intA);
	~drones.createDrone(\sine, 1, amp: 0.03, harmonics: 3, speed: -9, length: 100, angle: 210, name: \intB);
	~drones.createDrone(\sine, 2, amp: 0.03, harmonics: 4, speed: 6, length: 110, angle: 330, name: \intC);
}, \interSineTriad);
)


// 11) WORKFLOW REGISTRAZIONE / STATO / SCORE
(
// Inizia a registrare i comandi eseguiti.
~drones.startRecord;
)

(
// Esempio: durante la registrazione crei nuovo materiale.
~drones.createDrone(\formant, 1, amp: 0.04, harmonics: 4, speed: 13, length: 120, angle: 70, name: \recordedA);
~drones.createSatellites(\pulse, ratios: [1, 2, 5, 8], tonic: 2, num: 10, amp: 0.015, speed: 12, length: 25, spread: 1.7, name: \recordedSat);
)

(
// Stop registrazione e salvataggio.
~drones.stopRecord;
~drones.saveState(\lab_state_a); // snapshot atemporale
~drones.saveScore(\lab_score_a); // score temporale
)

(
// Recall stato + riproduzione score.
~drones.loadState(\lab_state_a);
~drones.playScore(\lab_score_a, 1.0);
)

(
// Ferma score e mostra archivio disponibile.
~drones.stopScore;
~drones.states;
~drones.scores;
)

(
// Transizione morbida verso altro stato (opzionale).
// ~drones.swapState(\another_state_name, 12);
)


// 12) GRUPPI (opzionale: richiede droni nominati esistenti)
(
// Definisci un template gruppo da droni esistenti.
// Primo argomento = nome template gruppo, poi nomi droni.
// ~drones.defineGroup(\ritualGroup, \root, \shadow, \air);
// ~drones.groups; // elenca gruppi salvati
)

(
// Istanzia gruppo da template salvato (opzionale).
// ~drones.createGroup(\ritualGroup, \saw, 1, 4, 0.04, 10, 120, 0, 1, 1, [6, 8], 1);
)


// 13) MIDI (opzionale)
(
// Mappa parametri di un drone a controllo MIDI.
// ~root.exposeMIDI([\amp, \harmonics, \degree]);

// Modalita tastiera MIDI -> creazione droni.
// ~drones.addMIDI(\saw, 4, false, [0.01, 0.2]);
// ~drones.stopMIDI;
)


// 14) GUI / SCOPE (opzionale)
(
// Scope richiede setup server compatibile (usare con cautela).
// ~drones.scope_(true);
// ~drones.scope_(false);

~drones.gui_(true); // apre DroneGUI
// ~drones.gui_(false); // chiude DroneGUI
)


// 15) MACRO LIVE VELOCI (comandi rapidi in performance)
~drones.tuning_(\just, 8);
~drones.tuning_(\pythagorean, 8);
~drones.scale_(\minor);
~drones.scale_(\chromatic);
~drones.drawHarmonics_(true);
~drones.drawScale_(false);
~drones.amp_(0.05, 5);
~drones.relativeAmp_(-0.2, 5);


// 16) CHIUSURA SICURA SESSIONE
(
~drones.stopScore;    // ferma timeline score
~drones.killMachines; // ferma machine
~drones.killAll(18);  // fade-out finale lungo
)
