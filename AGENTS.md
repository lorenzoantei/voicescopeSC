# Repository Guidelines

## Project Structure & Module Organization
- Root `.sc` files define the core Threnoscope/Voicescope classes (e.g., `Drone.sc`, `DroneGUI.sc`, `ThrenoScope.sc`).
- `HelpSource/` contains SuperCollider help files (`*.schelp`).
- `threnoscope/` holds runtime assets and user data:
  - `samples/` for audio WAVs and `_samples.scd` metadata.
  - `scores/`, `groups/`, `states/`, `scl/`, `scl_user/` for saved content and tuning data.
- RTF files (`*.rtf`) are end‑user docs and examples.

## Build, Test, and Development Commands
This is a SuperCollider Quark; there is no separate build step.
- Install/update locally in SuperCollider:
  - `Quarks.install("https://github.com/thormagnusson/threnoscopeSC.git");`
- Recompile the class library after changes.
- Run the app:
  - `s.boot;`
  - `ThrenoScope.new(2);` (use 2 for stereo; adjust for your speaker setup)

## Coding Style & Naming Conventions
- Follow existing SuperCollider style: tabs for indentation, braces on the same line, and concise method names.
- Class names are `UpperCamelCase` (e.g., `DroneGUI`); instance variables and methods use `lowerCamelCase`.
- Keep audio/control parameter keys as symbols (e.g., `\env`, `\gate`, `\piano`).
- No automated formatter or linter is configured; keep diffs small and readable.

## Testing Guidelines
- There is no automated test suite in this repository.
- Validate changes by compiling the class library and running `ThrenoScope.new(...)`.
- If you touch samples, verify `_samples.scd` loads and audio plays without buffer/channel errors.

## Commit & Pull Request Guidelines
- Recent commits use short, imperative, lowercase summaries (e.g., “fix drone kill system”).
- Keep commits focused (one topic per commit) and include context in the PR description.
- If UI behavior or documentation changes, update the relevant `.rtf` or `.schelp` files and mention it in the PR.

## Configuration Notes
- `sc3-plugins` are required for full functionality; mention this in any setup instructions.
- Asset paths should be built from the app/quark path (not hard-coded user paths) so installs remain portable.
