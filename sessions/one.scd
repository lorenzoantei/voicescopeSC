/*
==============================================================================
THRENOSCOPE SESSION: "INTERCONNECTED MACHINES"
Duration: 30 minutes
Tuning: Just Intonation / Harmonic Series Focus

Note:
- ThrenoScope gives you global access through ~drones. [page:1]
- Drones/chords/satellites are controlled via the object returned by create*
  OR via the global variable created by the name: \symbol you pass. [page:1]
==============================================================================
*/

// Exit ProxySpace if you are in it (common in live coding setups)
// p = ProxySpace.pop;   // or just: ProxySpace.pop;
// currentEnvironment.postln; // should NOT print "ProxySpace (...)"



// --- STEP 0: INITIALIZATION ---
(
s.waitForBoot({

    // ThrenoScope init: ThrenoScope.new(channels, mode, key, appPath?) [page:2]
    // For stereo, use 2 channels. Key sets the fundamental, e.g. "A". [page:2]
    t = ThrenoScope.new(2, \displayWin, "A");  // single backslash [page:1]
	~drones.tuning = \just;
	~drones.scale  = \minor; //backslash for symbols

    // Global envelope setting: in docs it's an array [attack, release]. [page:1]
    // (Using long times for smooth transitions.)
    ~drones.env_([10, 10]);

    // Set tuning/scale explicitly (optional but consistent with your concept). [page:1]
    ~drones.drawHarmonics = true;
    ~drones.drawScale = false;
    ~drones.tuning = \just;
    ~drones.scale = \minor;

    "ThrenoScope Initialized. Ready for 'Interconnected Machines'.".postln;
});
)

/*
==============================================================================
PHASE 1: SUBMERGED (00:00 - 05:00)
Focus: Deep noise floors and resonant foundations.
==============================================================================
*/

(
~base = ~drones.createDrone(\noise, 1,
    amp: 0.12,      // was 0.64/0.9 -> too hot [page:1]
    speed: 10,
    length: 260,
    harmonics: 2,
    angle: 30,
    name: \subGround
);

~metal = ~drones.createDrone(\saw, 0.5,
    amp: 0.07,
    speed: -14,
    length: 140,
    harmonics: 6,
    angle: 190,
    name: \core
);

~subGround.resonance = true;
~subGround.resonance_(2.2, 8);

fork {
    25.wait;
    ~anchor = ~drones.createDrone(\sine, 1,
        amp: 0.035,
        speed: 4,
        length: 60,
        harmonics: 1,
        angle: 0,
        name: \anchor
    );
};
)


~base.amp_(0.9, 6);
~metal.amp_(0.9, 6);
~subGround.amp_(0.9, 5);


/*
==============================================================================
PHASE 2: ORBITAL RESONANCE (05:00 - 10:00)
Focus: Geometric harmonic expansion and satellites.
==============================================================================
*/

(
~sat1 = ~drones.createSatellites(\sine,
    ratios: [1, 2, 3, 5, 8],
    tonic: 1,
    name: \orbits,
    num: 14,
    amp: 0.045,
    speed: { rrand(-18, 22) },
    length: 80,
    spread: 3
);

~pulser = ~drones.createDrone(\tri, 2,
    amp: 0.030,
    speed: 18,
    length: 20,
    harmonics: 2,
    angle: 90,
    name: \engineBeat
);

~orbits.harmonics_(8, 15);

fork {
    20.wait;
    ~sat2 = ~drones.createSatellites(\tri,
        ratios: [1, 2, 3, 5, 8, 13],
        tonic: 1.5,
        name: \fibonacci,
        num: 10,
        amp: 0.035,
        speed: { rrand(-14, 18) },
        length: 60,
        spread: 2
    );
};
)


/*
==============================================================================
PHASE 2b: SCALE FLIP + SWARM (circa 08:00 - 11:00)
Focus: flip harm/scale view + satellite swarm morph.
==============================================================================
*/
(
~drones.drawHarmonics = false;
~drones.drawScale = true;

// quick modulation: minor -> chromatic (still in just, sounds very different)
~drones.scale_(\chromatic);   // immediate, or use tuning_/scale_ variants if you prefer [page:1]
~drones.tuning_(\just, 8);    // smooth tuning change in 8s [page:1]

// Add a short-lived swarm burst that later collapses
~drones.createSatellites(\saw,
    ratios: [1,2,3,4,5,6,7,8],
    tonic: 2,
    amp: 0.03,
    speed: { rrand(-28, 32) },   // lively but not insane [page:1]
    length: 35,
    num: 28,
    spread: 4,
    name: \burst
);

fork {
    45.wait;
    ~burst.num_(10);
    ~burst.length_(70, 10);
    25.wait;
    ~burst.kill(40);
};
)



/*
==============================================================================
PHASE 3: THE MACHINE ENGINE (10:00 - 15:00)
Focus: Automated interconnectivity (NO DroneMachine dependency)
==============================================================================
*/

(
~tension = ~drones.createDrone(\saw, 1.5,
    amp: 0.05,
    harmonics: 4,
    speed: 22,      // NOT 200 [page:1]
    length: 110,
    angle: 210,
    name: \tension
);

// keep your routines, but make the amp modulation centered around current value safely
if(~machineHarmony.notNil) { ~machineHarmony.stop };
if(~machineBreath.notNil)  { ~machineBreath.stop };
if(~machineTimbre.notNil)  { ~machineTimbre.stop };

~machineHarmony = Routine({
    var degSeq = [1, 3, 5, 4, 6, 2, 7];
    var idx = 0;
    loop {
        if(~orbits.notNil) {
            ~orbits.degree_(degSeq.wrapAt(idx), 2);
            ~orbits.tonic_([1, 1, 2, 1.5, 0.75].choose, 3);
        };
        ~tension.degree_(degSeq.wrapAt(idx + 2), 1.5);
        ~tension.harmonics_([3,4,5,7,9].choose, 2);
        idx = idx + 1;
        (2.0 + 1.0.rand).wait;
    }
}).play;

~machineBreath = Routine({
    var targets;
    loop {
        targets = [~subGround, ~core, ~anchor, ~tension, ~orbits, ~fibonacci].select(_.notNil);
        targets.do { |dr|
            var baseAmp = (dr.amp ? 0.04).clip(0.005, 0.20);
            dr.amp_(baseAmp * rrand(0.75, 1.25), 4);
        };
        5.wait;
    }
}).play;

~machineTimbre = Routine({
    loop {
        if(~subGround.notNil) {
            ~subGround.resonance = true;
            ~subGround.resonance_(rrand(1.4, 2.8), 6);
            ~subGround.harmonics_([2,3,4,5,6,8].choose, 8);
        };
        9.wait;
    }
}).play;
)

/*
==============================================================================
PHASE 3b: MACHINE GATES (circa 12:00 - 16:00)
Focus: irregular gating + tension/orbits interdependence.
==============================================================================
*/
(
if(~gate.notNil) { ~gate.stop };

~gate = Routine({
    loop {
        // gate: pull down bed, push up tension, then release
        if(~subGround.notNil) { ~subGround.amp_(~subGround.amp * 0.7, 0.8) };
        if(~core.notNil)      { ~core.amp_(~core.amp * 0.75, 0.8) };
        if(~tension.notNil)   { ~tension.amp_(~tension.amp * 1.25, 0.8) };

        // tie orbit speed to tension activity
        if(~orbits.notNil and: { ~tension.notNil }) {
            ~orbits.speed_( (~tension.speed ? 10) * [0.6, 0.8, 1.0, 1.2].choose, 1.5);
        };

        (2.0 + 2.0.rand).wait;

        // release back
        if(~subGround.notNil) { ~subGround.amp_(~subGround.amp * 1.15, 2.5) };
        if(~core.notNil)      { ~core.amp_(~core.amp * 1.10, 2.5) };
        if(~tension.notNil)   { ~tension.amp_(~tension.amp * 0.90, 2.5) };

        (4.0 + 3.0.rand).wait;
    }
}).play;

"Gate routine started.".postln;
)




/*
==============================================================================
PHASE 4: HARMONIC DENSITY (15:00 - 25:00)
Focus: Rich additive textures and dense frequency clusters.
==============================================================================
*/

(
~dense = ~drones.createDrone(\eliane, 1,
    amp: 0.07,
    speed: 9,
    harmonics: 8,
    length: 200,
    name: \highDensity
);

~cloud = ~drones.createDrone(\tri, 1.5,
    amp: 0.06,
    harmonics: 6,
    speed: -12,
    length: 160,
    name: \upperCloud
);

~chaos = ~drones.createDrone(\gendy, 0.75,
    amp: 0.045,
    harmonics: 5,
    speed: 16,
    length: 90,
    name: \chaos
);

~orbits.num_(20);
~orbits.spread_(4);

~orbits.tonic_(2, 120);
~upperCloud.tonic_(2, 180);

fork {
    60.wait;
    ~chord = ~drones.createChord(\sine, \minor7th, 1,
        harmonics: 2,
        amp: 0.04,
        speed: 8,
        length: 140,
        name: \verticalHarmony
    );
};

fork {
    300.wait;
    ~voice = ~drones.createDrone(\saw, 1.2,
        amp: 0.035,
        harmonics: 8,
        speed: -10,
        length: 80,
        name: \voice
    );
    ~voice.resonance = true;
    ~voice.resonance_(3, 30);
};
)

/*
==============================================================================
PHASE 4b: VERTICAL/HORIZONTAL HYBRID (circa 18:00 - 23:00)
Focus: chord drones + satellite chord morph.
==============================================================================
*/
(
~drones.drawHarmonics = true;
~drones.drawScale = false;

~drones.createChord(\tri, \dominant7add11, 2,
    amp: 0.035, harmonics: 4, speed: 9, length: 160, angle: 15, name: \domClimb
);

~domClimb.resonance = true;
~domClimb.resonance_(2.0, 10);
~domClimb.harmonics_(9, 20);

// If orbits exists, morph its harmonic field via chord changes (satellite method) [page:1]
if(~orbits.notNil) {
    ~orbits.changeChord(\minor7th, 15, 0);   // chord, duration, transposition [page:1]
    fork {
        40.wait;
        ~orbits.changeChord(\major9, 20, 0);
        50.wait;
        ~orbits.changeChord(\sus4, 12, 0);
    };
};
)

/*
==============================================================================
PHASE 4c: MICROTONAL DETOUR (circa 23:00 - 25:00)
Focus: tuning swap as a structural event.
==============================================================================
*/
(
~drones.drawHarmonics = false;
~drones.drawScale = true;

~drones.scale_(\chromatic);
~drones.tuning_(\mean6, 12);   // smooth morph [page:1]

fork {
    70.wait;
    ~drones.tuning_(\just, 12);   // return [page:1]
    ~drones.scale_(\minor);
};
)


/*
==============================================================================
PHASE 5+: FINAL WASH + GUARANTEED STOP
==============================================================================
*/
(
fork {
    30.wait;
    if(~subGround.notNil) { ~subGround.harmonics_(1, 20) };
    if(~core.notNil)      { ~core.harmonics_(1, 25) };
    if(~orbits.notNil)    { ~orbits.num_(3); ~orbits.speed_(6, 12) };

    90.wait;
    ~drones.killAll(10);   // guaranteed silence [page:1]
};
)


/*
==============================================================================
UTILITY COMMANDS
==============================================================================
*/

// Show active drones (names):
 ~drones.droneArray.collect(_.name);  // droneArray is mentioned as concept; method may vary by version. [page:1]

// Emergency stop:
// ~drones.killAll;      // immediate [page:1]
// ~drones.killAll(3);   // fade out [page:1]

// Change scale/tuning live:
// ~drones.scale = \minor;  // [page:1]
// ~drones.tuning = \just;  // [page:1]

// Manual live edits examples:
// ~orbits.amp_(0.15, 10); [page:1]
// ~orbits.harmonics_(10, 20); [page:1]
// ~orbits.speed_(1.0); [page:1]
