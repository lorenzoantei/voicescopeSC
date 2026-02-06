// Forward messages from a NodeProxy to a non-UGen source object if applicable.
+ NodeProxy {
	doesNotUnderstand { |selector ... args|
		var src = this.source;
		if(src.notNil and: { src.respondsTo(selector) }) {
			^src.performList(selector, args);
		};
		^super.doesNotUnderstand(selector, args);
	}
}
