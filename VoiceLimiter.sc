
// Based on Batuhan Bozkurt's 2009 StageLimiter class

VoiceLimiter { 
	classvar lmFunc, activeSynth;
	
	*activate {
		
		fork{
			lmFunc = { { 
				activeSynth = Synth(\voiceLimiter, target: RootNode(Server.default), addAction: \addToTail) }.defer(0.01)};
			lmFunc.value;
			//CmdPeriod.add(lmFunc);
			"VoiceLimiter active".postln;
		}
	}
	
	*deactivate {
		activeSynth.free;
		//CmdPeriod.remove(lmFunc);
		"VoiceLimiter inactive...".postln;
	}
}
