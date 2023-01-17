# Clerk: Moldable Live Programming for Clojure

``` clojure
(ns nextjournal.clerk.px-23
  {:nextjournal.clerk/toc true
   :nextjournal.clerk/visibility {:code :hide}})
```

## Abstract

> In such a future working relationship between human problem-solver and computer ‘clerk’, the capability of the computer for executing mathematical processes would be used whenever it was needed.
>
> – Douglas Engelbart

This paper presents Clerk, a Clojure programmer’s assistant that builds upon the traditions of interactive and literate programming to provide a holistic moldable development environment. Clerk layers static analysis and browser-based rich graphical presentations on top of a Clojure programmer's familiar toolkit to enhance their workflow.

## Introduction: Literate Programming, Notebooks and REPL-Driven Development

With Literate Programming, Knuth highlighted the importance of focusing on human beings as the consumers of computer programs. He was generating two derived artifacts from a single file: source code for the computer and a typeset document in natural language to help fellow humans understand what the program should do.

Computational notebooks like Jupyter or Observable have gained popularity in recent years. These systems allow developers to mix code, text and visualizations in a document and improve upon Knuth's original idea by shortening the feedback loop from per-file processing to having a stateful process with which one can execute individual cells.

REPL-Driven Development in LISPs generally and Clojure specifically allow for code evaluation with even greater fidelity letting the programmer evaluate individual forms. Clojure's single pass compilation strategy together with its focus on functional semantics make it very well suited to interactive development. The REPL output is limited to textual output however which imposes a severe limitation on its information design. Problems typically arise when printing structurally large results that cause either the editor performance to degrade or truncate output with only limiting customization abilities and no way to request more data. Furthermore, the output is dead text without interactivity.

Smalltalk systems like Pharo, Glamorous Toolkit or Newspeak offer a completely open and customizable programming environment. Glamorous Toolkit wants to reduce the time developers spend reading code in order to figure the system out.

## Programming with Clerk

### Basic Interaction: Bring-Your-Own-Editor

The main idea behind Clerk is meeting Clojure programmers where they are, letting Clerk progressively enhance their existing workflows in their favorite editors. This is a hard-learnt lesson after years of unsuccessfully trying to get our Clojure dev team to use an [online browser-based notebook platform][nextjournal] that we've also developed part of our day-to-day work life.

When working with Clerk, a split-view is typically used with the code editor next to the browser showing Clerk’s representation of the same document, see Figure 1).

Clerk’s audience is experienced Clojure developers that are familiar with interactive development at the Clojure REPL. Clerk is meant to complement this workflow. Programmers continue to use the Clojure REPL to build up their programs incrementally, one form at a time and inspect intermediate results. Clerk’s evaluation model intentionally does not offer the same level of granularity: it only works on files or source code strings. To keep the feedback loops short, Clerk caches the results of computations and only recomputes what needs to be changed.

Clerk is a Clojure library that runs in-process, allowing it to access any library code. Clerk does not introduce a separate format, it works on top of regular Clojure namespaces in which line comments are interpreted as Markdown and are displayed as prose. As in many other programming languages, line comments have no effect on the program’s semantics. The same format was previously used by [maria.cloud][maria]. This allows Clerk to avoid a lot of the problems that alternative notebooks with bespoke formats face and makes putting the notebooks into version control or using them as library code trivial.

Clerk offers two main modes of interaction: 

* an optional file watcher that can show a notebook as a result of saving a Clojure namespace on the filesystem; or alternatively,
* an editor hotkey that can be bound to show the current document. As authors, we prefer the editor hotkey over the file watcher as it feels more direct and gives more control over when to show something in Clerk.

### Fast Feedback: Caching & Incremental Computation

Control of Clerk also happens through the Clojure REPL. Besides showing a specific namespace, there are functions to control Clerk’s caching behavior.

Clerk’s caching works at the granularity of top-level forms. 

Clerk will first perform an analysis of the forms to be evaluated. In this step, we will perform macro-expansion in order to collect all dependency vars. We then go on to recursively analyze all dependencies until the full graph is discovered. For each top-level form, a hash is computed as the hash of the form and the hash of all its dependencies.

Following the analysis, Clerk will proceed to evaluate the document. Here, it will traverse the doc and evaluate each form unless if finds a cached value for the hash of the form. Each result is stored in an in-memory cache and in an on-disk cache using the nippy serialization library. Clerk currently restricts caching to anonymous forms or forms that define a single var. For the on-disk cache, Clerk additionally checks if the result is cacheable and does not contain lazy sequences beyond a configurable size to avoid infinite loops. Every result cached on-disk is stored in a content-addressed store where the filename is derived from the SHA512 of the contents using a base58-encoded multihash to support changing the hash algorithm in the future. Additionally, a file contains a pointer from a SHA-1 hash of the form to the contents of the result. This setup allows to distribute the Clerk cache. 

Clerk is consumable as a library published on Clojars or as a git dependency using Clojure `tools.deps`. This allows to reproducibly compute a classpath from a deps.edn file. Because Clerk’s hashing is also deterministic (given unchanged dependencies) results can be shared by distributing the cache without needing to track them in version control.

Caching behavior can be disabled on a per-form or per-namespace basis using metadata annotations. There’s also an option to disable Clerk’s caching globally using a system property.

Clojure encourages programming with pure functions and using mutable containers called atoms to isolate mutable state. The value inside an atom can be accessed by dereferencing it for which Clojure includes `@` as a syntax affordance. When using atoms for mutable state, Clerk will attempt to compute a hash based on the value inside an atom for any expression that dereferences it, making Clerk’s caching play nice with how mutable state is most commonly modeled in Clojure.

### Built-in Viewers

Clerk comes with a number of built-in viewers. These include viewers for Clojure’s built-in data structures, HTML (including the hiccup variant that is often used for Clojure and SVG), Plotly, Vega, tables, math code, images, grids as well as a fallback viewer that builds on top of Clojure’s printer via `pr-str`. The [Book of Clerk][book-of-clerk] gives a good overview of the available built-ins. Clerk’s view is running in the browser. We made this choice in order to benefit from its rendering engine and leverage the vast number of libraries in the JS ecosystem (e.g. plotly, vega, codemirror` and KaTeX). Users have successfully experimented with in-process rendering without a browser. 

In order to not overload the browser, Clerk’s built-in collection views will only show the first 20 items, allowing to request more data on demand. Besides this simple limit, there’s a second global budget per result to limit the total number of items also for deeply nested data. We’ve found this simple system to work fairly well in practice.

### Moldable Viewer API

Viewer selection and elision of data happens on the JVM in Clojure. As a nod to a similar system in Common Lisp ?, we call this *presentation*. Clerk’s viewers are an ordered collection of plain Clojure hash maps. Clerk willl interpret the following optional keys:

* A `:pred` function to test if a given viewer should be selected;
* a function on the `:transform-fn` key that can perform a transformation of the value. This receives a map argument with the original value under a key. Additional keys carry the path, the viewer stack and the budget.
* A `:render-fn` key containing a quoted form that will be sent to the browser where it will be evaluated using `sci` (the Small Clojure Interpreter) and turned into a function;
* A `:page-size` key on collection viewers to control how many items to show.

Viewers can also be explicitly selected using functions like `clerk/with-viewer` which will wrap a given value in a map with the given viewer. Alternatively to the explicit functional API, viewers can be selected using metadata on the form. This has no meaning in Clojure and thus won’t in any way affect the value of the program when run without Clerk and is also useful for when downstream consumers rely on a value being used unmodified.

### Sync

Clerk also supports bidirectional sync of state between the SCI viewer environment and the JVM. If an atom is annotated (via metadata) to be synced, Clerk will create a corresponding var in the SCI environment and watch this atom for modifications on both the JVM Clojure and the SCI browser side and broadcast a diff to the other side. In addition, a JVM-side change will cause a recompilation of the currently active document, which means no re-parsing or analysis of the document will be performed but only a re-evaluation of cells dependent on the value inside this atom. This allows to use Clerk for small local-first apps.

### Static Publishing

Clerk also comes with a way to turn a collection of notebooks into static HTML pages for publishing to the web.

[book-of-clerk]:https://book.clerk.vision
[nextjournal]:https://nextjournal.com
[maria]:https://maria.cloud
