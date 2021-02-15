# ComposeModel

This is a proof-of-concept for a compiler plugin that generates models from interfaces describing
their public API, where the model business logic is implemented as a composable function.

## Usage

### Defining your model API

```kotlin
@ComposeModel
interface TodoListModel {
  // Data
  val todos: List<TodoModel> = emptyList()
  val completedTodos: List<TodoModel> = emptyList()

  // Event handlers
  fun onTodoAdded(todo: TodoModel)
  fun onTodoCompleted(todo: TodoModel)
}
```

This interface defines a model for a todo list. We can write a composable function to render the
list, but that's left as an exercise for the reader. What this library does is generate some code
to help you implement the business logic for this model using Compose.

In other words, the model interface defines both the model's public API, and a DSL for implementing
the model.

### Defining your model behavior

Here's an implementation:

```kotlin
@Composable fun TodoListModel(): TodoListModel = rememberTodoListModel {
  onTodoAdded { todo ->
    todos += todo
  }
  onTodoCompleted { todone ->
    require(todone in todos) { "Invalid todo: $todone" }
    todos -= todone
    completedTodos += todone
  }
}
```

`rememberTodoListModel` is generated for you. The code inside the lambda gets a mutable version of
`TodoListModel` – it can write to the properties, and when it calls the event handlers, it actually
passes lambdas that _handle_ the events. The lambda is a composable function, and you can do stuff
like create private state with `remember` and `rememberSaveable`, launch coroutines with
`LaunchedEffect`, etc. You can read `CompositionLocal`s, but all the usual warnings about that
apply.

Let's demonstrate private state by adding a timer:
```kotlin
@ComposeModel
interface TodoListModel {
  // Data
  // …

  val timer: String

  // …
}
```
Note that the `timer` property doesn't have a default value, so we'll have to specify it explicitly.
```kotlin
@Composable fun TodoListModel(): TodoListModel = rememberTodoListModel(
  // When the function is re-generated after the above change, this parameter will be required,
  // and the code won't compile until we specify it.
  timer = Duration.ZERO.toString()
) {
  // Run the timer loop in a coroutine for as long as the model is composed.
  LaunchedEffect(Unit) {
    val startTime = System.currentTimeNanos()
    while(true) {
      delay(1000)
      timer = (System.currentTimeNanos() - startTime).nanoseconds.toString()
    }
  }

  // …
}
```

You could even emit things (e.g. UI composables), because Compose doesn't provide any APIs
for stopping you, but that's strongly discouraged. The model composable should only be responsible
for the _model_'s business logic – UI should be defined separately. The behavior is also undefined –
models don't expect their children to emit UI, so there's no meaningful layout context.

The generated `rememberTodoListModel` function and the builder interface are both `internal`, so
they don't pollute your module's public API.

### Persisting your model

If you were to run this app, you'd find it automatically saves and restores the models on config
change. By default, `rememberTodoListModel` will store your model in the `UiSavedStateRegistry`.
Only the properties are stored, and they must all be auto-saveable (in the same sense as the default
`autoSaver()` value used by `rememberSaveable`). You can turn off this behavior by passing
`saveable = false` to the `@ComposeModel` annotation.

## How it works

The plugin is implemented as a [KSP](https://github.com/google/ksp) processor.

- The builder interface is simply a copy of the model interface with vals changed to vars, default
  getters erased, and the event handler function signatures changed.
- A private implementation class is generated. This class implements both the model interface and
  the builder interface. Since properties have the same names in both interfaces, they don't clash.
  Each property is backed by a `MutableState`. Two overloads of each event handler function are
  generated – one for each interface. Each pair of functions has a backing property that is simply
  a mutable lambda holder. When a builder event handler is called the backing property is set, and
  when the model function is called it is invoked. This is _not_ a `MutableState` since nothing
  needs to be notified when the event handler changes. If the model is to be saveable, a `Saver`
  implementation is also generated for this class that stores each property in a map.
- The remember function simply calls `remember { Impl() }` or `rememberSaveable { Impl() }` and
  then passes it to the lambda argument on every composition before returning the remembered object.

## Is this a terrible idea?

Probably. There are quite a few potential issues:

- If a parent model implementation reads its child's properties, I believe it won't see changes to
  them until the next composition pass – and this is usually strongly advised against by the Compose
  team when it comes up in the Kotlin Slack.
- It's easy to forget to set all the event handlers in the `remember*` function. This could maybe
  be enforced better with a real compiler plugin that could warn if there was a missing call.
- Ideally model composables would not be allowed to emit any UI. There's no way to enforce this at
  compile time, nor at runtime.
    - We could run the model composition separately from the UI composition, with an `Applier` type
      that doesn't allow emitting anything (`Nothing` nodes), but that's problematic because:
        - It still doesn't provide safety at compile time, only runtime.
        - Some things, like text editing, don't work when changes and updates are shuffled between
          different compositions.
    - We could wrap the root model composable in a special layout that throws if any children are
      emitted, but that would require remembering to wrap the root, and obviously would only provide
      runtime safety.

## Status

This project is _very_ rough. The code is super gross and undocumented, there's no real tests, and
it's not published. There is a demo module that should build and run however, and you can checkout
the repo and mess around if you like. There's some validation with vaguely useful error messages,
but there's probably a lot of ways to get the plugin to just puke.

I don't expect I'll spend much more time on this, but if I wanted to make it a real thing, some
features I'd like to add are:

- Annotation for leaving certain properties out of persistence (probably just use `@Transient`).
- Annotation for specifying custom `Saver`s for individual properties.
- Helpers for writing unit tests – create a special composition that forbids emissions.
- Support model properties with `StateFlow` types. The builder interface would still just get a
  mutable property, but instead of being backed by a `MutableState` it would be backed by a
  `MutableStateFlow`.
- Multiplatform support.
- The `@ComposeModel` annotation should be a `@StableMarker` to opt-in to compiler optimizations.
- Implement as a full-fledged compiler plugin instead of a KSP processor to integrate more tightly
  with the IDE (real-time redlines, not require a manual build to show changes to generated code),
  and maybe make the generated APIs cleaner.
