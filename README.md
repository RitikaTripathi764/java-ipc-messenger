# Player assessment

Requires JDK 17+, Maven 3.6.3+, and Bash on Linux/macOS (or a configured WSL environment).
No application or test dependencies. Maven downloads its build plugins on the first build.

Run from this project directory:

```sh
bash start.sh same
bash start.sh separate 5000
```

Alternatively, in two terminals, start `bash start.sh server 5000` and
`bash start.sh client 5000`. Connect within ten seconds after the server starts listening.
The server is the responder; the client is the initiator. Both outputs must report
`sent=10 received=10`. PIDs match in same mode and differ in separate mode.

The initial message is `hello`, counted as the initiator's first send. A reply appends
the outgoing message's one-based ordinal with no separator. Thus the first messages
are `hello`, `hello1`, `hello12`, `hello122`. Each player has its own counter.
The initiator consumes its tenth reply without replying. The responder finishes
after its tenth send; no control message or EOF is counted as a message.

Player is thread confined and owns only protocol state. MessageChannel is its
transport strategy. LocalMessageChannel uses two capacity-one ArrayBlockingQueues;
Main owns and joins its two workers, interrupting them on failure. SocketMessageChannel
owns framed socket sessions and resource scopes. Separate mode starts exactly two
Java processes; Bash retains both PIDs and waits for completion.

Normal termination is driven by message counts, not timeouts. Socket accept/read
inactivity is limited to ten seconds. Connection refusals are retried briefly during
startup. Local completion has a fifteen-second watchdog. These limits detect a
failed/missing peer and are not scheduling targets. The socket framing is Java's
length-prefixed modified UTF-8 (`writeUTF`/`readUTF`), bounded to 65,535 encoded bytes.

Dependency-free protocol checks (run explicitly; Maven's test phase does not run this main):

```sh
mvn -q test-compile
java -cp target/classes:target/test-classes com.example.players.PlayerChecks
```

On native Windows, replace the classpath colon with a semicolon.
Checks cover both players' counters and reply suffixes, the exact terminal receive,
delivery failure accounting, and one-shot player use. Run both start modes as
integration smoke checks, inspecting the counts and PIDs described above.
