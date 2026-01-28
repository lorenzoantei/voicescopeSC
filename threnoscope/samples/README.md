# Samples README

This folder contains sample files and the `_samples.scd` dictionary used by
`DroneSynths`/`Drone` to load sample-based instruments (e.g. `\piano`).

## Add a custom sample instrument

1) Drop your audio files in this folder.
2) Edit `threnoscope/samples/_samples.scd` and add a new instrument block:

```supercollider
(
    myinstr: (
        55: (
            midinote: 33,
            path: "my_A1.wav",
            startPos: 0,
            endPos: 12345
        ),
        110: (
            midinote: 45,
            path: "my_A2.wav",
            startPos: 0,
            endPos: 23456
        )
    )
)
```

Notes:
- Keys like `55`, `110`, `220`, etc. are reference frequencies (Hz).
- `midinote` is the MIDI note of the original sample pitch.
- `startPos`/`endPos` are frame indices for the loop region.
- `path` is relative to this folder.

## Audio format

The loader reads **channel 0** only, so stereo files are fine but will be
downmixed to mono. If you want true stereo support, the synths will need to
be adapted to 2 channels.

## Reload

After editing `_samples.scd`, recompile classes or run:

```supercollider
DroneSynths.new;
```
