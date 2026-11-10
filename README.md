# JESUtilities

Utility plugin for jes.earth.

## Autoboat

If you hold W in a boat for 5 seconds, autoboat turns on and the boat keeps going forward by itself. You can still steer with A/D.

It turns off if you press S, type `/autoboat off`, get out of the boat, or the boat stops moving (hits land etc). It also won't turn on if you're rowing into a wall.

Commands:
- `/autoboat [on|off]`
- `/autoboat reload` - reloads the config

Permissions:
- `jesutilities.autoboat` - everyone by default
- `jesutilities.reload` - op by default

Messages and timings can be changed in `config.yml`.

## Building

`./gradlew build`, jar ends up in `build/libs/`. Needs Paper 26.2 / Java 25.
