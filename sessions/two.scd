s.boot;

(
~numChannels = 2;

s.waitForBoot({

    // --- ThrenoScope init (channels, mode, key) ---
    ~threno = ThrenoScope.new(~numChannels, \displayWin, "A"); // mode/key as per reference [web:31]

    // Clear previous ThrenoScope drones safely
    ~drones.killAll(3);

    // --- Performance controls ---
    ~masterAmp = 0.82;
    ~droneFundAmp = 0.17;
    ~machineAmp = 0.20;
    ~satAmp = 0.13;
    ~machineDensity = 0.55;
    ~satDensity = 0.45;
    ~brightness = 0.42;
    ~orbitSpeed = 0.035;
    ~duckDepth = 0.35;
    ~resonanceLift = 0.22;
    ~harmonicWarp = 0.0;
    ~globalRelease = 12;

    // --- Harmonic logic (single backslash symbols) ---
    ~drones.tuning_(\just);
    ~drones.scale_(\dorian);

    // --- Routing ---
    ~srcGroup = Group.head(s);
    ~fxGroup  = Group.after(~srcGroup);

    ~machineBus = Bus.audio(s, 2);
    ~satBus     = Bus.audio(s, 2);

    ~machineCtlBus = Bus.control(s, 1);
    ~droneCtlBus   = Bus.control(s, 1);

    ~machineCtlBus.set(0);
    ~droneCtlBus.set(~droneFundAmp);

    // --- SynthDefs ---
    SynthDef(\vsMachineIrregular, { |out=0, ctlOut=0, gate=1, amp=0.2, density=1.0, bright=0.4, orbit=0, orbitSpeed=0.03, base=70|
        var dens, trig, env, pitch, body, click, sig, pan, eg;
        dens = density.clip(0.05, 10);
        trig = Dust.kr(dens * (1 + LFNoise1.kr(0.18).range(0, 2.5)));
        env  = Decay2.kr(trig, 0.001, LFNoise1.kr(0.22).range(0.03, 0.28));
        pitch = TExpRand.kr(base * 0.8, base * (2 + (bright * 8)), trig);
        body  = SinOsc.ar(pitch * [1, 1.003], 0, env * 0.7);
        click = BPF.ar(WhiteNoise.ar(env), TExpRand.kr(900, 8000, trig), 0.12 + (bright * 0.2));
        sig   = (body + (click * 0.8)) * amp;
        pan   = SinOsc.kr(orbitSpeed, orbit).range(-0.9, 0.9);
        eg    = EnvGen.kr(Env.asr(2, 1, 6), gate, doneAction: 2);
        Out.kr(ctlOut, Lag.kr(env, 0.08));
        Out.ar(out, Pan2.ar(sig, pan) * eg);
    }).add;

    SynthDef(\vsMachineSub, { |out=0, gate=1, amp=0.12, density=0.6, bright=0.3, orbit=0, orbitSpeed=0.02|
        var trig, env, freq, sig, pan, eg;
        trig = Dust.kr(density.clip(0.02, 6));
        env  = Decay2.kr(trig, 0.004, LFNoise1.kr(0.14).range(0.08, 0.45));
        freq = TExpRand.kr(38, 130 + (bright * 120), trig);
        sig  = Ringz.ar(PinkNoise.ar(0.3) * env, freq, 0.18 + (bright * 0.35));
        sig  = HPF.ar(sig, 28);
        pan  = SinOsc.kr(orbitSpeed * 0.7, orbit + 1.1).range(-0.65, 0.65);
        eg   = EnvGen.kr(Env.asr(2, 1, 8), gate, doneAction: 2);
        Out.ar(out, Pan2.ar(sig, pan, amp) * eg);
    }).add;

    SynthDef(\vsSatelliteField, { |out=0, machineCtl=0, droneCtl=0, gate=1, amp=0.12, density=0.5, bright=0.5, orbit=0, orbitSpeed=0.04, duckAmt=0.35|
        var mAct, dAmp, dens, trig, env, f1, f2, src, form, sig, duck, pan, eg;
        mAct = In.kr(machineCtl, 1).lag(0.08).clip(0, 1);
        dAmp = In.kr(droneCtl, 1).lag(0.4).clip(0, 1);
        dens = (density * (0.35 + dAmp.linlin(0, 0.4, 0.1, 1.2))).clip(0.02, 12);
        trig = Dust.kr(dens);
        env  = Decay2.kr(trig, 0.002, LFNoise1.kr(0.11).range(0.04, 0.35));
        f1   = TExpRand.kr(250, 2800 + (bright * 6500), trig);
        f2   = TExpRand.kr(450, 5000 + (bright * 8000), trig);
        src  = HPF.ar(WhiteNoise.ar(0.35) * env, 180);
        form = BPF.ar(src, f1, 0.05 + (bright * 0.25)) + Ringz.ar(src, f2, 0.03 + (bright * 0.16));
        duck = (1 - (mAct * duckAmt).clip(0, 0.9));
        sig  = form * amp * duck;
        pan  = SinOsc.kr(orbitSpeed, orbit + 2.3).range(-1, 1);
        eg   = EnvGen.kr(Env.asr(3, 1, 9), gate, doneAction: 2);
        Out.ar(out, Pan2.ar(sig, pan) * eg);
    }).add;

    SynthDef(\vsSubMix, { |out=0, inMachine=0, inSat=0, master=0.8, duckDepth=0.35, tone=0.4|
        var machine, sat, key, satDucked, glue;
        machine = In.ar(inMachine, 2);
        sat     = In.ar(inSat, 2);
        key = Amplitude.kr(Mix(machine), 0.01, 0.12).clip(0, 1);
        satDucked = sat * (1 - (key * duckDepth).clip(0, 0.95));
        glue = machine + satDucked;
        glue = HPF.ar(glue, 28);
        glue = XFade2.ar(glue, LPF.ar(glue, (1200 + (tone * 13000)).clip(800, 16000)), (tone * 2) - 1);
        glue = LeakDC.ar(glue) * master;
        Out.ar(out, glue);
    }).add;

    SynthDef(\vsSafety, { |bus=0, pregain=1.0, compThresh=0.72, compSlope=0.55, post=0.95|
        var sig;
        sig = InFeedback.ar(bus, 2) * pregain;
        sig = CompanderD.ar(sig, compThresh.clip(0.2, 1), compSlope.clip(0.2, 1), 1, 0.01, 0.15);
        sig = Limiter.ar(sig, 0.98, 0.01);
        ReplaceOut.ar(bus, sig * post);
    }).add;

    s.sync;

    // --- Helpers ---
    ~withDrone = { |name, fn|
        var d = currentEnvironment[name]; // e.g. \baseA -> ~baseA [web:31]
        if(d.notNil) { fn.value(d) };
    };

    ~sharedAngle = 0;

    ~applyControls = {
        var orbitRadA = (~sharedAngle * (pi/180));
        var orbitRadB = ((~sharedAngle + 90) * (pi/180));
        var orbitRadC = ((~sharedAngle + 180) * (pi/180));

        if(~machineA.notNil) {
            ~machineA.set(
                \amp, (~machineAmp * 0.95),
                \density, (~machineDensity.linlin(0, 1, 0.12, 4.5)),
                \bright, ~brightness,
                \orbitSpeed, ~orbitSpeed,
                \orbit, orbitRadA,
                \base, (56 + (~harmonicWarp * 8))
            );
        };
        if(~machineB.notNil) {
            ~machineB.set(
                \amp, (~machineAmp * 0.65),
                \density, (~machineDensity.linlin(0, 1, 0.05, 2.2)),
                \bright, ~brightness,
                \orbitSpeed, (~orbitSpeed * 0.75),
                \orbit, orbitRadB
            );
        };
        if(~satA.notNil) {
            ~satA.set(
                \amp, ~satAmp,
                \density, (~satDensity.linlin(0, 1, 0.06, 3.8)),
                \bright, ~brightness,
                \duckAmt, ~duckDepth,
                \orbitSpeed, (~orbitSpeed * 1.25),
                \orbit, orbitRadC
            );
        };
        if(~subMix.notNil) {
            ~subMix.set(\master, ~masterAmp, \duckDepth, ~duckDepth, \tone, ~brightness);
        };
        if(~safety.notNil) {
            ~safety.set(\post, (~masterAmp.clip(0.2, 1.2)));
        };
    };

    // --- Mod task (fix symbol access) ---
    ~modTask = nil;
    ~startModTask = {
        if(~modTask.notNil) { ~modTask.stop };
        ~modTask = Routine({
            loop {
                var dA, dB, dC, thin, dynRes;

                dA = ~baseA;
                dB = ~baseB;
                dC = ~airC;

                if(dA.notNil) { ~droneCtlBus.set(dA.amp.clip(0, 1)) } { ~droneCtlBus.set(~droneFundAmp.clip(0, 1)) };

                thin = (1 - ~machineDensity).clip(0, 1);
                dynRes = (0.18 + (~resonanceLift * thin)).clip(0.05, 1.15);

                if(dA.notNil) { dA.resonance_(dynRes, 2) };
                if(dB.notNil) { dB.resonance_((dynRes * 0.82).clip(0.05, 1), 2) };
                if(dC.notNil) { dC.resonance_((dynRes * 1.05).clip(0.05, 1.2), 2) };

                ~sharedAngle = (~sharedAngle + (~orbitSpeed * 32)).wrap(0, 360);
                if(dA.notNil) { dA.angle_(~sharedAngle) };
                if(dB.notNil) { dB.angle_((~sharedAngle + 120).wrap(0, 360)) };
                if(dC.notNil) { dC.angle_((~sharedAngle + 240).wrap(0, 360)) };

                ~applyControls.();
                1.5.wait;
            }
        }).play(SystemClock);
    };

    // --- Build layers ---
    ~buildCustomLayers = {
        ~machineA = Synth.head(~srcGroup, \vsMachineIrregular, [\out, ~machineBus, \ctlOut, ~machineCtlBus]);
        ~machineB = Synth.after(~machineA, \vsMachineSub, [\out, ~machineBus]);
        ~satA     = Synth.after(~machineB, \vsSatelliteField, [\out, ~satBus, \machineCtl, ~machineCtlBus, \droneCtl, ~droneCtlBus]);
        ~subMix   = Synth.head(~fxGroup, \vsSubMix, [\out, 0, \inMachine, ~machineBus, \inSat, ~satBus]);
        ~safety   = Synth.tail(~fxGroup, \vsSafety, [\bus, 0]);
        ~applyControls.();
    };

    // --- STOP ---
    ~stopSet = { |fade=12|
        if(~scoreTask.notNil) { ~scoreTask.stop; ~scoreTask = nil };
        if(~modTask.notNil)   { ~modTask.stop;   ~modTask = nil };

        [~machineA, ~machineB, ~satA].do { |syn| if(syn.notNil) { syn.set(\gate, 0) } };

        AppClock.sched(fade + 0.2, {
            [~subMix, ~safety].do { |x| if(x.notNil) { x.free } };
            ~drones.killAll(fade.max(2));
            nil
        });
    };

    // --- SCORE (fix createDrone/createSatellites signatures + backslashes) ---
    ~score = [
        [0, {
            ~drones.killAll(3);

            ~drones.tuning_(\just);
            ~drones.scale_(\dorian);

            // createDrone(type, tonic, ...) [web:17]
            ~drones.createDrone(\saw, 1, harmonics: 4, amp: 0.20, speed: 42, length: 145, angle: 10, degree: 1, ratio: 1, env: [8,16], octave: 1, name: \baseA);
            ~drones.createDrone(\tri, 2, harmonics: 3, amp: 0.15, speed: 27, length: 170, angle: 130, degree: 5, ratio: 1, env: [10,18], octave: 1, name: \baseB);
            ~drones.createDrone(\sine, 1, harmonics: 2, amp: 0.10, speed: -19, length: 210, angle: 240, degree: 9, ratio: 1, env: [9,14], octave: 2, name: \airC);

            // Satellites: use ratios: ... (reference) [web:31]
            ~drones.createSatellites(\pulse,
                ratios: [1,2,3,5,8,9],
                tonic: 2,
                harmonics: 2,
                amp: 0.05,
                speed: 30,
                length: 60,
                angle: 22,
                num: 9,
                spread: 1.2,
                env: [4,8],
                name: \orbA
            );

            ~machineDensity = 0.22;
            ~satDensity = 0.20;
            ~brightness = 0.28;
            ~duckDepth = 0.25;
            ~orbitSpeed = 0.020;
            ~applyControls.();
        }],

        // ... (rest of your score events can stay, but fix all \\symbols -> \symbols,
        // and every createDrone must use tonic as 2nd arg, not tonic: keyword) [web:17]

        [1800, { ~stopSet.(20); }]
    ];

    // --- Score player ---
    ~scoreTask = nil;
    ~playScore = {
        var sorted = ~score.sort({ |a, b| a[0] <= b[0] });
        var last = 0;
        if(~scoreTask.notNil) { ~scoreTask.stop };

        ~scoreTask = Routine({
            sorted.do { |ev|
                (ev[0] - last).max(0).wait;
                ev[1].value;
                last = ev[0];
            }
        }).play(SystemClock);
    };

    // --- CmdPeriod hook (add once per run) ---
    if(~cmdPeriodHook.notNil) { CmdPeriod.remove(~cmdPeriodHook) };
    ~cmdPeriodHook = { ~stopSet.(~globalRelease) };
    CmdPeriod.add(~cmdPeriodHook);

    // --- START ---
    ~startSet = {
        ~stopSet.(4);
        AppClock.sched(4.2, {
            ~buildCustomLayers.();
            ~startModTask.();
            ~playScore.();
            nil
        });
    };

    ~startSet.();
});
)
