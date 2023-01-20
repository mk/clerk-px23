# Clerk: Moldable Live Programming for Clojure

```clojure
(ns nextjournal.clerk.px23
  {:nextjournal.clerk/toc true
   :nextjournal.clerk/visibility {:code :hide}}
  (:require [nextjournal.clerk :as clerk]))
```

```clojure
(nextjournal.clerk/html [:div.rounded-lg.p-4.text-xs.font-sans.bg-yellow-100.border-2.border-yellow-200.dark:bg-slate-800.dark:border-slate-700
"⚠️ This is an early draft to be submitted to the " [:a {:href "https://2023.programming-conference.org/home/px-2023"} "Programming Experience 23 Workshop"] "."])
```

## Abstract

```clojure
(clerk/html
 [:div.flex.flex-col.not-prose
  {:class "min-[860px]:flex-row"}
  [:div
   [:p.italic.leading.leading-snug
    "Clerk is a Clojure programmer’s assistant that builds upon the traditions of interactive and literate programming to provide a holistic moldable development environment. Clerk layers static analysis, incremental computation, and rich browser-based graphical presentations on top of a Clojure programmer's familiar toolkit to enhance their workflow."]]
  [:div.font-sans.flex-shrink-0.mt-6.text-sm
   {:class "min-[860px]:w-[165px] min-[860px]:ml-[40px] min-[860px]:text-xs min-[860px]:mt-1"}
   [:a.hover:opacity-70 {:href "https://nextjournal.com"}
    [:img.block.dark:hidden {:src "https://nextjournal.com/images/nextjournal-logo.svg" :width 100 :class "min-[860px]:-ml-[8px]"}]
    [:img.hidden.dark:block {:src "https://nextjournal.com/images/nextjournal-logo-white.svg" :width 100 :class "min-[860px]:-ml-[8px]"}]]
   [:a.block.mt-2.hover:opacity-70 {:href "https://twitter.com/mkvlr"} "Martin Kavalar"]
   [:a.block.mt-1.hover:opacity-70 {:href "https://twitter.com/unkai"} "Philippa Markovics"]
   [:a.block.mt-1.hover:opacity-70 {:href "https://twitter.com/jackrusher"} "Jack Rusher"]]])
```

## Introduction: Literate Programming, Notebooks and Interactive Development

Knuth's _Literate Programming_ emphasized the importance of focusing on human beings as consumers of computer programs. His original implementation involved authoring files that combined source code and documentations, which were then divided into two derived artifacts: source code for the computer and a typeset document in natural language to explain the program.

At the same time, other software was developed to target scientific use cases rather than program documentation. These systems, which prefigured modern computational notebooks, ranged from REPL-driven approaches like Macsyma and Mathematica to integrated WYSIWYG editors like Ron Avitzur's _Milo_ and _MathCAD_.

In contemporary data science and software engineering practice, we often see interfaces that combine these two approaches, like Jupyter and Observable. In these notebooks, a user can mix prose, code, and visualizations in a single document that provides the advantages of Knuth's _Literate Programming_ with those of a scientific computing environment. Unfortunately, most such systems require the programmer to use a browser-based editing environment (which alienates programmers with a strong investment in their own tooling) and custom file formats (which cause problems for integration with broader software engineering practices)[^notebook-pain-points].

[^notebook-pain-points]: See [What’s Wrong with Computational Notebooks? Pain Points, Needs, and Design Opportunities](https://doi.org/10.1145/3313831.3376729) by Souti Chattopadhyay, Ishita Prasad, Austin Z. Henley, Anita Sarma and Titus Barik. TODO why is this broken?

Although notebooks of this kind present an improvement on the programming experience of many languages, they often feel like a step backward to experienced Lisp programmers. In Lisp environments, it is common to be able to place the cursor after a single Lisp form and evaluate it in the context of a running program, providing finer granularity of control compared to the per-cell model of most notebooks. This workflow leads to a development style that these programmers are loath to lose.

> That LISP users tend to prefer structured growth rather than stepwise refinement is not an effect of the programming system, since both methods are supported. I believe, however, that it is a natural consequence of the interactive development method, since programs in early stages of growth can be executed and programs in early stages of refinement cannot.[^sandewall]
>
> – Erik Sandewall

[^sandewall]: See [Programming in an Interactive Environment: the "Lisp" Experience](https://doi.org/10.1145/356715.356719) by Erik Sandewall

At the same time, though a number of Lisp environments have included graphical presentations of program objects[^mcclim], the default Clojure development experience relies on text-based representations of evaluation output and doesn't include the ability to embed widgets for direct manipulation of program state.

[^mcclim]: TODO https://en.wikipedia.org/wiki/Common_Lisp_Interface_Manager

Additional problems often arise when printing structurally large results, which can cause editor performance to degrade or lead to the truncation of output, and there's limited room for customization or support for requesting more data.

In comparison, interactive programming in Smalltalk-based systems has included GUI elements since the beginning, and work to further improve programmer experience along these lines has continued in Smalltalk-based systems like Pharo, Glamorous Toolkit[^moldable-tools] and Newspeak, which offer completely open and customizable integrated programming environments. Glamorous Toolkit, in particular, champions the idea of using easily constructed custom tools to improve productivity and reduce time spent on code archeology, which is also a big inspiration for what we'll present here.

[^moldable-tools]: See [Towards Moldable Development Tools](https://doi.org/10.1145/2846680.2846684) by Andrei Chiş, Oscar Nierstrasz and Tudor Gîrba

## Programming with Clerk

> In such a future working relationship between human problem-solver and computer ‘clerk’, the capability of the computer for executing mathematical processes would be used whenever it was needed.[^engelbart]
>
> – Douglas Engelbart

[^engelbart]: See [Augmenting Human Intellect: A Conceptual Framework](https://www.dougengelbart.org/pubs/augment-3906.html) by Douglas Engelbart.

### Basic Interaction: Bring-Your-Own-Editor

The main idea behind Clerk is meeting Clojure programmers where they are, letting Clerk progressively enhance their existing workflows in their favorite editors. This is a hard-learnt lesson after years of unsuccessfully trying to get our Clojure dev team to use an [online browser-based notebook platform][nextjournal] that we developed as part of our day-to-day work life.

``` clojure
^{::clerk/width :full}
(clerk/html
 [:div#figure-1.not-prose
  [:video {:loop true :controls true}
   [:source {:src "https://cdn.nextjournal.com/data/QmVYLx5SByNZi9hFnK2zx1K6Bz8FZqQ7wYtAwzYCxEhvfh?content-type=video/mp4"}]]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong "Figure 1: "] "Clerk side-by-side with Emacs"]]])
```

When working with Clerk, a split-view is typically used with the code editor next to the browser showing Clerk’s representation of the same document, [see Figure 1](#figure-1).

Clerk’s audience is experienced Clojure developers that are familiar with interactive development at the Clojure REPL. Clerk is meant to complement this workflow. Programmers continue to use the Clojure REPL to build up their programs incrementally, one form at a time and inspect intermediate results. In contrast, Clerk’s evaluation model is intentionally limited to the file, allowing for a simple editor integration.

To keep the feedback loops short in the presence of whole-file evaluation, Clerk caches the results of computations and only recomputes changed expressions.

As a Clojure library that runs in-process, Clerk has access any library code on the classpath. 

Clerk does not introduce a separate format, it works on top of regular Clojure namespaces in which line comments are interpreted as Markdown and are displayed as prose. As in many other programming languages, line comments have no effect on the program’s semantics. The same format was previously used by [maria.cloud][maria]. This allows Clerk to avoid a lot of the problems that alternative notebooks with bespoke formats face and makes putting the notebooks into version control or using them as library code trivial.

Clerk offers two main modes of interaction:

* an optional file watcher that can show a notebook xas a result of saving a Clojure namespace on the filesystem; or alternatively,
* an editor hotkey that can be bound to show the current document. As authors, we prefer the editor hotkey over the file watcher as it feels more direct and gives more control over when to show something in Clerk.

### Fast Feedback: Caching & Incremental Computation

Control of Clerk also happens through the Clojure REPL. Besides showing a specific namespace, there are functions to control Clerk’s caching behavior. Clerk’s caching works at the granularity of top-level forms. 

Clerk will first perform an analysis of the forms to be evaluated. In this step, we will perform macro-expansion in order to collect all dependency vars. We then go on to recursively analyze all dependencies until the full graph is discovered. For each top-level form, a hash is computed from the form and its dependencies.

Following the analysis, Clerk will proceed to evaluate the document. Here, it will traverse the doc and evaluate each form unless if finds a cached value for the hash of the form. Each result is stored in an in-memory cache and in an on-disk cache using the nippy serialization library. Clerk currently restricts caching to anonymous forms or forms that define a single var. For the on-disk cache, Clerk additionally checks if the result is cacheable and does not contain lazy sequences beyond a configurable size to avoid infinite loops. Every result cached on-disk is stored in a content-addressed store where the filename is derived from the SHA512 of the contents using a base58-encoded multihash to support changing the hash algorithm in the future. Additionally, a file contains a pointer from a SHA-1 hash of the form to the contents of the result. This setup allows to distribute the Clerk cache.

Clerk is consumable as a library published on Clojars or as a git dependency using Clojure `tools.deps`. This allows to reproducibly compute a classpath from a deps.edn file. Because Clerk’s hashing is also deterministic (given unchanged dependencies) results can be shared by distributing the cache without needing to track them in version control.

Caching behavior can be disabled on a per-form or per-namespace basis using metadata annotations. There’s also an option to disable Clerk’s caching globally using a system property.

Clojure encourages programming with pure functions and using mutable containers called atoms to isolate mutable state. The value inside an atom can be accessed by dereferencing it for which Clojure includes `@` as a syntax affordance. When using atoms for mutable state, Clerk will attempt to compute a hash based on the value inside an atom for any expression that dereferences it, making Clerk’s caching play nice with how mutable state is most commonly modeled in Clojure.

### Semantic Differences from regular Clojure

The compilation unit in regular Clojure is the top-level form. This allows for excellent interactive development. On the flip side, it's easy for the filesystem state to diverge from the state of the running system. A problem often occurring in practice is deleting a definition from a file without also removing the definition from the runtime. This is often only observed after a restart of the process.

In order to avoid this, Clerk will default to showing an error unless it can find all referenced definitions in the file it is asked to show.

It is our goal to match the semantics of Clojure as closely as possible but as a very dynamic language, there's limits to what Clerk's caching will understand. Here's some of the things we currently do not support:

* Re-definitions of the same var in a file
* Setting dynamic variables using [`set!`](https://clojuredocs.org/clojure.core/set!)
* Dynamically altering vars using [`alter-var-root`](https://clojuredocs.org/clojure.core/alter-var-root)
* Temporarily redefining vars using [`with-redefs`](https://clojuredocs.org/clojure.core/with-redefs)

### Presentation
Clerk's rendering happens in the browser. On the JVM Clojure-side, a given document is _presented_. The name is a nod to a similar system in Common Lisp. In its generalized form, `present` is a function that does a depth-first traversal of a given tree, starting at the root node. It will select a viewer for this root node, and unless told otherwise, descend further down the tree to present its child nodes.

It's possible to use Clerk's presentation system in other contexts we know of at least one case of a user leveraging Clerk's presentation system to do in-process rendering without a browser.[^desk]

[^desk]: [Desk](https://github.com/phronmophobic/desk) by Adrian Smith.

### Built-in Viewers

```clojure
^{::clerk/width :wide}
(clerk/html
 [:div.not-prose.overflow-hidden.rounded-lg
  [:img {:src "https://cdn.nextjournal.com/data/QmQLcS1D9ZLNQB8bz1TivBEL9AWttZdoPMHT9xDASYYm7F?filename=Built-in+Viewers.png&content-type=image/png"}]])
```

Clerk comes with a number of built-in viewers. These include viewers for Clojure’s built-in data structures, HTML (including the hiccup variant that is often used for Clojure and SVG), Plotly, Vega, tables, math code, images, grids as well as a fallback viewer that builds on top of Clojure’s printer via `pr-str`. The [Book of Clerk][book-of-clerk] gives a good overview of the available built-ins. Clerk’s view is running in the browser. We made this choice in order to benefit from its rendering engine and leverage the vast number of libraries in the JS ecosystem. For example we're using [Plotly](https://plotly.com/javascript/) and [vega](https://github.com/vega/vega-embed) for plotting, [CodeMirror](https://codemirror.net) for rendering code cells and [KaTeX](https://katex.org) for typesetting math.

Another benefit of the using the browser for Clerk's rendering layer is that Clerk can produce static HTML pages for publishing to the web. We could not resist the temptation to leverage Clerk for the production of this document and have used it as an excuse to improve the display of sidenotes.

In order to not overload the browser, Clerk’s built-in collection viewer carry an attribute to control for the number of items displayed, allowing to request more data on demand. Besides this simple limit, there’s a second global budget per result to limit the total number of items also for deeply nested data. We’ve found this simple system to work fairly well in practice.

### Moldable Viewer API

Viewer selection and elision of data happens on the JVM in Clojure.  Clerk’s viewers are an ordered collection of plain Clojure hash maps. Clerk willl interpret the following optional keys:

* A `:pred` function to test if a given viewer should be selected;
* a function on the `:transform-fn` key that can perform a transformation of the value. This receives a map argument with the original value under a key. Additional keys carry the path, the viewer stack and the budget.
* A `:render-fn` key containing a quoted form that will be sent to the browser where it will be evaluated using sci[^sci] and turned into a function;
* A `:page-size` key on collection viewers to control how many items to show.

[^sci]: [Small Clojure Interpreter](https://github.com/babashka/sci) by Michiel Borkent

Viewers can also be explicitly selected using functions like `clerk/with-viewer` which will wrap a given value in a map with the given viewer. Alternatively to the explicit functional API, viewers can be selected using metadata on the form. This has no meaning in Clojure and thus won’t in any way affect the value of the program when run without Clerk and is also useful for when downstream consumers rely on a value being used unmodified.

### Sync

Clerk also supports bidirectional sync of state between the SCI viewer environment and the JVM. If an atom is annotated (via metadata) to be synced, Clerk will create a corresponding var in the SCI environment and watch this atom for modifications on both the JVM Clojure and the SCI browser side and broadcast a diff to the other side. In addition, a JVM-side change will cause a recompilation of the currently active document, which means no re-parsing or analysis of the document will be performed but only a re-evaluation of cells dependent on the value inside this atom. This allows to use Clerk for small local-first apps as shown in the [Regex Dictionary Example](#regex-dictionary).

### Experience

Our experience as the developers and users of Clerk has been surprisingly positive but of course we're heavily biased. We've  chosen a few quotes from Clerk's userbase.

> [Clerk] is making the training of junior #Clojure programmers a massive pleasure! [...]
> 
> It helps us to bypass what would otherwise be a lot of distracting UI programming. Set up your env, make a namespace, hit a keybind, hey presto, your code is running in a browser.
> 
> – Robert Stuttaford[^tweets]

[^tweets]: Via a [rapidly degrading social media platform](https://web.archive.org/web/20230119113752/https://twitter.com/RobStuttaford/status/1574328589306281987)

> I'm using Clerk to visualize statistics properties from a simulation in a model checker [...] it's basically a wrapper over TLA+ [...]
>
> Amazing that Clerk just lets you focus on what really matters and nothing else!
>
> – Paulo Feodrippe


## Examples of Moldable Development with Clerk

### Augmenting table names

This example illustrates an approach that we needed to make working with a legacy Db2 database easier.
The database’s column names are made up of 8 character sequences that can’t be read much out of:

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#figure-2.not-prose.overflow-hidden.rounded-lg
  [:img {:src "https://cdn.nextjournal.com/data/QmWnzjc5c9qpUUaLoK3ytZk4Zs1AzDpZj1Tx5FF4ZR8a5t?filename=AS400-Cut.png&content-type=image/png"}]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong "Figure 2: "] "AS/400 Column Names"]]])
```

One can’t read much out of those names but it turns out there is a metaschema available that maps those 8-character names to human-readable (German-only) names (which we can then translate to English names). In typical LISP fashion, we go on and inspect a query from the REPL. We can use the translated names in the table even print them but one quickly sees the limit of plain-text printing:

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#figure-3.not-prose.overflow-hidden.rounded-lg
  [:video {:loop true :controls true}
   [:source {:src "https://cdn.nextjournal.com/data/QmbGFKpEXLGyqngHe7q1dqAsEAWfotSHG8XxYZPQfHirQ1?content-type=video/mp4"}]]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong "Figure 3: "] "Inspecting A Query Using the REPL"]]])
```

With Clerk, we can render the output as graphical table without the limitations of plain text. Further, we can use the Viewer API to extend the table viewer’s headings to show the translated metaschema names (plus showing the original 8 character names in a de-emphasized way so that they aren’t lost). We can go further still and also show the original German names when move the mouse over the headings:

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#figure-4.not-prose.overflow-hidden.rounded-lg
  [:video {:loop true :controls true}
   [:source {:src "https://cdn.nextjournal.com/data/QmVZsXxsX2wcYYc758yHkZjijW2HdZhaGcfQaHpAkZeqWk?content-type=video/mp4"}]]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong "Figure 4: "] "Augmented Table Headings"]]])
```

### Rich documentation features

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#figure-5.not-prose.overflow-hidden.rounded-lg
  [:img {:src "https://cdn.nextjournal.com/data/QmQgTLi8qfzrBRTkaAGfWQ4RceM4v3fp4Wna7knivMgusb?filename=clojure2d-color.png&content-type=image/png"}]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong "Figure 5: "] "Custom Viewers for Clojure2d’s Colors Library"]]])
```

This example illustrates the use of Clerk to create rich documentation for `clojure2d`’s colors package. They used Clerk’s Viewer API to implement custom viewers to visualize colors, gradients and color spaces.

### Regex Dictionary

Built as a showcase for Clerk’s sync feature, this example allows entering a regex into a text input and get dictionary matches as result while you type:

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#figure-6.not-prose.overflow-hidden.rounded-lg
  [:video {:loop true :controls true}
   [:source {:src "https://cdn.nextjournal.com/data/QmTwZWw4FQT6snxT8RkKt5P7Vxdt2BjM6ofbjKYEcvAZiq?content-type=video/mp4"}]]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong "Figure 6: "] "Interactive Regex Dictionary"]]])
```

It is built using a Clojure atom containing the text input’s current value that is synced between the JVM and the browser. As you type into the input, the atom’s content will be updated and synced. As such, printing the atom’s content in your editor will show the input’s current value:

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#figure-7.not-prose.overflow-hidden.rounded-lg
  [:img {:src "https://cdn.nextjournal.com/data/QmNS2jigrDn2WdS7AVa4qMiWtwZovJmfzYbWczwg1Ptaqk?filename=Regex+Value+Cut.png&content-type=image/png"}]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong "Figure 7: "] "Printing the value of a synced Clojure atom"]]])
```

### [Lurk](https://github.com/nextjournal/lurk): Interactive Lucene-powered Log Search

Also building on Clerk’s sync feature, this interactive log search uses [Lucene](https://lucene.apache.org/) on the JVM side to index and search a large number of log entries. In addition to using query input, logs can also be filtered by timeframe via an interactive chart. It is worth noting that this example has a completely custom user interface styling (nothing left of Clerk’s default styling) via Clerk’s CSS customization options.

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#figure-8.not-prose.overflow-hidden.rounded-lg
  [:video {:loop true :controls true}
   [:source {:src "https://cdn.nextjournal.com/data/QmRtGb5aByKD6i5SsxfS1JCJPKpC1kW5wbGvmT1h6awyB9?content-type=video/mp4"}]]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong "Figure 8: "] "Interactive Log Search"]]])
```

## Related & Future Work

🚧 🚧 🚧

Related:
* org-mode
* Ron Avitzur's Milo
* Macsyma
* Tioga/Camino Real
* MathCAD
* Mathematica
* Jupyter
* Observable Notebooks
* R Markdown
* Newspeak
* Glamorous Toolkit

Future Work
* Viewers: lets `:pred` function opt into more context
* Open up caching
* Use distributed cache more
* Make caching more granular, also allow caching functions?
* Clerk printer to fix REPL printing problem
* Open toolbox


## Conclusion

🚧 🚧 🚧

-----------------------------------------------
[book-of-clerk]:https://book.clerk.vision
[nextjournal]:https://nextjournal.com
[maria]:https://maria.cloud
