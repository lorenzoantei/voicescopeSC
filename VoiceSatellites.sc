
VoiceSatellites {

	var <>voicearray;
	var <selected;
	var hub, name;
	var scale, chord, tonality, transposition, spread;
	var number, tonic, rat; 
	
	*new { | hub, name |
		^super.new.initSatellites( hub, name );
	}

	initSatellites { | arghub, argname |
		hub = arghub;
		name = argname;
	}
	
	// ratios are scale ratios, i.e., 
	createSatellites { | type=\sine, ratios=#[1,2,3,4,5,6,7,8,9,10,11,12], tonic=1, harmonics, amp=0.2, speed, length, angle, num=20, argspread, env, octave |
		var voice;
		number = num;
		selected = false;
		spread = argspread;
		if(angle == 0, { angle = nil }); // allow for the angle slider to set random angles (0=rand)
		voicearray = [];
		if(ratios.isArray, { // it's a custom scale
			scale = ratios;
		}, {  // it's a scale from the Scale class
			scale = Scale.newFromKey(ratios).degrees+1; // indexing by 1
		});		
		rat = [];
		spread.do({|i| rat = rat.add(scale*(i+1)) });
		rat = rat.flatten;
		num.do({arg i;
			var tonicArg, angleArg, lengthArg, speedArg, harmonicsArg, ratioArg;
			angleArg = angle.value ? 360.rand;
			speedArg = speed.value ? 30.0.rand2; // speed.value allow for a function to be passed as arguments
			lengthArg = length.value ? (4+(4.0.rand));
			tonicArg = tonic.value ? (2+(14.rand));
			harmonicsArg = harmonics.value ? (1+(4.rand));
			voice = hub.voices.createDrone( type, tonicArg, harmonicsArg, amp, speedArg, lengthArg, angleArg, 1, rat.choose, name++"_"++(1+i), env, octave, post:false );
			voicearray = voicearray.add(voice);
		});			
	}

	num_ { |num|
		var diff, voice;
		diff = num - number;
		number = num;
		if(diff > 0, {
			diff.do({ |i|
				var tonicArg, angleArg, lengthArg, speedArg, harmonicsArg, ratioArg, env, octave, amp, type;
				angleArg = 360.rand;
				speedArg = 30.0.rand2;
				lengthArg = (4+(4.0.rand));
				tonicArg = voicearray.choose.tonic ? (2+(14.rand));
				harmonicsArg = voicearray.choose.harmonics;
				env = voicearray.choose.env;
				amp = voicearray.choose.amp;
				octave = voicearray.choose.octave;
				type = voicearray.choose.type;
				voice = hub.voices.createDrone( type, tonicArg, harmonicsArg, amp, speedArg, lengthArg, angleArg, 1, rat.choose, name++"_"++(1+i+diff), env, octave, post:false );
				
				voicearray = voicearray.add(voice);
			});
		}, {
			voicearray[num..].do({ |voice| voice.kill});
			voicearray = voicearray[0..num-1];
		});
		
	}
	
	spread_ { |argspread|
		var rat;
		rat = [];
		spread = argspread;
		spread.do({|i| rat = rat.add(scale*(i+1)) });
		rat = rat.flatten;
		voicearray.do({ |voice| voice.ratio_(rat.choose) });
	}
	
	// satellites can be either in chords or scales
	changeScale { | argscale, dur, transp=0 |
		var thisratios, rat;
		tonality = argscale;
		transposition = transp;
		if(argscale.isArray, { // its a custom scale
			scale = argscale;
		}, {  // it's a scale from the dict
			scale = Scale.newFromKey(argscale).degrees+1; // indexing by 1
		});		
		rat = [];
		spread.do({|i| rat = rat.add(scale*(i+1)) });
		rat = rat.flatten;
		voicearray.do({ | voice | voice.ratio_( rat.choose+transp, dur ) });
	}

	// satellites can be either in chords or scales
	changeChord { | argchord, dur=nil, transp=0  |
		var thisratios, rat;
		tonality = argchord;
		transposition = transp;
		if(argchord.isArray, { // its a custom scale
			chord = argchord;
		}, {  // it's a chord from the dict
			chord = hub.getChordDict[argchord]+1; // indexing by 1
		});		
		rat = [];
		spread.do({|i| rat = rat.add(chord*(i+1)) });
		rat = rat.flatten;
		voicearray.do({ | voice | voice.ratio_( rat.choose+transp, dur ) });
	}
	
	tonality {
		^("Satellite tonality is : " ++ tonality);
	}

	selected_ { | bool |
		selected = bool;
		voicearray.do({ |voice| voice.selected = bool });
	}

	freeSynths { | releasetime |
		voicearray.do({arg voice; voice.freeSynths( releasetime ) });
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
	tuning_ { |tuning, dur | voicearray.do({arg voice; voice.tuning_(tuning, dur) }) } // working
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
	kill { |releasetime| hub.voices.killSatellite(name); voicearray.do({arg voice; voice.kill(releasetime) }) }
	auto_ { |bool| voicearray.do({arg voice; voice.auto_(bool) }) }
	clearauto { voicearray.do({arg voice; voice.clearauto() }) }
	recParam {| method, min, max, round=0 | hub.voices.rec_(voicearray, method, min, max, round) }
	setParam {| method, min, max, round=0 | hub.voices.setParam_(voicearray, method, min, max, round) }
	stopParam { | method | voicearray.do({arg voice; voice.stopParam(method) }) }

	perform { arg command;
		if(command.contains("changeScale") || command.contains("changeChord") || command.contains("type"), {
			var combinedstring; // = ("this"++command);
			combinedstring = ("~voices.satellitesDict[\\"++name++"]"++command); // accessing this through the dict (since this. doesn't work)
			combinedstring.interpret;
			// hub.states.addToScore_(combinedstring); // the below will do this
		}, {
			voicearray.do({arg voice;
				var combinedstring, interprstr;
				combinedstring = ("~voices.voiceDict[\\"++voice.name++"]"++command);
				combinedstring.postln;
				try{ 
					interprstr = combinedstring.interpret;
					hub.states.addToScore_(combinedstring);
				}{ "\nThis satellite exists, but there is a syntax error".postln };
			});
		});
	}	
}
