
VoiceChord {

	var <>voicearray;
	var <selected;
	var hub, <name;
	var chordtype, chordratios;
	var <tuning, <scale, scaledegrees; // scale is used for chord progressions
	
	*new { | hub, name, scale |
		^super.new.initVoiceChord( hub, name, scale );
	}

	initVoiceChord { | arghub, argname, argscale |
		hub = arghub;
		name = argname;
		scale = argscale;
	}
	
	createChord { | type=\saw, chord=#[1,5,8], tonic=1, harmonics, amp=0.2, speed, length, angle, degree=1, ratio=1, env, octave, note |

		var voice;
		chordtype = chord; // for outside info, see chord method below
		selected = false;
		voicearray = [];
		if(chord.isArray, { // its a custom chord
			//rat = "";
			//chord.do({arg i; rat = rat++i.asString });
			chordratios = chord;
		}, {  // it's a chord from the dict
			chordratios = hub.getChordDict.at(chord.asSymbol)+1; // indexing by 1
		});
		
		if(note.isNil.not, {
			[\hubkey, hub.key].postln;
			[\note, note].postln;
			ratio = (\A:1, \As:2, \Bb:2, \B:3, \C:4, \Cs:5, \Db:5, \D:6, \Ds:7, \Eb:7, \E:8, \F:9, \Fs:10, \Gb:10, \G:11, \Gs:12, \Ab:12).at(note.asSymbol);
			[\ratio, ratio].postln;
			ratio = (ratio - (\A:0, \As:1, \Bb:1, \B:2, \C:3, \Cs:4, \Db:4, \D:5, \Ds:6, \Eb:6, \E:7, \F:8, \Fs:9, \Gb:9, \G:10, \Gs:11, \Ab:11).at(hub.key.asSymbol)).mod(12);
			[\ratio, ratio].postln;
		});
		
		// a chord can be created in different ratios (which are halftones by default)
		chordratios.do({arg chordratio, i;
			var angleArg, lengthArg, harmonicsArg, voice;
			angleArg = angle.value ? 360.rand; // angle.value allows for functions to be passed, e.g. {rrand(100,190)}
			lengthArg = length.value ? (100+(160.rand));
			harmonicsArg = harmonics.value ? (2+(3.rand));

			voice = hub.voices.createDrone( type, tonic, harmonicsArg, amp, speed, lengthArg, angleArg, degree, chordratio+ratio-1, name++"_"++chordratio, env, octave, post:false );
			voice.chord_(chordratios); // make the voice aware of which chord it is a member of
			voicearray = voicearray.add(voice);
		});
		this.scale_(scale); // set the scaledegrees (if chord is to be harmonically transposed)
	}
	
	// define a chord from existing voices
	defineChord { | array | // XXX - TODO- is this working?
		Post << array;
		voicearray = array;
		voicearray.do({ | voice | voice.name.postln });
	}
	
	selected_ { | bool |
		selected = bool;
		voicearray.do({ |voice| voice.selected = bool });
	}
	
	printVoiceArray {
		voicearray.do({arg voice; voice.name.println });
	}
	
	type {
		^("Chord type is : " ++ chordtype);
	}
	
	changeChord { | chord, dur, transp=0 |
		if(chord.isArray, { // its a custom chord
			chordratios = chord;
		}, {  // it's a chord from the dict
			chordratios = hub.getChordDict.at(chord.asSymbol)+1; // indexing by 1
		});
		
		if(chordratios.size == voicearray.size, {
			"--> SAME SIZE".postln;
			chordratios.do({ | ratio, i |
				voicearray[i].ratio_(ratio+transp, dur);
			});
		}, {
			if(chordratios.size < voicearray.size, { // if there are more voices than tunings 
				voicearray.copy.do({ | voice, i |
					if(chordratios[i].isNil, {
						"--> KILLING DRONE".postln;
						if(dur.isNil.not, { // if it's a time-change chord
							voice.amp_(0, dur); // and fade out
							//voice.ratio_(chordratios[i]+transp, dur);
							{var drname;
							drname = voice.name;
							voice.kill;
							voicearray.copy.do({ | voice, i |
								if(voice.name == drname, {
									voicearray.removeAt(i)
								});
							});
							}.defer(dur);
						}, {
							var drname;
							drname = voice.name;
							voice.kill;
							voicearray.copy.do({ | voice, i |
								if(voice.name == drname, {
									voicearray.removeAt(i)
								});
							});
						});
					}, {
						voice.ratio_(chordratios[i]+transp, dur);
					});
				});				
			}, {
				"--> YES - MORE RATIOS".postln;
				chordratios.do({ | ratio, i |
					if(voicearray[i].isNil, {
						var voice;
						"--> ADDING DRONE".postln;
						[\name, name].postln;
						voice = hub.voices.createDrone(
											voicearray[0].type,
											voicearray[0].tonic,
											voicearray[0].harmonics,
											voicearray[0].amp,
											voicearray[0].speed,
											rrand(100, 300),
											360.rand,
											1, // degree
											ratio, 
											name++"_"++ratio, 
											voicearray[0].env,
											voicearray[0].octave,
											post:false
										);
						voicearray = voicearray.add( voice );
						if(dur.isNil.not, { // if it's a time change chord
							voice.amp_(0); // start new voice with 0 vol
							voice.amp_(voicearray[0].amp, dur); // and fade in
							voice.ratio_(ratio+transp, dur);
						});
					}, {
						voicearray[i].ratio_(ratio+transp, dur);
					});
				});				
			});
		});
		
		// check if the agent is a chord
		// if a chord, then move to the new tuningratios
		// if there are more or fewer notes, then add or delete
		
	}
	
	// in many of these I need to calculate from the offset
	
	env_ { |envt| voicearray.do({arg voice; voice.env = envt }) }
	tonic_ { |tonic,dur| voicearray.do({arg voice; voice.tonic_(tonic, dur) }) }
	relTonic_ { |change, dur| voicearray.do({arg voice; voice.relTonic_(change, dur) }) }
	freq_ { |freq, dur| 
		var fundamental, ratio;
		fundamental = voicearray.collect({|voice| voice.freq }).minItem;
		voicearray.do({arg voice; 
			ratio = voice.freq / fundamental;
			voice.freq_(freq*ratio, dur);
		}) 
	}// not working on a chord
	relFreq_ { |change, dur| voicearray.do({arg voice; voice.relFreq_(change, dur) }) }
	
	ratio_ { |ratio, dur, harmonic=false| 
		var offsetratio;
		chordratios.do({ |pureratio, i|
			offsetratio = pureratio+ratio-1;
			if(harmonic, {offsetratio = (offsetratio-0.2).nearestInList(scaledegrees) });
			voicearray[i].ratio_(offsetratio, dur);
		});
	} // works
	
	relRatio_ { |change, dur, harmonic=false| 
		voicearray.do({arg voice; voice.ratio_(change+voice.ratio, dur, harmonic) }) 
	} // works
	
	note_ { |note, dur, harmonic=false| 
		var offsetratio, ratio;
			[\hubkey, hub.key].postln;
			[\note, note].postln;
			ratio = (\A:1, \As:2, \Bb:2, \B:3, \C:4, \Cs:5, \Db:5, \D:6, \Ds:7, \Eb:7, \E:8, \F:9, \Fs:10, \Gb:10, \G:11, \Gs:12, \Ab:12).at(note.asSymbol);
			[\ratio, ratio].postln;
			ratio = (ratio - (\A:0, \As:1, \Bb:1, \B:2, \C:3, \Cs:4, \Db:4, \D:5, \Ds:6, \Eb:6, \E:7, \F:8, \Fs:9, \Gb:9, \G:10, \Gs:11, \Ab:11).at(hub.key.asSymbol)).mod(12);
			if(ratio == 0, {ratio = 12});
			[\ratio, ratio].postln;
		
		chordratios.do({ |pureratio, i|
			offsetratio = pureratio+ratio-1;
			if(harmonic, {offsetratio = (offsetratio-0.2).nearestInList(scaledegrees) });
			voicearray[i].ratio_(offsetratio, dur);
		});
	} 
		
	degree_ { |argdegree, dur, harmonic=false| // harmonic is the argument for staying in scale (for chord progressions) 
		var offsetratio, ratio, degree;
		degree = argdegree.max(1).mod(scaledegrees.size);
		ratio = scaledegrees[degree]; // get the tuning ratio for that degree (note)
		chordratios.do({ |pureratio, i|
			offsetratio = pureratio+ratio-1;
			if(harmonic, {offsetratio = (offsetratio-0.2).nearestInList(scaledegrees) });
			voicearray[i].ratio_(offsetratio, dur);
		});
	} 
	relDegree_ { |change, dur, harmonic=false| voicearray.do({arg voice; voice.degree_(change+voice.degree, dur, harmonic) }) } // works

	// playRatios { |dur, slide| voicearray.do({arg voice; voice.playRatios(dur, slide) }) }
	// playDegrees { |dur, slide| voicearray.do({arg voice; voice.playDegrees(dur, slide) }) }
	// playScale { |dur, slide| voicearray.do({arg voice; voice.playScale(dur, slide) }) }
	octave_ { |octave, dur| voicearray.do({arg voice; voice.octave_(octave, dur) }) }
	relOctave_ { |change, dur| voicearray.do({arg voice; voice.relOctave_(change, dur) }) }
	transpose_ { |interval, dur| voicearray.do({arg voice; voice.transpose_(interval, dur) }) }
	interval_ { |interval, dur| voicearray.do({arg voice; voice.interval_(interval, dur) }) }
	
	harmonics_ { |harmonics, dur| voicearray.do({arg voice; voice.harmonics_(harmonics, dur) }) }
	resonance_ { |res, dur| voicearray.do({arg voice; voice.resonance_(res, dur) }) }
	amp_ { |amp, dur| voicearray.do({arg voice; voice.amp_(amp, dur) }) } // working
	amp { ^voicearray[0].amp } 
	relAmp_ { |change, dur| voicearray.do({arg voice; voice.relAmp_(change, dur) }) } // working
	speed_ { |speed| voicearray.do({arg voice; voice.speed_(speed) }) } // working
	relSpeed_ { |change| voicearray.do({arg voice; voice.relSpeed_(change) }) } // working
	angle_ { |angle| voicearray.do({arg voice; voice.angle_(angle) }) } // working
	length_ { |length| voicearray.do({arg voice; voice.length_(length) }) } // working
	relLength_ { |change| voicearray.do({arg voice; voice.relLength_(change) }) } // working
	tuning_ { |argtuning, dur| tuning = argtuning; voicearray.do({arg voice; voice.tuning_(tuning, dur) }) } // working
	scale_ { |scale| 
		var scala, scl, semitones, octaveRatio, scalesize;
		scala = false; // by default expecting an SC scale, not Scala (Fokker) - see VoiceScales class
		if(scale.isArray, {
			semitones = scale;
		}, {
			scl = Scale.newFromKey(scale.asSymbol); // this will post a warning if it's a Scala scale
			octaveRatio = scl.tuning.octaveRatio; // this is for non-octave repeating scales such as Bohlen Pierce 3:1 'tritave' scale
			if(scl.isNil, { // support of the Scala scales
				scala = true;
				scl = VoiceScale.new(scale); 
				"This is a Scala scale".postln;
				this.tuning_(scale); // the Scala scales are also tunings
			}); 
			semitones = scl.semitones;
		});
		scalesize = semitones.size; // needed from VoiceMachines
		scaledegrees = Array.fill(5, {|i| semitones+(i*12)+1 }).flatten; // add 1 because of indexing from 1
		scaledegrees = scaledegrees.insert(0, 0); // put a zero at the beginning so I can index from 1
		
		voicearray.do({arg voice; voice.scale_(scale) });
	}
	chord_ { |chord, dur, transp=0| // transposition in ratios (half-notes if et12)
		this.changeChord(chord, dur, transp);
		voicearray.do({arg voice; voice.chord_(chord) }); // the chord of a voice
	}  
	type_ { |type| voicearray.do({arg voice; voice.type_(type) }) }
	set { | ...args | voicearray.do({arg voice; voice.set(*args) }) }
	freeSynths { |releasetime| voicearray.do({arg voice; voice.freeSynths(releasetime) }) } // the chord of a voice 
	kill { |releasetime| hub.voices.killChord(name); voicearray.do({arg voice; voice.kill(releasetime) }) }
	auto_ { |bool| voicearray.do({arg voice; voice.auto_(bool) }) }
	clearauto { voicearray.do({arg voice; voice.clearauto() }) }
	recParam {| method, min, max, round=0 | hub.voices.rec_(voicearray, method, min, max, round) }
	setParam {| method, min, max, round=0 | hub.voices.setParam_(voicearray, method, min, max, round) }
	stopParam { | method | voicearray.do({arg voice; voice.stopParam(method) }) }

	// Chord specific MIDI interface listener
	addMIDI { | transp=0, dur, harmonic=false | // this chord will listen to MIDI messages
		if(hub.midi == false, { MIDIIn.connectAll });
		MIDIdef.noteOn(this.name, {arg ...args;
			{
				this.ratio_(args[1]-11, dur, harmonic);
				this.amp_(args[0]/127, dur);
			}.defer;
		});
	}
	
	removeMIDI {
		MIDIdef(this.name).free;
	}

	perform { arg command;
		if(command.contains("changeChord") || command.contains("type"), { // methods of this class (not its voices)
			var combinedstring; // = ("this"++command);
			combinedstring = ("~voices.chordDict[\\"++name++"]"++command); // accessing this through the dict (since this. doesn't work)
			combinedstring.interpret;
		}, {
			voicearray.do({arg voice;
				var combinedstring, interprstr;
				combinedstring = ("~voices.voiceDict[\\"++voice.name++"]"++command);
				combinedstring.postln;
				try{ 
					interprstr = combinedstring.interpret;
					hub.states.addToScore_(combinedstring);
				}{ "\nThis chord exists, but there is a syntax error".postln };
			});
		});
	}
}
