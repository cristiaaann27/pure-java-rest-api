# REST API in pure Java 25

A working REST API with **zero dependencies and no build tool**. No Spring, no Jackson, no Lombok,
no Vavr, no JUnit, no Maven, no Gradle. Everything the application needs is either in the JDK or
written here by hand.

The point is not that you should build APIs this way. The point is that after writing the JSON
parser, the router and the error mapping yourself, you know exactly what a framework is doing for
you, and why.

```
requires jdk.httpserver;   // the entire dependency list, in src/main/java/module-info.java
```

## Requirements

JDK 25 or newer. Nothing else. Check with `javac -version`.

## Quick start

```bash
./api build     # compile with javac into out/classes
./api test      # compile and run the test suite
./api run       # start the server on http://localhost:8000
./api jar       # package a modular, executable jar
./api hash      # print a password hash for the admin credentials
./api clean     # delete out/
```

`api` is a shell script that calls `javac`, `jar` and `java`. That is the whole build system.

There is not even a compilation step if you do not want one. Since Java 22 the launcher runs
multi-file source programs directly:

```bash
java --source-path src/main/java src/main/java/com/cristiannustes/app/Application.java
```

### IDE

`.project`, `.classpath` and `.settings/` are committed. They are plain XML read by the Java
language server that Eclipse, VS Code and Cursor share, and they describe exactly what `./api`
does: two source folders, Java 25, no libraries. Without them the IDE has to guess the layout of a
project that has no build file, and a wrong guess shows phantom errors on code that compiles.

## Trying it out

```bash
# Health
curl localhost:8000/api/health

# Create a user
curl -X POST localhost:8000/api/users \
  -H 'Content-Type: application/json' \
  -d '{"login":"marcin","password":"a-good-password"}'

# List, with pagination
curl 'localhost:8000/api/users?page=0&size=10'

# Read, replace, delete
curl localhost:8000/api/users/{id}
curl -X PUT localhost:8000/api/users/{id} \
  -H 'Content-Type: application/json' \
  -d '{"login":"marcin.nowak","password":"another-good-one"}'
curl -X DELETE localhost:8000/api/users/{id}

# The greeting endpoint, behind basic authentication
curl -u admin:<password> 'localhost:8000/api/hello?name=Cristian'
```

On start up, if no admin password is configured, the server generates one and prints it to the log.
To keep it stable, run `./api hash` and export the result as `API_ADMIN_PASSWORD_HASH`.

### Endpoints

| Method | Path                   | Description                        | Auth |
|--------|------------------------|------------------------------------|------|
| GET    | `/api/health`          | Liveness, uptime and user count    | no   |
| POST   | `/api/users`           | Register a user, returns 201       | no   |
| POST   | `/api/users/register`  | Alias kept from the original guide | no   |
| GET    | `/api/users`           | Paginated list                     | no   |
| GET    | `/api/users/{id}`      | Read one user                      | no   |
| PUT    | `/api/users/{id}`      | Replace credentials                | no   |
| DELETE | `/api/users/{id}`      | Delete, returns 204                | no   |
| GET    | `/api/hello`           | Greeting                           | yes  |

### Configuration

Everything comes from the environment; nothing is hardcoded.

| Variable                     | Default  | Meaning                              |
|------------------------------|----------|--------------------------------------|
| `API_PORT`                   | `8000`   | Listening port, `0` picks a free one |
| `API_BACKLOG`                | `0`      | Pending connection queue             |
| `API_ADMIN_USER`             | `admin`  | Basic auth user                      |
| `API_ADMIN_PASSWORD_HASH`    | generated| Output of `./api hash`               |
| `API_MAX_BODY_BYTES`         | `65536`  | Largest accepted request body        |
| `API_SHUTDOWN_GRACE_SECONDS` | `5`      | Grace period on shutdown             |
| `API_PASSWORD_ITERATIONS`    | `210000` | PBKDF2 cost factor                   |

## Layout

```
src/main/java/
  module-info.java            requires jdk.httpserver, and nothing else
  com/cristiannustes/
    json/                     JSON parser and writer          (replaces Jackson)
    http/                     router, request, response       (replaces the web framework)
      error/                  sealed error hierarchy, mapper
      filter/                 request id, logging, body limit
    domain/                   users, validation, business rules
    data/                     in memory storage
    api/                      HTTP handlers and JSON codecs
    security/                 PBKDF2 hashing, basic auth
    app/                      configuration, wiring, bootstrap
src/test/java/
  com/cristiannustes/
    testing/                  the test framework              (replaces JUnit)
```

Request flow:

```
client -> HttpServer -> RequestLogFilter -> BodyLimitFilter -> Router -> handler -> service -> repository
                              |                                  |
                        binds RequestContext              ErrorMapper on failure
```

## What is written by hand, and what it taught

### JSON, instead of Jackson

`com.cristiannustes.json` is a sealed interface with six record implementations, a recursive descent
parser and a writer. The sealed hierarchy is the interesting part:

```java
switch (value) {
    case JsonNull ignored            -> out.append("null");
    case JsonBoolean(boolean flag)   -> out.append(flag);
    case JsonNumber(double number)   -> writeNumber(number, out);
    case JsonString(String text)     -> writeString(text, out);
    case JsonArray(List<JsonValue> items)          -> writeArray(items, out);
    case JsonObject(Map<String, JsonValue> fields) -> writeObject(fields, out);
}
```

No `default` branch. The compiler knows every possible shape of a `JsonValue`, so if a seventh one
is ever added, every switch that forgot about it stops compiling. That is a class of bug removed at
the language level rather than caught by a test.

The parser is deliberately strict: no comments, no trailing commas, no duplicate keys, no trailing
content, and a nesting limit of 64. That last one is not pedantry: `[[[[[...]]]]]` with a few
thousand brackets is a StackOverflowError, which is a denial of service wearing a bug costume.

Mapping between records and JSON is explicit, in `UserJson`, rather than reflective. It is more
code, but renaming a record component can no longer silently rename a field of the public API.

### A router, instead of `@GetMapping`

The original tutorial called path parameters "cumbersome" and left them out. `PathPattern` compiles
`/api/users/{id}` once, at start up, into a regular expression with one capturing group per
variable. `Router` then does what a framework does:

- picks the handler by verb and path;
- tells a missing resource (404) apart from a wrong verb on an existing one (405), and sends the
  `Allow` header that RFC 9110 requires with a 405;
- makes `HEAD` behave like the `GET` it shadows, minus the body;
- writes the response exactly once, with the right `Content-Length`, inside a `try (exchange)`.

### Errors, with an exhaustive switch

`HttpException` is a sealed hierarchy and `ErrorMapper` translates it in a single switch. The
previous version used a chain of `instanceof` plus manual casts, and a new error type meant
remembering to add another branch. Now forgetting is a compile error.

Every error response is the same shape, and carries the request id so a user can quote it:

```json
{"code":400,"status":"Bad Request","message":"the request is not valid","requestId":"7d349e78edfa2909",
 "errors":[{"field":"login","message":"must be between 3 and 64 characters"}]}
```

A 500 never echoes the internal exception message. Exception text leaks class names, file paths and
sometimes queries; it belongs in the log, not in the response.

### A test framework, instead of JUnit

`com.cristiannustes.testing` is three files: a `@Test` annotation, an `Assertions` class and a
`TestRunner` that scans `out/test-classes`, loads the classes and invokes the annotated methods. It
exits non zero when something fails, which is all a CI job needs.

The integration tests start a real server on an ephemeral port and drive it with
`java.net.http.HttpClient`, also from the JDK. Nothing is mocked: sockets, router, filters and JSON
writer all run exactly as in production.

```
60 tests, 60 passed, 0 failed in 341 ms
```

## Java 25 features, and why each one is here

**Records** replace Lombok. `@Value`, `@Builder`, `@Getter` and `@AllArgsConstructor` all disappear,
along with the annotation processor that used to be part of the build.

**Sealed interfaces and pattern matching** give the JSON model and the error hierarchy exhaustive
switches that the compiler verifies.

**Virtual threads** replace `server.setExecutor(null)`, which served one request at a time. Now
`Executors.newVirtualThreadPerTaskExecutor()` gives each request its own thread, and blocking I/O no
longer holds a platform thread hostage. The concurrency test issues sixty simultaneous
registrations, which the original code would have serialised.

**Scoped values** (JEP 506, final in 25) carry the request id and the authenticated user through the
call without adding a parameter to every method:

```java
ScopedValue.where(CURRENT, context).call(() -> { body.run(); return null; });
```

This is where the virtual thread part matters. A `ThreadLocal` on a thread created and destroyed per
request is at best wasteful and at worst a leak. A scoped value is immutable, visible only inside
the block that binds it, and needs no cleanup.

**Flexible constructor bodies** (JEP 513) let `HttpException` validate its arguments *before* calling
`super(...)`, instead of constructing a half broken object and checking afterwards.

**Stream gatherers** slice the user list into pages with `Gatherers.windowFixed(size)`.

**Compact source files** (JEP 512) are used in [examples/hello.java](examples/hello.java): the whole
first chapter of the original tutorial, with no class declaration and no static `main`, runnable with
`java examples/hello.java`.

Deliberately left out: *structured concurrency*, still a preview feature in Java 25, which would
force `--enable-preview` on both compilation and execution.

## What was wrong with the previous version

The rewrite started from a review of the original code. These were real defects, not style
preferences:

- **One request at a time.** `server.setExecutor(null)` uses the default executor, which is a single
  thread.
- **No graceful shutdown.** Killing the process dropped requests in flight. There is now a shutdown
  hook that calls `server.stop(grace)`.
- **Passwords stored in clear text.** `PasswordEncoder.encode` returned its argument, with a
  `//TODO: implement password encoding` next to it.
- **Credentials in the source.** `user.equals("admin") && pwd.equals("admin")`, which also threw a
  `NullPointerException` on a null user and leaked the secret through comparison timing.
- **Wrong status code.** Registration answered `200 OK`; creating a resource is `201 Created`, with a
  `Location` header.
- **A NullPointerException in query parsing.** `splitQuery` split on `=` and took two parts blindly,
  so `?verbose` produced a null key and blew up inside the grouping collector.
- **Chunked responses for no reason.** `sendResponseHeaders(status, 0)` starts a chunked response;
  `-1` means "no body" and a positive number sets `Content-Length`.
- **Leaked exchanges.** The error path never closed the `HttpExchange`.
- **`printStackTrace()` as logging**, with no request correlation.
- **A static, raw `Map` as the datastore**, shared by every instance, which makes isolated tests
  impossible and needs casts on every read.
- **No duplicate login check, no validation, no body size limit, no tests.**

## Trade-offs worth naming

**The domain throws HTTP exceptions.** `UserService` raises `ConflictException` and
`NotFoundException` directly. In a larger system the domain would define its own errors and a
translation layer would map them to statuses. For an API this size that indirection would cost more
than it buys, but it is a real coupling and not an accident.

**Storage is a `LinkedHashMap` behind one lock.** Insertion order gives pagination a stable total
order for free, and the lock keeps the id map and the login index consistent. A database would do
both better.

**Basic auth caches verified credentials.** Deriving a PBKDF2 hash with 210 000 iterations on every
request would add roughly 200 ms to each call, so successful verifications are remembered by digest.
Real systems use a token after the first authentication.

**Passwords travel as `String`.** A `char[]` that can be wiped would be better, but records and
`String` interoperate badly here. `toString` is overridden on `NewUser`, `UserUpdate` and `User` so
an accidental log statement cannot print the secret.

## What you should not hand-roll in production

Writing this was worth it. Shipping it would not be. In a real system, use a JSON library, because
yours will not handle streaming, big decimals or the fifteen encoding edge cases someone will find.
Use a real framework's router, because content negotiation, CORS, compression and HTTP/2 are more
than an afternoon of work. Use a real test framework, for parallel execution, parameterised tests
and IDE integration. Use a build tool, for reproducible dependency resolution.

What does survive contact with production is everything the JDK gave us for free: `jdk.httpserver`,
virtual threads, scoped values, PBKDF2, and a language whose sealed types and records make whole
categories of bug impossible to write.

## Credits

Based on the original [pure-java-rest-api](https://github.com/piczmar/pure-java-rest-api) by Marcin
Piczkowski: a Spring developer wondering what it feels like to build an API without a framework.
That version still leaned on Jackson, Vavr, Lombok and Maven, and targeted Java 11. This one answers
the same question with a Java 25 vocabulary and removes the remaining crutches.
