// scales: http://www.looknohands.com/chordhouse/piano/


// xCoAx -> 1400*1050


// system
// TODO: implement a parameter-to-midi feature where a specific parameter of a voice can be assigned to a slider or a knob. (-> this should be stored in states, such that a setup can be recalled with parameters etc.)
// TODO: make an UNDO function
// TODO: make the system argument save (if use writes wrong types) (eg. createDrone(\saw, \minor, 2))


// sound

// TODO: Add Lag Ugens to detune and other ~drn.set() arguments
// TODO: Cube sinewaves in the voices SinOsc.ar().cub
// TODO: oscillating amp.
// TODO: fix the perceived loudness when using resonace !!!
// TODO: create a DynKlank and DynKlang synthdefs
// TODO: Explore "harmonic stretching" as in the harmonics of a piano (8th harmonic stretched) Here I can use the Drag Handle (as in recParam) to tune the harmonic
// TODO: explore the use of lissajous curves to explore harmonic relationships
// TODO: create a noise Voice. But using BPF or FFT brick wall?
// TODO: add LFNoise2 into all frequency args.
// ToDO: Check amplitude argument for sample instruments. Also check RedFrik Phahsor for playing them
// i.e., fix the tonic_(1, 20) glitches that will be heard when the pitch is changed.
// a saw wave version
// play{{({|i|x=i+1;y=LFNoise2.ar(0.1);f=77*(x/2)+y.range(-2,2);LPF.ar(Saw.ar(f,y*(Line.ar(0,0.2,99.rand)/(x*0.3))),f*3)}!rrand(9,28)).sum}!2}
// TODO: add tremolo to synths


// voices
// TODO: user specified Chords into chorddict
// TODO: add a spectrogram view
// TODO: expose voice parameters down to a MIDI interface

// score
// TODO: make it possible to start multiple scores, and stop a unique one
// TODO: chords and satellites into the voicescore (as a line representation)
// TODO: Fix problem if one tries to swap state with the same state.
// Having removed the opInt - addToScore needs to be added for all relevant methods
// BUG: Machines cannot be created from a score (cannot be called from this process)
// TODO: solve problem with .interpret code (Try e.g., the score for Lauren, \s1Intro)
// TODO: .killAll needs to go and send a .kill message to all voices, thus ending them on the timeline
// TODO: if voicescore is playing without view, and the view is started, then start timeline at location

// TODO: 	loadsamples here : VoiceSynths.new(loadsamples: false);
// TODO: ~voices.killAll should kill running scores too

// machines
// TODO: fix them all!


// TODO: allow for frequency creation argument, as in ~voices.createDrone(\saw, freq: 432)
/// TODO: Make ~shf.freq post the frequency (or any other args) into the freq window

// Display Modes:
// perform
// dev
// displayFS (full screen)
// displayWin (window)

// BUG TO FIX -> When running a score, you can't kill Satellites
// PlayBufCF & PlayBufAlt in the wslib Quark. Crossfade and back-and-forth playback.�

// TODO: Add literal arrays where appropriate

/*
Lukas:
I just had another �feature suggestion� for the VoiceScope in mind that I forgot to ask you about earlier: I would find it very useful to be able to change the curve of interpolation between parameters. So when calling for example �~bob.harmonics_(10, 30)�, you could add something like �.exp�, �.sqr�, �.cub� to interpolate exponentially/inverted exponentially, cubicly etc.. That seems like an important compositional parameter when dealing with these kinds of forms.

*/



VoiceScope {
	var window, screendimension;
	var hub;

	*new { arg channels=2, mode=\perform, key="A", appPath;
		^super.new.initVoiceScope(channels, mode, key, appPath);
	}

	initVoiceScope { | channels, mode, key, appPath |
		
		var fundamental;
		var speakers, interpreter, machines, states;
		var voices, env;
		var tuning = \et12;
		var scale = \minor;
		// var mode = argmode;
		//var channels = argchannels;
		var voicescopeColor;
		var border, fullScreen, bgcolor;

		//GUI.cocoa; // use this if on SC3.6-
		//GUI.qt; // use this if on SC3.7+

		
		if(appPath.isNil) {
			// Opzione A: Usa il sistema Quarks (Più sicuro se il quark è installato correttamente)
			var q = Quarks.all.detect { |q| q.name.asString == "voicescopeSC" }; // Assicurati che "voicescopeSC" sia il nome esatto della cartella/quark
			if(q.notNil) {
				appPath = q.localPath;
			} {
				// Opzione B: Fallback basato sulla posizione del file
				// Tenta di risalire finché non trova la cartella "voicescopeSC"
				appPath = this.class.filenameSymbol.asString.dirname;
                // Se il file è dentro una cartella 'Classes' o 'voicescope', risaliamo ancora
                if(appPath.endsWith("Classes")) { appPath = appPath.dirname };
                // Aggiungi altri controlli se necessario, ma di solito basta questo
			};
			
			"App path auto-detected: %".format(appPath).postln;
		};
		
		if((Main.scVersionMajor<=3) && (Main.scVersionMinor<=6) && (thisProcess.platform.name == \osx), {
			"USING COCOA GUI on old versions of SuperCollider".postln;
			GUI.cocoa;
		});


		screendimension = Window.screenBounds.height;

		switch(mode)
			{\perform} { // regular perform mode with terminal and console
				border = false;
				fullScreen = true;
				bgcolor = Color.black;
				}
			{\performWin} { // regular perform mode with terminal and console but in a Window
				border = true;
				fullScreen = false;
				bgcolor = Color.black;
				}
			{\dev} { // development mode
				border = true;
				fullScreen = false;
				bgcolor = Color.black;
				}
			{\displayFS} { // full screen for display (for non-interactive performance)
				border = false;
				fullScreen = true;
				bgcolor = Color.black;
				}
			{\displayWin} { // display in a window (for app playback)
				border = true;
				fullScreen = false;
				bgcolor = Color.black;
			};

		window = Window("VoiceScope", Rect(0, 0, screendimension, screendimension), resizable:true, border:border).front;
		window.view.onResize = { |view| hub.resize(view.bounds) };

		if(fullScreen, { window.fullScreen });

		window.view.background = bgcolor;
		voicescopeColor = Color.black; // sfondo quadrante grafico

		if(key.isNumber,{
			fundamental = key;
		},{
			fundamental = (\C:24, \Cs:25, \Db:25, \D:26, \Ds:27, \Eb:27, \E:28, \F:29, \Fs:30, \Gb:30, \G:31, \Gs:32, \A:33, \As:34, \Bb:34, \B:35, \C2:36, \Cs2:37, \Db2:37, \D2:38, \Ds2:39, \Eb2:39).at(key.asSymbol).midicps;
		});
//		fundamental = (\C:24, \Cs:25, \Db:25, \D:26, \Ds:27, \Eb:27, \E:28, \F:29, \Fs:30, \Gb:30, \G:31, \Gs:32, \A:33, \As:34, \Bb:34, \B:35, \C2:36, \Cs2:37, \Db2:37, \D2:38, \Ds2:39, \Eb2:39).at(key.asSymbol).midicps;

		// fundamental = 36.midicps; // in C
		// fundamental = 40.midicps; // in E
		// fundamental = (49-12).midicps; // in Es for bass sax (w. Inigo Ibaibarriaga)
		// fundamental = 34.midicps; // in Bb for bass clarinet (w. Pete Furniss)
		// fundamental = 58.2; // in Bb for bass clarinet (w. Pete Furniss)
		// fundamental = 55; // + (20.rand); // remove rand (just here to remove stress from ears)
		// fundamental = 53.6 + (20.rand); // remove rand (just here to remove stress from ears)
	 	// fundamental = 54; // this is if A = 432 Hz. A popular 19th century tuning.
		// fundamental = 30.midicps; // in F# (with Adriana Sa)
		// fundamental = 28.midicps;

		//fundamental = 55;


		// VoiceLimiter.activate;
		// Voice - the key class of each voice
		// VoiceMachine - the class containing the diverse machines
		// VoiceGUI - an experiment with GUI
		// VoiceChord / VoiceSatellites / VoiceGroup
		// VoiceCodeScore - an experiment with Score

		hub = VoiceHub.new( window, mode, scale, fundamental, voicescopeColor, key, channels, appPath ); // - all key data accessible to other classes
		env = hub.env ? currentEnvironment;
		env.put(\voicehub, hub);

		VoiceSynths.new(false, hub);

		states = VoiceStates.new( hub ); // - storing states and recording/playing scores

		hub.registerStates( states );
		speakers = VoiceSpeakers.new( hub, channels, fundamental ); // drawing background
		hub.registerSpeakers( speakers );
		voices = VoiceController.new( hub, tuning, scale, fundamental); // - main interface
		env.put(\voices, voices);
		{
			var proxyClass = \ProxySpace.asClass;
			if(proxyClass.notNil and: { currentEnvironment.isKindOf(proxyClass) }) {
				currentEnvironment.envir.put(\voices, voices);
				currentEnvironment.envir.put(\voicehub, hub);
			};
		}.value;
		hub.registerVoices( voices );

		if((mode == \perform) || (mode == \performWin) || (mode == \dev), {
			interpreter = VoiceInterpreter.new( hub, mode:mode); // - the 'console' for live coding
			hub.registerInterpreter( interpreter );
		});
	}

/*
	mode {arg mode;
		this.quit;
		this.initVoiceScope(hub.channels, mode, hub.key, hub.appPath);
	}
*/

	quit {
		if(hub.notNil and: { hub.voices.notNil }, { hub.voices.killAll });
		window.close;
	}
}
