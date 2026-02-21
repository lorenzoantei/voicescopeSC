/*
Time Machines-inspired live coding snippets for Voicescope/ThrenoScope.
Evaluate block-by-block during performance.
*/

// 1) BOOT + GLOBAL SETUP
(
s.waitForBoot({
	t = ThrenoScope.new(2, \displayWin, "A");
	~drones.killAll(2);

	~drones.tuning = \just;
	~drones.scale = \minor;
	~drones.env_([8, 14]);
	~drones.drawHarmonics = true;
	~drones.drawScale = false;

	if(~pulseTask.notNil) { ~pulseTask.stop };
	if(~warpTask.notNil) { ~warpTask.stop };
	"Voicescope ready: Time Machines session".postln;
});
)

// 2) FOUNDATION: DARK MACHINE BED
(
~floor = ~drones.createDrone(\noise, 1,
	amp: 0.10, harmonics: 2, speed: 6, length: 240, angle: 20, name: \floor
);

~motor = ~drones.createDrone(\saw, 0.5,
	amp: 0.06, harmonics: 5, speed: -12, length: 130, angle: 200, name: \motor
);

~sub = ~drones.createDrone(\sine, 0.5,
	amp: 0.05, harmonics: 1, speed: 3, length: 70, angle: 330, name: \sub
);

~floor.resonance = true;
~floor.resonance_(2.1, 6);
)

// 3) CLOCKWORK PULSES: MECHANICAL GHOST RHYTHM
(
~ticks = ~drones.createSatellites(\pulse,
	ratios: [1, 2, 3, 5, 8, 13],
	tonic: 2,
	num: 16,
	amp: 0.028,
	speed: { rrand(-26, 32) },
	length: 28,
	spread: 2.6,
	name: \ticks
);

~pulseTask = Routine({
	loop {
		~ticks.num_([8, 12, 16, 24].choose, 1.5);
		~ticks.length_([20, 28, 40, 55].choose, 2);
		~ticks.amp_(rrand(0.016, 0.040), 2);
		(2.5 + 2.0.rand).wait;
	}
}).play;
)

// 4) RITUAL LIFT: HARMONIC BLOOM + SCALE FLIP
(
~choir = ~drones.createDrone(\pad, 2,
	amp: 0.05, harmonics: 6, speed: 9, length: 180, angle: 120, degree: 5, name: \choir
);

~air = ~drones.createSatellites(\tri,
	ratios: [1, 3, 5, 7, 9],
	tonic: 1,
	num: 10,
	amp: 0.020,
	speed: { rrand(-14, 18) },
	length: 90,
	spread: 3.5,
	name: \air
);

~drones.drawHarmonics = false;
~drones.drawScale = true;
~drones.scale_(\chromatic);
~drones.tuning_(\just, 10);
)

// 5) WARP MODE: DRIFT PITCH/SPACE IN LONG CYCLES
(
~warpTask = Routine({
	var deg = [1, 3, 4, 6, 7, 9, 11];
	var i = 0;
	loop {
		~motor.degree_(deg.wrapAt(i), 3);
		~choir.degree_(deg.wrapAt(i + 2), 5);
		~floor.harmonics_([2, 3, 4, 6].choose, 4);
		~sub.amp_(rrand(0.03, 0.06), 6);
		~floor.resonance_(rrand(1.6, 2.8), 8);
		i = i + 1;
		(5 + 4.rand).wait;
	}
}).play;
)

// 6) PERFORMANCE MACROS (evaluate anytime)
~drones.scale_(\minor);
~drones.scale_(\dorian);
~ticks.kill(10);
~air.kill(12);
~motor.amp_(0.02, 8);
~choir.amp_(0.08, 14);
~floor.amp_(0.12, 8);

// 7) CLEAN FADE OUT
(
if(~pulseTask.notNil) { ~pulseTask.stop };
if(~warpTask.notNil) { ~warpTask.stop };
~drones.killAll(20);
)
