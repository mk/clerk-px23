# Clerk: Moldable Live Programming for Clojure

```clojure
(ns nextjournal.clerk.px23
  {:nextjournal.clerk/toc true
   :nextjournal.clerk/visibility {:code :hide}}
  (:require [nextjournal.clerk :as clerk]
            [applied-science.edn-datasets :as datasets]))
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
    "Clerk is an open source Clojure programmer’s assistant that builds upon the traditions of interactive and literate programming to provide a holistic moldable development environment. Clerk layers static analysis, incremental computation, and rich browser-based graphical presentations on top of a Clojure programmer's familiar toolkit to enhance their workflow."]]
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

Knuth's _Literate Programming_ [^literateprogramming] emphasized the importance of focusing on human beings as consumers of computer programs. His original implementation involved authoring files that combine source code and documentation, which were then divided into two derived artifacts: source code for the computer and a typeset document in natural language to explain the program.

[^literateprogramming]: An extensive archive of related material is maintained [here](http://www.literateprogramming.com).

At the same time, other software was developed to target scientific use cases rather than program documentation. These systems, which prefigured modern computational notebooks, ranged from REPL-driven approaches like Macsyma and Mathematica to integrated WYSIWYG editors like Ron Avitzur's _Milo_, PARC's _Tioga_ and _Camino Real_, and commercial software like _MathCAD_[^mathematical-software].

[^mathematical-software]: See [A Survey of User Interfaces for Computer Algebra Systems](https://people.eecs.berkeley.edu/~fateman/temp/kajler-soiffer.pdf) for a history of these systems up until 1998.

In contemporary data science and software engineering practice, we often see interfaces that combine these two approaches, like [Jupyter](https://jupyter.org) and [Observable](https://observablehq.com). In these notebooks, a user can mix prose, code, and visualizations in a single document that provides the advantages of Knuth's Literate Programming with those of a scientific computing environment. Unfortunately, most such systems require the programmer to use a browser-based editing environment (which alienates programmers with a strong investment in their own tooling) and custom file formats (which cause problems for integration with broader software engineering practices)[^notebook-pain-points].

[^notebook-pain-points]: See [What’s Wrong with Computational Notebooks? Pain Points, Needs, and Design Opportunities](https://doi.org/10.1145/3313831.3376729) by Souti Chattopadhyay, Ishita Prasad, Austin Z. Henley, Anita Sarma and Titus Barik.

Although notebooks of this kind present an improvement on the programming experience of many languages, they often feel like a step backward to experienced Lisp programmers. In Lisp environments, it is common to be able to place the cursor after a single Lisp form and evaluate it in the context of a running program, providing finer granularity of control compared to the per-cell model of most notebooks. This workflow leads to a development style that these programmers are loath to lose.

> That LISP users tend to prefer structured growth rather than stepwise refinement is not an effect of the programming system, since both methods are supported. I believe, however, that it is a natural consequence of the interactive development method, since programs in early stages of growth can be executed and programs in early stages of refinement cannot.[^sandewall]
>
> – Erik Sandewall

[^sandewall]: See [Programming in an Interactive Environment: the "Lisp" Experience](https://doi.org/10.1145/356715.356719) by Erik Sandewall

At the same time, though a number of Lisp environments have included graphical presentations of program objects[^mcclim], the default Clojure[^clojure] development experience relies on text-based representations of evaluation output and doesn't include the ability to embed widgets for direct manipulation of program state. Additionally, problems often arise when printing structurally large results, which can cause editor performance to degrade or lead to the truncation of output, and there's limited room for customization or support for requesting more data.

[^mcclim]: See, for example, the [Common Lisp Interface Manager](https://en.wikipedia.org/wiki/Common_Lisp_Interface_Manager).

[^clojure]: For a description of the language and its motivations, see [A history of Clojure](https://dl.acm.org/doi/10.1145/3386321).

In comparison, interactive programming in Smalltalk-based systems has included GUI elements since the beginning, and work to further improve programmer experience along these lines has continued in Smalltalk-based systems like [Self](https://selflanguage.org), [Pharo](https://pharo.org), [Glamorous Toolkit](https://gtoolkit.com)[^moldable-tools] and [Newspeak](https://newspeaklanguage.org)[^ample-forth], which offer completely open and customizable integrated programming environments. Glamorous Toolkit, in particular, champions the idea of using easily constructed custom tools to improve productivity and reduce time spent on code archeology, which is also a big inspiration for what we'll present here.

[^moldable-tools]: See [Towards Moldable Development Tools](https://doi.org/10.1145/2846680.2846684) by Andrei Chiş, Oscar Nierstrasz and Tudor Gîrba

[^ample-forth]: See [Ampleforth: A Live Literate Editor](https://blog.bracha.org/Ampleforth-Live22/out/primordialsoup.html?snapshot=Live22Submission.vfuel) by Gilad Bracha

## Programming with Clerk

> In such a future working relationship between human problem-solver and computer ‘clerk’, the capability of the computer for executing mathematical processes would be used whenever it was needed.[^engelbart]
>
> – Douglas Engelbart

[^engelbart]: See [Augmenting Human Intellect: A Conceptual Framework](https://www.dougengelbart.org/pubs/augment-3906.html) by Douglas Engelbart.

### Basic Interaction: Bring-Your-Own-Editor

Clerk combines Lisp-style interactive programming with the benefits of computational notebooks, literate programming, and moldable development, all without asking programmers to abandon their favorite tools or give up their existing software engineering practices. Its design stems partially from the difficult lessons we learned after years of unsuccessfully trying to get our _own team_ to use an [online browser-based notebook platform][nextjournal] that we also developed.

When working with Clerk, a split-view is typically used with a code editor next to a browser showing Clerk’s representation of the same notebook, as [seen in _Clerk side-by-side with Emacs_](#clerk-side-by-side-with-emacs).

``` clojure
^{::clerk/width :full}
(clerk/html
 [:div#clerk-side-by-side-with-emacs.not-prose
  [:video {:loop true :controls true}
   [:source {:src "https://cdn.nextjournal.com/data/QmVYLx5SByNZi9hFnK2zx1K6Bz8FZqQ7wYtAwzYCxEhvfh?content-type=video/mp4"}]]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong.mr-1 "Figure:"] "Clerk side-by-side with Emacs"]]])
```

As shown here, our _notebooks_ are just source files containing regular Clojure code. Block comments are treated as markdown text with added support for LaTeX, data visualization, and so on, while top-level forms are treated as code cells that show the result of their evaluation. This format allows us to use Clerk in the context of production code that resides in revision control. Because files decorated with these comment blocks are legal code without Clerk loaded, it they can be used in many contexts where traditional notebook-specific code cannot. This has led, among other things, to Clerk being used extensively to publish documentation for libraries that are then able to ship artifacts that have no dependency on Clerk itself[^maria].

[^maria]: We have borrowed this approach from [maria.cloud][maria], a web-hosted interactive Clojure learning tool created by [Matt Huebert](https://matt.is), [Dave Liepmann](https://www.daveliepmann.com/), and [Jack Rusher](https://jackrusher.com/). Maria grew out of [work presented at PX16](https://px16.matt.is) by Matt Huebert.

Clerk’s audience is experienced Clojure developers who are familiar with interactive development. They are able to continue programming in their accustomed style, evaluating individual forms and inspecting intermediate results, but with the added ability to `show!` a namespace/file in Clerk. A visual representation of the file is then re-computed either:

* every time the file is saved, using an an optional file watcher; or alternatively,
* via an editor hot-key that can be bound to show the current document. (The authors generally prefer the hot-key over the file watcher, as it feels more direct and gives more control over when to show something in Clerk.)

Control and configuration of Clerk primarily occurs through evaluation of Clojure forms from within the programmer's environment, rather than using outside control panels and settings. This integration with the programmer's existing tooling eases adoption and allows advanced customization of the system through code.

### Fast Feedback: Caching & Incremental Computation

To keep feedback loops short and avoid excess re-computation, Clerk uses dependency analysis to recompute only the minimum required subset of a file's forms. In addition, it optionally caches the results of long-running computations to disk to allow the user to continue work after a restart without recomputing potentially expensive operations[^data-ingestion]. This caching behavior can be fine-tuned (or disabled) down to the level of individual forms.

[^data-ingestion]: In tasks with intensive data preparation steps, this savings can be considerable. It's also possible to share Clerk's immutable, content-addressed cache between users so a given computation is performed only once for a workgroup.

🚧 TODO too low level/not focused on end-user experience? 🤔

Clerk begins by parsing and analyzing the code in a given file, then performs macro expansion and recursively traverses each form's dependencies, collecting them in a graph. For each top-level form, a hash is computed from the form and its dependencies. Next, Clerk evaluates each form unless it finds a cached value for that form. Because Clojure supports lazy evaluation of potentially infinite sequences, safeguards are in place to skip caching unreasonable values.

On-disk caches use a content-addressed store where each filename is derived from the hash of the file's contents using a base58-encoded multihash. Additionally, each file contains a pointer from the hash of the form to the result file, which allows us to indirect lookups to, for example, a remote storage service. This combination of immutability and indirection makes distributing sharing of the cache trivial. 

🚧

> While I did believe, and it has been true in practice, that the vast majority of an application could be functional, I also recognized that almost all programs would need some state. Even though the host interop would provide access to (plenty of) mutable state constructs, I didn’t want state management to be the province of interop; after all, a point of Clojure was to encourage people to stop doing mutable, stateful OO. In particular I wanted a state solution that was much simpler than the inherently complex locks and mutexes approaches of the hosts for concurrency-safe state. And I wanted something that took advantage of the fact that Clojure programmers would be programming primarily with efficiently persistent immutable data.[^history-of-clojure]

[^history-of-clojure]: [A History of Clojure](https://download.clojure.org/papers/clojure-hopl-iv-final.pdf), Rich Hickey

It is idiomatic in Clojure to use boxed containers to manage mutable state[^clojure-state]. While there are several of these constructs in the language, in practice [atoms](https://clojure.org/reference/atoms) are the most popular by far. An atom allows reading the current value inside it with [`deref/@`](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/deref) and updating it's value with [`swap!`](https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/swap!).

[^clojure-state]: [Values and Change: Clojure’s approach to Identity and State](https://clojure.org/about/state)

When Clerk encounters an expression in which an atom's mutable value is being read using `deref`, it will try to compute a hash based on the value _inside_ the atom  at runtime, and extend the expression's static hash with it. This extension makes Clerk's caching work naturally with idiomatic use of mutable state, and frees programmers from needing to manually opt out of caching for those expressions.

### Semantic Differences from regular Clojure

Clojure uses a single-pass, whole-file compilation strategy in which each evaluated form is added to the state of the running system. One positive aspect of this approach is that manually evaluating a series of forms produces the same result as loading a file containing the same forms in the same order, which is a useful property when interactively building up a program.

A practical concern with this sort of "bottom-up" programming is that the state of the system can diverge from the state of the source file, as forms that have been deleted from the source file may still be present in the running system. This can lead to a situation where newly written code depends on values that will not exist the next time the program runs, leading to surprising errors. To help avoid this, Clerk defaults to showing an error unless it can resolve all referenced definitions in both the runtime and the source file.

It is our goal to match the semantics of Clojure as closely as possible but as a very dynamic language, there are limits to what Clerk's analysis can handle. Here are some of the things we currently do not support:

* Multiple definitions of the same var in a file
* Setting dynamic variables using [`set!`](https://clojuredocs.org/clojure.core/set!)
* Dynamically altering vars using [`alter-var-root`](https://clojuredocs.org/clojure.core/alter-var-root)
* Temporarily redefining vars using [`with-redefs`](https://clojuredocs.org/clojure.core/with-redefs)

We have included a mechanism to override Clerk's error checking in cases where the user knows that one or more of these techniques are in use.

### Presentation

Clerk uses a client/server architecture. The server runs in the JVM process that hosts the user's development environment. The client executes in a web browser running an embedded Clojure interpreter[^sci].

[^sci]: [Small Clojure Interpreter](https://github.com/babashka/sci) by Michiel Borkent

The process of conveying a value to the client is a _presentation_, a
term taken from Common Lisp systems that support similar features [^presentations]. The process of presentation makes use of _viewers_, each of which is a map from well-known keys to quoted forms containing source code for Clojure functions that specify how the client should render data structures of a given type. When a viewer form is received on the client side, it is compiled into a function that will be then called on data later sent by the server.

[^presentations]: This feature originated on the Lisp Machine, and lives on in a reduced form as a feature of the emacs package [Slime](https://slime.common-lisp.dev/doc/html/Presentations.html).

When the `present` function is called on the server side, it defaults to performing a depth-first traversal of the data structure it receives, attaching appropriate viewers at each node of the tree. The resulting structure containing both data and viewers is then sent to the client.

To avoid overloading the browser or producing uselessly large output, Clerk’s built-in collection viewer carries an attribute to control the number of items initially displayed, allowing more data to be requested by the user on demand. Besides this simple limit, there’s a second global _budget_ per result to limit the total number of items shown in deeply nested data structures. We’ve found this simple system to work fairly well in practice.

One benefit of using the browser for Clerk's rendering layer is that it can produce static HTML pages for publication to the web. We could not resist the temptation to produce this document with Clerk, and have used that experience as an opportunity to improve the display of sidenotes.

It's also possible to use Clerk's presentation system in other contexts. We know of at least one case of a user leveraging Clerk's presentation system to do in-process rendering without a browser.[^desk]

[^desk]: [Desk](https://github.com/phronmophobic/desk) by Adrian Smith.

### Built-in Viewers

```clojure
^{::clerk/width :wide}
(clerk/html
 [:div.not-prose.overflow-hidden.rounded-lg
  [:img {:src "https://cdn.nextjournal.com/data/QmQLcS1D9ZLNQB8bz1TivBEL9AWttZdoPMHT9xDASYYm7F?filename=Built-in+Viewers.png&content-type=image/png"}]])
```

Clerk comes with a set of built-in viewers for common situations. These include support for Clojure’s immutable data structures, HTML (including the [hiccup variant](https://github.com/weavejester/hiccup) that is often used in Clojure to represent HTML and SVG), data visualization, tables, LaTeX, source code, images, and grids, as well as a fallback viewer based on Clojure’s printer. The [Book of Clerk][book-of-clerk] gives a good overview of the available built-ins. Because Clerk’s client is running in the browser, we are able to benefit from the vast JS library ecosystem. For example we're using [Plotly](https://plotly.com/javascript/) and [vega](https://github.com/vega/vega-embed) for plotting, [CodeMirror](https://codemirror.net) for rendering code cells, and [KaTeX](https://katex.org) for typesetting math.

Clerk’s built-in viewers try to suit themselves to typical Data Science use cases. By default, Clerk shows a code block’s result as-is with some added affordances like syntax coloring and expandability of large sub-structures that are collapsed by default. 

Here is an interactive example of the well-known `iris` data set that has been added as a dependency to this notebook. Clicking the disclosure triangles will expand the data structure.

```clojure
^{::clerk/visibility {:code :show}}
datasets/iris
```

Additional affordances are modes to auto-expand nested structures based on shape heuristics and expanding multiple sub-structures of the same level:

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#expanding-multiple-sub-structures-at-once.not-prose.overflow-hidden.rounded-lg
  [:video {:loop true :controls true}
   [:source {:src "https://cdn.nextjournal.com/data/QmciJrXQguekgeX6LsXUmvNthadkN2Eu4RMpMXzbKN6JDg?content-type=video/mp4"}]]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong.mr-1 "Figure: "] "Expanding multiple sub-structures at once"]]])
```

Using the built-in `clerk/table` viewer, the same data structure can also be rendered as table. The table viewer is using heuristics to infer the makeup of the table, such as column headers, from the structure of the data:

``` clojure
^{::clerk/visibility {:code :show}}
(clerk/table datasets/iris)
```

Together with tables, plots make up for the most common Data Science use cases. Clerk comes with built-in support for the popular [vega](https://github.com/vega/vega-embed) and [Plotly](https://plotly.com/javascript/) plotting grammars. 

In the following figure, the same `iris` dataset, as shown in the above table example, is used to render an interactive `vega-lite` plot using the `clerk/vl` viewer:

``` clojure
^{::clerk/visibility {:code :show}}
(clerk/vl {:data {:values datasets/iris}
           :width 500
           :height 500
           :title "sepal-length vs. sepal-width"
           :mark {:type "point"
                  :tooltip {:field :species}}
           :encoding {:color {:field :species}
                      :x {:field :sepal-length
                          :type :quantitative
                          :scale {:zero false}}
                      :y {:field :sepal-width
                          :type :quantitative
                          :scale {:zero false}}}
           :embed/opts {:actions false}})
```

It is important to note that Clerk’s viewers work in a way that encourages composition. Multiple viewers can be combined to suit a specific use case such as the following example showing a table of airline passenger numbers[^box+jenkins] by year and quarter and embedding a sparkline graph into the table row for each year.

[^box+jenkins]: using a [Clojure port](https://github.com/applied-science/edn-datasets) of R’s built-in dataset of [Box & Jenkins classic airline data](https://search.r-project.org/R/refmans/datasets/html/AirPassengers.html).

A typical Clerk workflow for this would be to first take a look at the shape of the data:

```clojure
^{::clerk/visibility {:code :show}
  ::clerk/opts {:auto-expand-results? true}}
datasets/air-passengers
```

Then, a `sparkline` function is defined that generates the graph (using `clerk/vl`) to be embedded into each table row later:

```clojure
^{::clerk/visibility {:code :show}}
(defn sparkline [values]
  (clerk/vl {:data {:values (map-indexed (fn [i n] {:x i :y n}) values)}
             :mark {:type :line :strokeWidth 1.2}
             :width 140
             :height 20
             :config {:background nil :border nil :view {:stroke "transparent"}}
             :encoding {:x {:field :x :type :ordinal :axis nil :background nil}
                        :y {:field :y :type :quantitative :axis nil :background nil}}
             :embed/opts {:actions false}}))
```

And finally reducing the data to quarters and years and adding the sparkline graphs in a final mapping step:

```clojure
^{::clerk/visibility {:code :show}}
(clerk/table
 {:head ["Year" "Q1" "Q2" "Q3" "Q4" "Trend"]
  :rows (->> datasets/air-passengers
             (group-by :year)
             (map (fn [[year months]]
                    (let [qs (->> months (map :n) (partition 3) (map #(reduce + %)))]
                      (concat [year] qs [(sparkline qs)]))))
             (sort-by first))})
```

### Moldable Viewer API

Clerk’s viewers are an ordered (and thus prioritized) collection of plain Clojure hash maps. Clerk interprets the following optional keys in each viewer map:

* `:pred` is a predicate function that tests whether this viewer should be used for a given data structure
* `:transform-fn` is an optional function run on the server side to transform data before sending it to the client. It receives a map argument with the original value under a key. Additional keys carry the path, the viewer stack, and the budget (for elision)
* `:render-fn` is a quoted form that will be sent to the browser, where it will be compiled into a function that will be called to display the data
* `:page-size` is a number that indicates how many items to send in each chunk during elision/pagination

Viewers can also be explicitly selected by wraping a value in the `clerk/with-viewer` function, which produces a presentation for that value using that viewer. Alternatively, viewers can be selected by placing a Clojure metadata declaration before a form. Because of the way Clojure handles compilation, metadata in this position is ultimately ignored in the generated code. So far as we know, this is a novel mechanism for out of band signaling to a specialized Clojure parser. TODO add example viewer source and metadata declaration here

The process of selecting viewers happens programmatically on the server side, thus using the programmer's already existing interactive programming environment as a user interface.

### Sync

To help with creating interactive tools using Clerk, it also supports bidirectional sync of state between the client and server Clojure environments. If a Clojure `atom` on the server is annotated with metadata indicating it is `sync`, Clerk will create a corresponding var in the client environment. Both of these atoms will be automatically instrumented with an update watcher that broadcasts a _diff_ to the other side.

In addition, a server-side change will trigger a refresh of the currently active document, which will then re-calculate the minimum subset of the document that is dependent on that atom's value. This allows us to use Clerk for small local-first apps, as shown in the [Regex Dictionary Example](#regex-dictionary).

## Prose-oriented Documents

The first and primary use case for Clerk was adding prose, visualizations, and interactivity to Clojure namespaces. However, when writing documents that are mainly prose, but would benefit from _some_ computational elements, it is rather tedious to write everything in comment blocks. To make this easier, Clerk can also operate on markdown files with “code-fenced” source code blocks. All Clojure source blocks in such a file are evaluated and replaced in the generated document with their result.

This format is very similar to other markdown-based notebooks, like [R Markdown](https://rmarkdown.rstudio.com), but specifically tailored to Clojure. We used this approach to write this paper, the source for which is located [on Github](https://github.com/mk/clerk-px23)[^github-format].

[^github-format]: One nice thing about this approach is that other systems, like Github, are able to render a reasonable version of the document, though without evaluation.

## Examples of Moldable Development with Clerk

In addition to the sorts of traditional data science use cases that one might expect from something that has "notebook" features, we intend Clerk to be a general purpose programmer's assistant[^programmers-assistant] that allows the rapid construction of tiny interfaces during daily work. Here are a few samples of tools and documentation created in this manner.

[^programmers-assistant]: We use this term in appreciation of pioneering historical work by Warren Teitelman.

### Augmenting table names

This example illustrates an approach we used to make working with a legacy DB2 database easier. The database’s column names are made up of largely human-unreadable 8 character sequences:

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#as400-column-names.not-prose.overflow-hidden.rounded-lg
  [:img {:src "https://cdn.nextjournal.com/data/QmWnzjc5c9qpUUaLoK3ytZk4Zs1AzDpZj1Tx5FF4ZR8a5t?filename=AS400-Cut.png&content-type=image/png"}]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong.mr-1 "Figure:"] "AS/400 Column Names"]]])
```

We were able to automatically translate these names using a metaschema extracted from the database. This allowed us to create a viewer that maps those 8-character names to human-readable (German-only) names (which we can then translate to English names). In typical Lisp fashion, we go on and inspect a query interactively. We can use the translated names in the table even print them but one quickly sees the limit of plain-text printing:

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#inspecting-a-query-using-the-repl.not-prose.overflow-hidden.rounded-lg
  [:video {:loop true :controls true}
   [:source {:src "https://cdn.nextjournal.com/data/QmbGFKpEXLGyqngHe7q1dqAsEAWfotSHG8XxYZPQfHirQ1?content-type=video/mp4"}]]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong.mr-1 "Figure:"] "Inspecting A Query Using the REPL"]]])
```

With Clerk, were able to render the output as a graphical table without the limitations of plain text. Further, we can use the Viewer API to extend the table viewer’s headings to show the translated metaschema names (plus showing the original 8 character names in a de-emphasized way so that they aren’t lost). We can go further still, showing the original German names when move the mouse over the headings:

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#augmented-table-headings.not-prose.overflow-hidden.rounded-lg
  [:video {:loop true :controls true}
   [:source {:src "https://cdn.nextjournal.com/data/QmVZsXxsX2wcYYc758yHkZjijW2HdZhaGcfQaHpAkZeqWk?content-type=video/mp4"}]]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong.mr-1 "Figure:"] "Augmented Table Headings"]]])
```

### Rich documentation features

This example illustrates the use of Clerk to create rich documentation for `clojure2d`’s colors package. They used Clerk’s Viewer API to implement custom viewers to visualize colors, gradients and color spaces, then publish that documentation on the web by generating a static website directly from the source code of the library.

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#custom-viewers-for-clojure2ds-colors-library.not-prose.overflow-hidden.rounded-lg
  [:img {:src "https://cdn.nextjournal.com/data/QmQgTLi8qfzrBRTkaAGfWQ4RceM4v3fp4Wna7knivMgusb?filename=clojure2d-color.png&content-type=image/png"}]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong.mr-1 "Figure:"] "Custom Viewers for Clojure2d’s Colors Library"]]])
```

### Regex Dictionary

Built as a showcase for Clerk’s sync feature, this example allows entering a regex into a text input and get dictionary matches as result while you type:

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#interactive-regex-dictionary.not-prose.overflow-hidden.rounded-lg
  [:video {:loop true :controls true}
   [:source {:src "https://cdn.nextjournal.com/data/QmTwZWw4FQT6snxT8RkKt5P7Vxdt2BjM6ofbjKYEcvAZiq?content-type=video/mp4"}]]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong.mr-1 "Figure:"] "Interactive Regex Dictionary"]]])
```

It is built using a Clojure atom containing the text input’s current value that is synced between the client and server. As you type into the input, the atom’s content will be updated and synced. Consequently, printing the atom’s content in your editor will show the input’s current value:

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#printing-the-value-of-a-synced-clojure-atom.not-prose.overflow-hidden.rounded-lg
  [:img {:src "https://cdn.nextjournal.com/data/QmNS2jigrDn2WdS7AVa4qMiWtwZovJmfzYbWczwg1Ptaqk?filename=Regex+Value+Cut.png&content-type=image/png"}]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong.mr-1 "Figure:"] "Printing the value of a synced Clojure atom"]]])
```

### [Lurk](https://github.com/nextjournal/lurk): Interactive Lucene-powered Log Search

Also building on Clerk’s sync feature, this interactive log search uses [Lucene](https://lucene.apache.org/) on the JVM side to index and search a large number of log entries. In addition to using query input, logs can also be filtered by timeframe via an interactive chart. It is worth noting that this example has a completely custom user interface styling (nothing left of Clerk’s default styling) via Clerk’s CSS customization options.

``` clojure
^{::clerk/width :wide}
(clerk/html
 [:div#interactive-log-search.not-prose.overflow-hidden.rounded-lg
  [:video {:loop true :controls true}
   [:source {:src "https://cdn.nextjournal.com/data/QmRtGb5aByKD6i5SsxfS1JCJPKpC1kW5wbGvmT1h6awyB9?content-type=video/mp4"}]]
  [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
   [:div.mx-auto.max-w-prose.px-8 [:strong.mr-1 "Figure:"] "Interactive Log Search"]]])
```

### Experience

Our experience as the developers and users of Clerk has been surprisingly positive, but we're heavily biased. We've chosen a few quotes from Clerk's user base to give a sense of how it has been received in the community:

> [Clerk] is making the training of junior Clojure programmers a massive pleasure! [...]
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

> I just wanted to express some gratitude for Clerk. It’s been a game changer for me in terms of understanding problems and communicating that understanding to other people. 
>
> – Jeffrey Simon


## Related & Future Work

Besides the aforementioned work there's a number of contemporary related systems:

* [Org mode][org-mode] is a major mode for Emacs supporting polyglot literate programming based on a plain text format.
* [Streamlit][streamlit] is a Python library that eshews a custom format and enables building a web UI on regular python scripts. It's [caching system][streamlit-cache] memoizes functions that are tagged using Python's decorators.
* [Pluto][pluto] is a Julia library that uses static analysis to   enable incremental computation and two-way bindings. It does come with a web-based editor. It's format a plain Julia files comment annotations for cell ids and order.
* [Livebook][livebook] is an Elixir notebook with code editing in the browser and explicit per-cell execution. It serializes notebooks to a Markdown format.

Our goal with the development of Clerk is to _leave the toolbox open_: we want Clerk's users to be able to customize behavior, often by providing functions.

Clerk's viewer api is a first example of that but we want to take this further by letting users:

* provide functions to control the caching e.g. to support more efficient caching of dataframes
* letting the viewer api's `:pred` function opt into receiving more context like the path in the tree
* make caching more granular and support caching function invocations
* override `parse` and `eval` to support different syntaxes than markdown and different semantics

So far we've mainly used Clerk's caching on local machines in isolation. We plan to share a distributed cache within our dev team in order to learn about the benefits and challenges this can bring. We also want to extend Clerk to better communicate caching behavior to its users (why a value could or could not be cached, if it was cached in memory or on-disk).

We've been talking about ways to write changes originating from controls in Clerk's view back to the source files. We also believe that for this to be a good developer experience, it's insufficient for this to be on the level of source files but concurrent modifications without intermediate saving should be supported. Since this is a significant chunk of work, we've avoided it until now.

## Conclusion 
🚧

-----------------------------------------------
[book-of-clerk]:https://book.clerk.vision
[nextjournal]:https://nextjournal.com
[maria]:https://maria.cloud
[streamlit]:https://streamlit.io
[streamlit-cache]:https://docs.streamlit.io/library/get-started/main-concepts#caching
[pluto]:https://plutojl.org
[livebook]:https://livebook.dev
[org-mode]:https://orgmode.org