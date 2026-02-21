# VoicescopeSC
SuperCollider source code for the Voicescope (Fork of ThrenoScope)

Author: Lorenzo Antei (Original code by Thor Magnusson)

Voicescope is a droning live coding ambient system, refactored to be independent from the original ThrenoScope.

Plese note that [sc3-plugins](https://supercollider.github.io/sc3-plugins/) must be installed in order for the Voicescope to work.

Enjoy!

## Installation Guide
Inside Supercollider type and evaluate:

```SuperCollider
Quarks.install("https://github.com/lorenzoantei/Voicescopesc.git");
```
Recompile the class library.

Inside SuperCollider type and evaluate: 

```SuperCollider
s.boot;
VoiceScope.new(2);
```

If everything works correctly this should launch the Voicescope!
Type this line inside the Voicescope editor to check if everything works:

```
~voices.createDrone(\saw, 2, 3);
```

Now voice on!
