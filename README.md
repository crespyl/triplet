# Triplet

A small project integrating Electron, ClojureScript, Re-Frame/Reagent/React,
and Quil/Processing.

The intent is to allow simple entry of triples, and to render/explore the
triple set as a graph, visualized with Quil/Processing.

## Development
This project uses shadow-cljs with electron, which means if we want full
editor/debug support for nREPL and friends, we must have a way to connect to
both the node/main and chrome/renderer processes.

For now, the most reliable way to do this is with three separate commands:

1. `yarn run server` - start the shadow-cljs server and nREPL
2. `yarn run watch` - start the shadow-cljs file-watcher and compiler for both the main and renderer builds
3. `yarn run electron` - start the actual electron app in dev mode

If all these start up correctly, you should have the electron window up and
running in dev mode.

Additionally, shadow-cljs should have a runtime inspector available at
`http://localhost:9630`, and the nREPL server at `localhost:9000`.

### Emacs notes
You should be able to use `cider-connect-cljs` and `cider-connect-sibling-cljs`
to connect to both the main and renderer cljs runtimes simultaneously.  If
`clojurescript-mode` complains about not finding the right kind of repl from a
cljs buffer even after you've connected, check that `cider-repl-type` is set to
`'cljs`, and that the cider configuration is set to use `shadow-cljs`.  The
project `.dir-locals.el` should set that up, but if you don't have dir-locals
enabled you can look in that file to see what needs to happen.

For some reason, at least in my set up, emacs only prompts to select the runtime
on the first connection, and any calls to `cider-connect-sibling-cljs` after
that will jump straight to the same runtime as the first.  I think this should
be fixable with the right configuration changes, but a work-around to let you
connect to the main process after the renderer is to make sure that you are in
the buffer for `shadow-cljs.edn` when you try to connect.


## Build Electron

To package the electron app for release
```
./build.sh [dev|release]
```
Build artifacts go in the `/target` directory
