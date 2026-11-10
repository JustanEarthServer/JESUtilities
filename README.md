# JESUtilities

Small quality-of-life features for jes.earth.

## Autoboat

Hold the forward key while steering a boat for 5 seconds and autoboat turns on: the boat keeps rowing forward at normal top speed by itself, so you can travel long distances without holding the key. You can still steer left and right.

Autoboat only turns on if the boat actually moved during those 5 seconds, so rowing against a wall or on land won't trigger it.

Autoboat turns off when:

- you press the back key (S)
- you type `/autoboat off`
- the boat stops making progress (hit the shore, stuck on something)
- you leave the boat

### Commands

| Command | Description |
| --- | --- |
| `/autoboat` | Toggle autoboat for the boat you are steering |
| `/autoboat on` | Turn autoboat on |
| `/autoboat off` | Turn autoboat off |

Permission: `jesutilities.autoboat` (default: everyone)

## Building

```
./gradlew build
```

The jar is written to `build/libs/`.

Requires Paper 26.2 and Java 25.
