# Ice Hockey Game

A simple two-dimensional JavaFX air-hockey game created for the Java 2 project assignment.

The game supports single-player gameplay, turn-based socket multiplayer, RMI chat, save/load, XML move history and replay, Reflection documentation, and external configuration.

## Requirements

- JDK 21
- IntelliJ IDEA
- Maven wrapper included in the project
- JavaFX Scene Builder for editing `hello-view.fxml`
- Bundled JNDI file-context provider in `lib/fscontext-4.5-b13.jar`

## Gameplay

- The aiming arrow rotates around the active player.
- Press `P` to launch the active player in the arrow direction.
- Hitting the puck transfers force to it.
- The puck bounces from rink walls and can enter either goal.
- The first player to reach the winning score wins, or the score leader wins when time expires.

In single-player mode, only the left player launches. The opponent is a deterministic vertical patrol obstacle and does not use AI.

## IntelliJ Run Configurations

### Single Player

Run:

```text
SINGLE_PLAYER
```

Click `Start`, then use `P` to launch.

### Multiplayer

Run these configurations in order and leave each process running:

```text
1. RMI_SERVER
2. PLAYER_1
3. PLAYER_2
```

`PLAYER_1` is the authoritative socket host. It advances physics, timer, scoring, and turn switching. `PLAYER_2` renders synchronized game-state snapshots and sends launch requests when it is Player 2's turn.

The RMI server provides shared chat history to both multiplayer windows.

## Menu Features

- `Game > New Game` resets the match and XML move history.
- `Game > Save Game` serializes the game to `game/save.dat`.
- `Game > Load Game` restores the serialized game.
- `Game > Replay` replays snapshots from `xml/gameMoves.xml` in single-player mode.
- `Documentation > Generate Documentation` writes Reflection documentation to `doc/documentation.html`.

## Configuration

Network settings are stored in `conf/app.conf`:

```properties
player.one.server.port=8001
player.two.server.port=8002
host.name=localhost
rmi.server.port=1099
```

## Project Structure

```text
hr.algebra.hockey
  controller/   JavaFX controller
  engine/       game loop, physics, collisions, scoring
  exception/    application exceptions
  jndi/         external configuration reader
  model/        serializable game models and XML move models
  network/      socket multiplayer messages and service
  rmi/          remote chat interface, implementation, and server
  utils/        serialization, XML, chat, dialogs, documentation
```

The JavaFX layout is located at:

```text
src/main/resources/hr/algebra/hockey/hello-view.fxml
```

Its reusable JavaFX styles are in `style.css` in the same directory.

## Assignment Feature Mapping

| Requirement | Implementation |
| --- | --- |
| JavaFX MVC and FXML | `HockeyGameApplication`, `HockeyGameController`, `hello-view.fxml` |
| Scene Builder layout | `hello-view.fxml` |
| Game model hierarchy | `GameState`, `Player`, `Puck`, enums |
| Threads and asynchronous work | socket service threads, asynchronous RMI chat calls |
| Serialization | `GameSaveUtils` and serializable `GameState` |
| Reflection API | `DocumentationUtils` |
| Socket networking | `SocketMultiplayerService`, `MultiplayerMessage` |
| JNDI/configuration | `ConfigurationReader`, `ConfigurationKey`, `conf/app.conf` |
| RMI | `ChatRemoteService`, `ChatRemoteServiceImpl`, `RmiServer` |
| XML DOM and DTD | `XmlUtils`, `HockeyMove`, `dtd/gameMoves.dtd` |
| Replay | JavaFX `Timeline` replay in `HockeyGameController` |

## Build and Tests

Run the complete test suite:

```powershell
.\mvnw.cmd test
```

Run a clean verification build:

```powershell
.\mvnw.cmd clean test
```

The regression suite covers single-player turn ownership, multiplayer turn switching, timer completion, deep network snapshot isolation, and a real JNDI file-context configuration lookup.

## Current Limitations

- Multiplayer currently supports one Player 1 and one Player 2 session.
- The default configuration targets `localhost`.
- Player 1 controls New Game, Load, Start, and Pause in multiplayer.
- Replay is intentionally limited to single-player mode.
