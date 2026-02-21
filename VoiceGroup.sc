
VoiceGroup {

	var <>voicearray;
	var <selected;
	var hub, name;
	
	*new { | hub, name |
		^super.new.initVoiceGroup( hub, name );
	}

	initVoiceGroup { | arghub, argname |
		name = argname;
		hub = arghub;
	}

	createGroup { | argname, type, tonic, harmonics, amp, speed, length, angle, degree, ratio, env, octave  | // instantiate a new group (not define it) - as in createDrone, createChord
		var grp, voice;
		var tonicoffset, degreeoffset, ratiooffset, octaveoffset; // pitch offsets (os_)
		voicearray = [];
		selected = false;	
		grp = hub.states.storedGroupsDict.at(argname); // a dict with voices (name + synthParams)
		if(tonic.isNil.not, {
			tonicoffset = tonic - grp.keys.asArray.collect({ | key | grp[key].tonic }).minItem;
		});
		\dev1.postln;
		if(degree.isNil.not, {
			degreeoffset = degree - grp.keys.asArray.collect({ | key | grp[key].degree }).minItem;
		});
		\dev2.postln;
		if(ratio.isNil.not, {
			ratiooffset = ratio - grp.keys.asArray.collect({ | key | grp[key].ratio }).minItem;
		});
		\dev3.postln;
		if(octave.isNil.not, {
			octaveoffset = octave - grp.keys.asArray.collect({ | key | grp[key].octave }).minItem;
		});
		[\KEYS, grp.keys].postln;
		grp.keys.do({ | key , i|
			[\key, key].postln;
			voice = hub.voices.createDrone(
				if(type.isNil, {grp[key].type}, {type}), 
				if(tonic.isNil, {grp[key].tonic}, {grp[key].tonic+tonicoffset}), 
				if(harmonics.isNil, {grp[key].harmonics}, {harmonics.value}),  // the value here allows for passing functions, like in Voice, Chord, etc.
				if(amp.isNil, {grp[key].amp}, {amp.value}), 
				if(speed.isNil, {grp[key].speed}, {speed.value}), 
				if(length.isNil, {grp[key].length}, {length.value}), 
				if(angle.isNil, {grp[key].angle}, {angle.value}), 
				if(degree.isNil, {grp[key].degree}, {grp[key].degree+degreeoffset}), 
				if(ratio.isNil, {grp[key].ratio}, {grp[key].ratio+ratiooffset}), 
				if(true, {name++"_"++grp[key].name}, {name}), // XXX FINISH
				if(env.isNil, {grp[key].env}, {env}), 
				if(octave.isNil, {grp[key].octave}, {grp[key].octave+octaveoffset}),
				post:false
				);
				\dev4_44.postln;
			voicearray = voicearray.add( voice );
		});
		\dev5.postln;
	}

	// define a group from existing voices
	defineGroup { | ...args |
		var argsDict;
	
		voicearray = [];
		argsDict = ();
		args = args.flatten;
		args.do({ | voicename | 
			var voiceState;			
			voiceState = hub.voices.voiceDict[voicename.asSymbol].synthParams;
			voiceState.add('speed' -> (hub.voices.voiceDict[voicename.asSymbol].speed*1000));
			voiceState.add('length' -> (hub.voices.voiceDict[voicename.asSymbol].length * 360 / (2*pi) ));
			voiceState.add('angle' -> ((hub.voices.voiceDict[voicename.asSymbol].rotation * 360 / (2*pi)) + (hub.voices.voiceDict[voicename.asSymbol].length* 360 / (2*pi)) ));
			voiceState.add('ratio' -> hub.voices.voiceDict[voicename.asSymbol].ratio);
			voicearray = voicearray.add( hub.voices.voiceDict[voicename.asSymbol] );
			argsDict.add( voicename.asSymbol -> voiceState ) 
		});
		hub.states.storeGroup( name, argsDict );
	}

	selected_ { | bool |
		selected = bool;
		voicearray.do({ |voice| voice.selected = bool });
	}

	changeScale { | argscale, dur, transp=0 |
		var scale;
		if(argscale.isArray, { // its a custom scale
			scale = argscale;
		}, {  // it's a scale from the dict
			scale = Scale.newFromKey(argscale).degrees+1; // indexing by 1
		});		
		voicearray.do({ | voice | 
			voice.ratio_( voice.ratio.nearestInScale(scale)+transp, dur ) });
	}

	// groups can be either in chords or scales
	changeChord { | argchord, dur, transp=0  |
		var chord;
		if(argchord.isArray, { // its a custom scale
			chord = argchord;
		}, {  // it's a chord from the dict
			chord = hub.getChordDict[argchord]+1; // indexing by 1
		});		
		voicearray.do({ | voice |
			voice.ratio_( voice.ratio.nearestInScale(chord)+transp, dur ) });
	}

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
	ratio_ { |ratio, dur| 
		var fundamental, offset;
		fundamental = voicearray.collect({|voice| voice.ratio }).minItem;
		voicearray.do({arg voice; 
			offset = voice.ratio - fundamental;
			voice.ratio_(ratio+offset, dur) }) 
	} // works
	relRatio_ { |change, dur| voicearray.do({arg voice; voice.ratio_(change+voice.ratio, dur) }) } // works
	degree_ { |degree, dur| 
		var fundamental, offset;
		fundamental = voicearray.collect({|voice| voice.degree }).minItem;
		voicearray.do({arg voice; 
			offset = voice.degree - fundamental;
			voice.degree_(degree+offset, dur) }) 
	} 
	relDegree_ { |change, dur| voicearray.do({arg voice; voice.degree_(change+voice.degree, dur) }) } // works

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
	tuning_ { |tuning, dur| voicearray.do({arg voice; voice.tuning_(tuning, dur) }) } // working
	scale_ { |scale, dur, transp=0| 
		this.changeScale(scale, dur, transp);
		voicearray.do({arg voice; voice.scale_(scale) }); 
	}
	chord_ { |chord, dur, transp=0| // transposition in ratios (half-notes if et12)
		this.changeChord(chord, dur, transp);
		voicearray.do({arg voice; voice.chord_(chord) }); // the chord of a voice
	}  
	type_ { |type| voicearray.do({arg voice; voice.type_(type) }) }
	set { | ...args | voicearray.do({arg voice; voice.set(*args) }) }
	freeSynths { |releasetime| voicearray.do({arg voice; voice.freeSynths(releasetime) }) } // the chord of a voice 
	kill { |releasetime| hub.voices.killGroup(name); voicearray.do({arg voice; voice.kill(releasetime) }) }
	auto_ { |bool| voicearray.do({arg voice; voice.auto_(bool) }) }
	clearauto { voicearray.do({arg voice; voice.clearauto() }) }
	recParam {| method, min, max, round=0 | hub.voices.rec_(voicearray, method, min, max, round) }
	setParam {| method, min, max, round=0 | hub.voices.setParam_(voicearray, method, min, max, round) }
	stopParam { | method | voicearray.do({arg voice; voice.stopParam(method) }) }

	perform { arg command;
		if(command.contains("tonic"), { // an exeption - bad idea - should simply use relative tonic
			var combinedstring, group, tonicoffset, tonicshift, tonicmove, interprstr;
			tonicoffset = command[8..command.size].asInteger;
			tonicshift = voicearray.collect({arg voice, i; voice.tonic});
			tonicmove = tonicoffset-tonicshift.minItem;
			tonicshift = tonicshift+tonicmove;  // (the logic -> 4-[4, 2, 7].minItem; [4,2,7]+2)
			voicearray.do({arg voice, i;
				combinedstring = ("~voices.voiceDict[\\"++voice.name++"].tonic_("++tonicshift[i]++")");
				combinedstring.postln;
				try{ 
					interprstr = combinedstring.interpret;
					hub.states.addToScore_(combinedstring);
				}{ "\nThis group exists, but there is a syntax error".postln };
			});
		}, {
			voicearray.do({arg voice;
				var combinedstring, interprstr;
				combinedstring = ("~voices.voiceDict[\\"++voice.name++"]"++command);
				combinedstring.postln;
				try{ 
					interprstr = combinedstring.interpret;
					hub.states.addToScore_(combinedstring);
				}{ "\nThis chord exists, but there is a syntax error".postln };
			})
		})
	}
}
