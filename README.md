# Clerk: Moldable Live Programming for Clojure

```clojure
(ns nextjournal.clerk.px23
  {:nextjournal.clerk/toc true
   :nextjournal.clerk/open-graph
   {:url "https://px23.clerk.vision"
    :title "Clerk: Moldable Live Programming for Clojure"
    :description "Clerk is an open source Clojure programmer’s assistant that builds upon the traditions of interactive and literate programming to provide a holistic moldable development environment. Clerk layers static analysis, incremental computation, and rich browser-based graphical presentations on top of a Clojure programmer's familiar toolkit to enhance their workflow."}
   :nextjournal.clerk/visibility {:code :hide}}
  (:require [nextjournal.clerk :as clerk]
            [applied-science.edn-datasets :as datasets]))
```

```clojure
^{::clerk/visibility {:result :hide}}
(defn figure [{:as opts ::clerk/keys [width]}]
  (clerk/with-viewer
    {:transform-fn (clerk/update-val
                    (fn [{:keys [id src caption video?]}]
                      (clerk/html
                       [:div.not-prose.overflow-hidden {:id id :class (when-not (= :full width) "rounded-lg")}
                        (if video?
                          [:video {:loop true :controls true} [:source {:src src}]]
                          [:img {:src src}])
                        (when caption
                          [:div.bg-slate-100.dark:bg-slate-800.dark:text-white.text-xs.font-sans.py-4
                           [:div.mx-auto.max-w-prose.px-8 [:strong.mr-1 "Figure:"] caption]])])))}
    opts opts))
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
   [:a.block.mt-1.hover:opacity-70 {:href "https://twitter.com/jackrusher"} "Jack Rusher"]
   [:a.block.mt-2.hover:opacity-70 {:href "https://2023.programming-conference.org/home/px-2023/"}"Presented at PX/23"]]])
```

## Introduction: Literate Programming, Notebooks and Interactive Development

Knuth's _Literate Programming_[^literateprogramming][^knuth84] emphasized the importance of focusing on human beings as consumers of computer programs. His original implementation involved authoring files that combine source code and documentation, which were then divided into two derived artifacts: source code for the computer and a typeset document in natural language to explain the program.

[^knuth84]: [Literate Programming](https://doi.org/10.1093/comjnl/27.2.97)
[^literateprogramming]: An extensive archive of related material is maintained [here](http://www.literateprogramming.com).

At the same time, other software was developed to target scientific use cases rather than program documentation. These systems, which prefigured modern computational notebooks, ranged from REPL-driven approaches like Macsyma and Mathematica to integrated WYSIWYG editors like Ron Avitzur's _Milo_, PARC's _Tioga_ and _Camino Real_, and commercial software like _MathCAD_.[^mathematical-software]

[^mathematical-software]: See [A Survey of User Interfaces for Computer Algebra Systems](https://people.eecs.berkeley.edu/~fateman/temp/kajler-soiffer.pdf) for a history of these systems up until 1998 ([DOI](https://doi.org/10.1006/jsco.1997.0170)).

In contemporary data science and software engineering practice, we often see interfaces that combine these two approaches, like [Jupyter](https://jupyter.org), [Observable](https://observablehq.com), [Pluto][pluto], and [Livebook][livebook]. In these notebooks, a user can mix prose, code, and visualizations in a single document that provides the advantages of Knuth's Literate Programming with those of a scientific computing environment. Unfortunately, most such systems require the programmer to use a browser-based editing environment (which alienates programmers with a strong investment in their own tooling) and custom file formats (which cause problems for integration with broader software engineering practices).[^notebook-pain-points]

[^notebook-pain-points]: See [What’s Wrong with Computational Notebooks? Pain Points, Needs, and Design Opportunities](https://doi.org/10.1145/3313831.3376729) by Souti Chattopadhyay, Ishita Prasad, Austin Z. Henley, Anita Sarma and Titus Barik.

Although notebooks of this kind present an improvement on the programming experience of many languages, they often feel like a step backward to experienced Lisp programmers. In Lisp environments, it is common to be able to place the cursor after a single Lisp form and evaluate it in the context of a running program, providing finer granularity of control compared to the per-cell model of most notebooks. This workflow leads to a development style that these programmers are in no hurry to lose.

> That LISP users tend to prefer structured growth rather than stepwise refinement is not an effect of the programming system, since both methods are supported. I believe, however, that it is a natural consequence of the interactive development method, since programs in early stages of growth can be executed and programs in early stages of refinement cannot.[^sandewall]
>
> – Erik Sandewall

[^sandewall]: See [Programming in an Interactive Environment: the "Lisp" Experience](https://doi.org/10.1145/356715.356719) by Erik Sandewall

At the same time, though a number of Lisp environments have included graphical presentations of program objects[^mcclim], most modern tooling relies on text-based representations of evaluation output and doesn't include the ability to embed widgets for direct manipulation of program state. Additionally, problems often arise when printing structurally large results, which can cause editor performance to degrade or lead to the truncation of output, and there's limited room for customization or support for requesting more data.

[^mcclim]: See, for example, the [Common Lisp Interface Manager](https://en.wikipedia.org/w/index.php?title=Common_Lisp_Interface_Manager&oldid=1121151005).

In comparison, interactive programming in Smalltalk-based systems has included GUI elements since the beginning, and work to further improve programmer experience along these lines has continued in Smalltalk-based systems like [Self](https://selflanguage.org)[^Ungar87], [Pharo](https://pharo.org), [Glamorous Toolkit](https://gtoolkit.com)[^moldable-tools] and [Newspeak](https://newspeaklanguage.org) or Ampleforth[^ample-forth], which offer completely open and customizable integrated programming environments. Glamorous Toolkit, in particular, champions the idea of using easily constructed custom tools to improve productivity and reduce time spent on code archeology, which is also a big inspiration for what we'll present here.

This paper contributes a description of the Clerk system, along with its background and the motivation for its construction. We include a number of examples of things built by users of the tool, and some discussion of the feedback that it has thus far received.

[^moldable-tools]: [Towards Moldable Development Tools](https://doi.org/10.1145/2846680.2846684) by Andrei Chiş, Oscar Nierstrasz and Tudor Gîrba

[^ample-forth]: [Ampleforth: A Live Literate Editor](https://blog.bracha.org/Ampleforth-Live22/out/primordialsoup.html?snapshot=Live22Submission.vfuel) by Gilad Bracha
[^Ungar87]: [Self: The power of simplicity](https://doi.org/10.1145/38807.38828)
## Programming with Clerk

> In such a future working relationship between human problem-solver and computer ‘clerk’, the capability of the computer for executing mathematical processes would be used whenever it was needed.[^latex-cit:Engelbart_1962]
>
> – Douglas Engelbart

[^latex-cit:Engelbart_1962]: See [Augmenting Human Intellect: A Conceptual Framework](https://www.dougengelbart.org/pubs/augment-3906.html) by Douglas Engelbart.

We have built Clerk on top of Clojure[^clojure], a functional-by-default Lisp dialect primarily hosted on the [Java Virtual Machine](https://en.wikipedia.org/w/index.php?title=Java_virtual_machine&oldid=1144897244). Several aspects of the language make it an appealing target for this project:

[^clojure]: For a description of the language and its motivations, see [A history of Clojure](https://doi.org/10.1145/3386321).

* being a Lisp, there is limited syntax with which to contend, and the language comes with good libraries for meta-linguistic programming
* an emphasis on pure functions and immutable data structures makes static analysis easier
* when mutable state is needed, there are idiomatic thread-safe boxes that are read and updated in a functional style

While there are some rough edges around a few particularly tricky language features, these aspects have mostly worked out in our favor.

### Basic Interaction: Bring-Your-Own-Editor

Clerk combines Lisp-style interactive programming with the benefits of computational notebooks, literate programming, and moldable development, all without asking programmers to abandon their favorite tools or give up their existing software engineering practices. Its design stems partially from the difficult lessons we learned after years of unsuccessfully trying to get our _own team_ to use an [online browser-based notebook platform][nextjournal] that we also developed.

When working with Clerk, a split-view is typically used with a code editor next to a browser showing Clerk’s representation of the same notebook, as seen in [_Clerk side-by-side with Emacs_](#clerk-side-by-side-with-emacs).

```clojure
(figure {:src "https://cdn.nextjournal.com/data/QmVYLx5SByNZi9hFnK2zx1K6Bz8FZqQ7wYtAwzYCxEhvfh?content-type=video/mp4"
         :poster-frame-src "https://cdn.nextjournal.com/data/QmSVeZpH3a5ag52AvodLDgBMA3AwnRrfawg5h2SQHk1rY3?filename=side-by-side.png&content-type=image/png"
         :id "clerk-side-by-side-with-emacs"
         :caption "Clerk side-by-side with Emacs"
         ::clerk/width :full
         :video? true})
```

As shown here, our _notebooks_ are just source files containing regular Clojure code. Block comments are treated as markdown text with added support for LaTeX, data visualization, and so on, while top-level forms are treated as code cells that show the result of their evaluation.[^maria] This format allows us to use Clerk in the context of production code that resides in revision control. Because files decorated with these comment blocks are legal code without Clerk loaded, they can be used in many contexts where traditional notebook-specific code cannot. This has led, among other things, to Clerk being used extensively to publish documentation for libraries that are then able to ship artifacts that have no dependency on Clerk itself.

[^maria]: We have borrowed this approach from [maria.cloud][maria], a web-hosted interactive Clojure learning tool created by [Matt Huebert](https://matt.is), [Dave Liepmann](https://www.daveliepmann.com/), and [Jack Rusher](https://jackrusher.com/). Maria grew out of [work presented at PX16](https://px16.matt.is) by Matt Huebert.

Clerk’s audience is experienced Clojure developers who are familiar with interactive development. They are able to continue programming in their accustomed style, evaluating individual forms and inspecting intermediate results, but with the added ability to `show!` a namespace/file in Clerk. A visual representation of the file is then re-computed either:

* every time the file is saved, using an an optional file watcher; or alternatively,
* via an editor hot-key that can be bound to show the current document. (The authors generally prefer the hot-key over the file watcher, as it feels more direct and gives more control over when to show something in Clerk.)

Control and configuration of Clerk primarily occurs through evaluation of Clojure forms from within the programmer's environment, rather than using outside control panels and settings. This integration with the programmer's existing tooling eases adoption and allows advanced customization of the system through code.

### Fast Feedback: Caching & Incremental Computation

To keep feedback loops short, Clerk uses dependency analysis to limit recomputation to forms that haven't previously been evaluated in Clerk.

In practice this means most changes to a Clerk document are reflected instantly (within 100ms) after saving a file or hitting the keybinding to update the open document.

The caching works on the level of top-level forms. A hash is computed for each top-level form. A change to the form or one of its transitive dependencies will lead to a new hash value. 


When Clerk is asked to show a notebook, it will only evaluate forms that aren't cached in one of Clerk's two caches:

* an in-memory cache stores a map of the hash of a given form to its current result. This cache is limited to the current forms of the active document.
* An on-disk-cache stores the same information but to allow the user to continue work after a restart without recomputing potentially expensive operations.[^data-ingestion] Because Clojure supports lazy evaluation of potentially infinite sequences, safeguards are in place to skip caching unreasonable values.

[^data-ingestion]: In tasks with intensive data preparation steps, this savings can be considerable.

This caching behavior can be fine-tuned (or disabled) down to the level of individual forms.

The on-disk caches use a content-addressed store where each result is stored using a filename derived from the SHA-2 hash of its contents. We use the self-describing [multihash format](https://multiformats.io/multihash/) which combines an identifier of the hash function with its digest length and value to support future changes of the hash algorithm. Additionally, a file named after the hash of a form contains a pointer to its results filename.

This combination of immutability and indirection makes distributing the cache trivial using last-write wins for the tiny (90 bytes) pointer files. The content-addressed result cache files are never changed and can thus be synchronized without conflict.

> While I did believe, and it has been true in practice, that the vast majority of an application could be functional, I also recognized that almost all programs would need some state. Even though the host interop would provide access to (plenty of) mutable state constructs, I didn’t want state management to be the province of interop; after all, a point of Clojure was to encourage people to stop doing mutable, stateful OO. In particular I wanted a state solution that was much simpler than the inherently complex locks and mutexes approaches of the hosts for concurrency-safe state. And I wanted something that took advantage of the fact that Clojure programmers would be programming primarily with efficiently persistent immutable data.[^history-of-clojure]
>
> – Rich Hickey

[^history-of-clojure]: [A History of Clojure](https://doi.org/10.1145/3386321), Rich Hickey

It is idiomatic in Clojure to use boxed containers to manage mutable state.[^clojure-state] While there are several of these constructs in the language, in practice [atoms](https://clojure.org/reference/atoms) are the most popular by far. An atom allows reading the current value inside it with [`deref/@`](https://clojuredocs.org/clojure.core/deref) and updating it's value with [`swap!`](https://clojuredocs.org/clojure.core/swap!).

[^clojure-state]: [Values and Change: Clojure’s approach to Identity and State](https://clojure.org/about/state)

When Clerk encounters an expression in which an atom's mutable value is being read using `deref`, it will try to compute a hash based on the value _inside_ the atom  at runtime, and extend the expression's static hash with it. 

This extension makes Clerk's caching work naturally with idiomatic use of mutable state, and frees programmers from the need to manually opt out of caching for those expressions.

### Semantic differences from regular Clojure

Clojure uses a single-pass, whole-file compilation strategy in which each evaluated form is added to the state of the running system. One positive aspect of this approach is that manually evaluating a series of forms produces the same result as loading a file containing the same forms in the same order, which is a useful property when interactively building up a program.

A practical concern with this sort of "bottom-up" programming is that the state of the system can diverge from the state of the source file, as forms that have been deleted from the source file may still be present in the running system. This can lead to a situation where newly written code depends on values that will not exist the next time the program runs, causing surprising errors. To help avoid this, Clerk defaults to signaling an error unless it can resolve all referenced definitions in the runtime to the source code.

It is our goal to match the semantics of Clojure as closely as possible but as a very dynamic language, there are limits to what Clerk's analysis can handle. Here are some of the things we currently do not support:

* Multiple definitions of the same var in a file
* Setting dynamic variables using [`set!`](https://clojuredocs.org/clojure.core/set!)
* Dynamically altering vars using [`alter-var-root`](https://clojuredocs.org/clojure.core/alter-var-root)
* Temporarily redefining vars using [`with-redefs`](https://clojuredocs.org/clojure.core/with-redefs)

We have included a mechanism to override Clerk's error checking in cases where the user knows that one or more of these techniques are in use.

### Presentation

Clerk uses a client/server architecture. The server runs in the JVM process that hosts the user's development environment. The client executes in a web browser running an embedded Clojure interpreter.[^sci]

[^sci]: [Small Clojure Interpreter](https://github.com/babashka/sci) by Michiel Borkent

The process of conveying a value to the client is a _presentation_, a
term taken from Common Lisp systems that support similar features.[^presentations] The process of presentation makes use of _viewers_, each of which is a hash map from well-known keys to quoted forms containing source code for Clojure functions that specify how the client should render data structures of a given type. When a viewer form is received on the client side, it is compiled into a function that will be then called on data later sent by the server.

[^presentations]: This feature originated on the Lisp Machine, and lives on in a reduced form as a feature of the emacs package [Slime](https://slime.common-lisp.dev/doc/html/Presentations.html).

When the `present` function is called on the server side, it defaults to performing a depth-first traversal of the data structure it receives, attaching appropriate viewers at each node of the tree. The resulting structure containing both data and viewers is then sent to the client.

To avoid overloading the browser or producing uselessly large output, Clerk’s built-in collection viewer carries an attribute to control the number of items initially displayed, allowing more data to be requested by the user on demand. Besides this simple limit, there’s a second global _budget_ per result to limit the total number of items shown in deeply nested data structures. We’ve found this simple system to work fairly well in practice.

One benefit of using the browser for Clerk's rendering layer is that it can produce static HTML pages for publication to the web. We could not resist the temptation to produce this document with Clerk.[^latex-skip:sidenotes]

[^latex-skip:sidenotes]: We also used this essay as an opportunity to improve Clerk's support for sidenotes like this one.

It's also possible to use Clerk's presentation system in other contexts. We know of at least one case of a user leveraging Clerk's presentation system to do in-process rendering without a browser.[^desk]

[^desk]: See [Desk](https://github.com/phronmophobic/desk), by Adrian Smith.

### Built-in Viewers

Clerk comes with a set of built-in viewers for common situations. These include support for Clojure’s immutable data structures, HTML (including the [hiccup variant](https://github.com/weavejester/hiccup) that is often used in Clojure to represent HTML and SVG), data visualization, tables, LaTeX, source code, images, and grids, as well as a fallback viewer based on Clojure’s printer. The [Book of Clerk][book-of-clerk] gives a good overview of the available built-ins. Because Clerk’s client is running in the browser, we are able to benefit from the vast JS library ecosystem. For example we're using [Plotly](https://plotly.com/javascript/) and [vega](https://github.com/vega/vega-embed) for graphing, [CodeMirror](https://codemirror.net) for rendering code cells, and [KaTeX](https://katex.org) for typesetting mathematics.

Clerk’s built-in viewers try to suit themselves to typical Data Science use cases. By default, Clerk shows a code block’s result as-is with some added affordances like syntax coloring and expandability of large sub-structures that are collapsed by default. 

Here is an interactive example of the well-known `iris` data set, which we've added as a dependency to this notebook. Clicking the disclosure triangles will expand the data structure:

```clojure
^{::clerk/visibility {:code :show}}
datasets/iris
```

Additional affordances are automatic expansion of a nested data structure based on its shape and expanding multiple sub-structures on the same level, as demonstrated in this video:

```clojure
(figure {:src "https://cdn.nextjournal.com/data/QmciJrXQguekgeX6LsXUmvNthadkN2Eu4RMpMXzbKN6JDg?content-type=video/mp4"
         :poster-frame-src "https://cdn.nextjournal.com/data/QmcABdFREVjdAcruW3Qb45PLAwD7E337BWgU2gvU2gXw1B?filename=multi-expand.png&content-type=image/png"
         :video? true
         :id "expanding-multiple-sub-structures-at-once"
         :caption "Expanding multiple sub-structures at once"
         ::clerk/width :wide})
```

Using the built-in `clerk/table` viewer, the same data structure can also be rendered as a table. The table viewer uses heuristics to infer the makeup of the table, such as column headers, from the structure of the data:

``` clojure
^{::clerk/visibility {:code :show}}
(clerk/table datasets/iris)
```

Together with tables, plots are the most commonly used viewer for Data Science use cases. In the following figure, the same `iris` dataset, as shown in the above table example, is used to render an interactive [Vega-Lite](https://vega.github.io/vega-lite/) plot using the `clerk/vl` viewer:

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

Then, a `sparkline` function is defined and tested to generate graphs (using `clerk/vl`) that will be embedded into each table row later:

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

^{::clerk/visibility {:code :show} ::clerk/latex-graphics-opts "width=0.15\\textwidth"}
(sparkline (shuffle (range 30)))
```

Finally, the data is reduced to quarters and years, adding the sparkline graphs in a final step:

```clojure
^{::clerk/visibility {:code :show}}
(clerk/table
 {:head ["Year" "Q1" "Q2" "Q3" "Q4" "Trend"]
  :rows (->> datasets/air-passengers
             (group-by :year)
             (map (fn [[year months]]
                    (let [qs (->> months (map :n) (partition 3) (map #(reduce + %)))]
                      (concat [year] 
                              qs 
                              [(sparkline (map :n months))]))))
             (sort-by first))})
```

This sort of gradual refinement is a hallmark of exploratory programming, and is especially enjoyable with access to the full power of a general purpose programming language.

### Moldable Viewer API

Clerk’s viewers are an ordered (and thus prioritized) collection of plain Clojure hash maps. Clerk interprets the following optional keys in each viewer map:

* `:pred` is a predicate function that tests whether this viewer should be used for a given data structure
* `:transform-fn` is an optional function run on the server side to transform data before sending it to the client. It receives a map argument with the original value under a key. Additional keys carry the path, the viewer stack, and the budget (for elision)
* `:render-fn` is a quoted form that will be sent to the browser, where it will be turned into a function that will be called to display the data
* `:page-size` is a number that indicates how many items to send in each chunk during elision/pagination

Here, for example, is the `code` viewer, which shows a piece of Clojure code with idiomatic syntax highlighting:[^self-rendered]

[^self-rendered]: The source code here is rendered by the viewer that it describes.

``` clojure
(clerk/code
'{:name :code 
  :render-fn 'nextjournal.clerk.render/render-code 
  :transform-fn (comp mark-presented (update-val (fn [v] (if (string? v) v (str/trim (with-out-str (pprint/pprint v)))))))})
```


Viewers can also be explicitly selected by wrapping a value in the `clerk/with-viewer` function, which produces a presentation for that value using that viewer. Alternatively, viewers can be selected by placing a Clojure metadata declaration before a form. Because of the way Clojure handles compilation, metadata in this position is ultimately ignored in the generated code. So far as we know, this is a novel mechanism for out-of-band signaling to a specialized Clojure parser.

The process of selecting viewers happens programmatically on the server side, thus using the programmer's already existing interactive programming environment as a user interface.

### Sync

To help with creating interactive tools using Clerk, it also supports bidirectional sync of state between the client and server Clojure environments. If a Clojure `atom` on the server is annotated with metadata indicating it is `sync`, Clerk will create a corresponding var in the client environment. Both of these atoms will be automatically instrumented with an update watcher that broadcasts a _diff_ to the other side.

In addition, a server-side change will trigger a refresh of the currently active document, which will then re-calculate the minimum subset of the document that is dependent on that atom's value. This allows us to use Clerk for small local-first apps, as shown in [the Regex Dictionary Example](#interactive-regex-dictionary).

### Tap Stream Inspector

Clerk also comes with an inspector for Clojure's tap system.

> **tap** is a shared, globally accessible system for distributing a series of informational or diagnostic values to a set of (presumably effectful) handler functions. It can be used as a better debug prn, or for facilities like logging etc.
> 
> – [Clojure 1.10 Changelog](https://github.com/clojure/clojure/blob/0b42eab4bfca5270e0d2b2e58d83b1e2c8a85473/changes.md#23-tap)

When enabled, Clerk will attach a tap listener function and record and show the tap stream. This makes Clerk's viewer system accessible across file and namespace boundaries and independently of the caching mechanisms.

### Embedding Examples

The [comment](https://clojuredocs.org/clojure.core/comment) macro in Clojure is typically used to annotate source code with rich examples that exercise the program during development and aid comprehension.

Clerk's example macro expands on that by showing the source code next to the evaluation result.

```clojure
^{::clerk/visibility {:code :show}}
(clerk/example
  (+ 40 2)
  (-> 42 range shuffle)
  (clerk/code (macroexpand '(clerk/example (inc 41))))
  (clerk/html [:h1 "👋"]))
```


### Prose-oriented Documents

The first and primary use case for Clerk was adding prose, visualizations, and interactivity to Clojure namespaces. However, when writing documents that are mainly prose, but would benefit from _some_ computational elements, it is rather tedious to write everything in comment blocks. To make this easier, Clerk can also operate on markdown files with “code-fenced” source code blocks. All Clojure source blocks in such a file are evaluated and replaced in the generated document with their result.

This format is very similar to other markdown-based notebooks, like [R Markdown](https://rmarkdown.rstudio.com), but specifically tailored to Clojure. We used this approach to write this paper, the source for which is located [on Github](https://github.com/mk/clerk-px23).[^github-format]

[^github-format]: One nice thing about this approach is that other systems, like Github, are able to render a reasonable version of the document, though without evaluation.

During the review process for this paper, one of the reviewers mentioned that they would have preferred a PDF document to a website. We took this is an opportunity to test the flexibility of our system, adding a LaTeX translation layer to produce a separate printable version. It sadly lacks the interactive features of the web presentation, but we are quite pleased with the quality of the document we were able to send to press with relatively little additional work.

## Examples of Moldable Development with Clerk

In addition to the sorts of traditional data science use cases that one might expect from something that has "notebook" features, we intend Clerk to be a general purpose programmer's assistant[^programmers-assistant] that allows the rapid construction of tiny interfaces during daily work. Here are a few samples of tools and documentation created in this manner.

[^programmers-assistant]: We use this term in appreciation of pioneering historical work by Warren Teitelman.

### Augmenting table names

This example illustrates an approach we used to make working with a legacy DB2 database easier. The database’s column names are made up of largely human-unreadable 8 character sequences:

```clojure
(figure {:id "as400-column-names"
         :src "https://cdn.nextjournal.com/data/QmWnzjc5c9qpUUaLoK3ytZk4Zs1AzDpZj1Tx5FF4ZR8a5t?filename=AS400-Cut.png&content-type=image/png"
         :caption "AS/400 Column Names"
         ::clerk/width :wide})
```

We were able to automatically translate these names using a metaschema extracted from the database. This allowed us to create a viewer that maps those eight-character names to human-readable (German-only) names (which we can then translate to English). In typical Lisp fashion, we go on to inspect a query interactively. We can use the translated names in the table, and even print them, but one quickly sees the limit of plain-text printing:

```clojure
(figure {:src "https://cdn.nextjournal.com/data/QmbGFKpEXLGyqngHe7q1dqAsEAWfotSHG8XxYZPQfHirQ1?content-type=video/mp4"
         :poster-frame-src "https://cdn.nextjournal.com/data/QmagkcNvHsscf3DEq18nuXi6z3u4CyQWGzaRLf1hUm5a32?filename=repl-inspecting-a-query.png&content-type=image/png"
         :video? true
         :id "inspecting-a-query-using-the-repl"
         :caption "Inspecting A Query Using the REPL"
         ::clerk/width :wide})
```

With Clerk, were able to render the output as a graphical table without the limitations of plain text. Further, we can use the Viewer API to extend the table viewer’s headings to show the translated metaschema names (plus showing the original eight character names in a de-emphasized way so that they aren’t lost). We can go further still, showing the original German names when move the mouse over the headings:

```clojure
(figure {:src "https://cdn.nextjournal.com/data/QmVZsXxsX2wcYYc758yHkZjijW2HdZhaGcfQaHpAkZeqWk?content-type=video/mp4"
         :poster-frame-src "https://cdn.nextjournal.com/data/QmVdGBFtcK7AAksAzmCH4t18f1RvCbepLZPixaq4ncz4cq?filename=augmented-table-headers.png&content-type=image/png"
         :id "augmented-table-headings"
         :caption "Augmented Table Headings"
         :video? true
         ::clerk/width :wide})
```

### Rich documentation features

This example illustrates the use of Clerk to create rich documentation for `clojure2d`’s colors package.[^color-package] They used Clerk’s Viewer API to implement custom viewers to visualize colors, gradients and color spaces, then publish that documentation on the web by generating a static website directly from the source code of the library.

[^color-package]: The full documentation is
[here](https://clojure2d.github.io/clojure2d/docs/notebooks/notebooks/color.html).

```clojure
(figure {::clerk/width :wide
         :id "custom-viewers-for-clojure2ds-colors-library"
         :poster-frame-src "https://cdn.nextjournal.com/data/QmQZ5kZwRYEEXUfNfWPsaidpoN88ugdSXYqEUVT5q1FyAE?filename=colors.png&content-type=image/png"
         :src "https://cdn.nextjournal.com/data/QmQgTLi8qfzrBRTkaAGfWQ4RceM4v3fp4Wna7knivMgusb?filename=clojure2d-color.png&content-type=image/png"
         :caption "Custom Viewers for Clojure2d’s Colors Library"})
```

### Regex Dictionary

Built as a showcase for Clerk’s sync feature, this example allows entering a regex into a text input and get dictionary matches as result while you type:

```clojure
(figure {:src "https://cdn.nextjournal.com/data/QmTwZWw4FQT6snxT8RkKt5P7Vxdt2BjM6ofbjKYEcvAZiq?content-type=video/mp4"
         :poster-frame-src "https://cdn.nextjournal.com/data/QmWo1npCDCK5F1t7Aofc65pbbeMfoQNMVje4F6UMTBK4CK?filename=regex-dict.png&content-type=image/png"
         :id "interactive-regex-dictionary"
         :caption "Interactive Regex Dictionary"
         :video? true
         ::clerk/width :wide})
```

It is built using a Clojure atom containing the text input’s current value that is synced between the client and server. As you type into the input, the atom’s content will be updated and synced. Consequently, printing the atom’s content in your editor will show the input’s current value:

```clojure
(figure {::clerk/width :wide
         :id "printing-the-value-of-a-synced-clojure-atom"
         :caption "Printing the value of a synced Clojure atom"
         :poster-frame-src "https://cdn.nextjournal.com/data/QmRvfwCT5wJCbU8ALLghhD7v9chKob8MXcUV3vAf6XTbm8?filename=regex-dict-output.png&content-type=image/png"
         :src "https://cdn.nextjournal.com/data/QmNS2jigrDn2WdS7AVa4qMiWtwZovJmfzYbWczwg1Ptaqk?filename=Regex+Value+Cut.png&content-type=image/png"})
```

### [Lurk](https://github.com/nextjournal/lurk): Interactive Lucene-powered Log Search

Also building on Clerk’s sync feature, this interactive log search uses [Lucene](https://lucene.apache.org/) on the JVM side to index and search a large number of log entries. In addition to using query input, logs can also be filtered by timeframe via an interactive chart. It is worth noting that this example uses a full-screen layout by opting out of Clerk's default notebook styling via Clerk’s CSS customization options.

```clojure
(figure {:src "https://cdn.nextjournal.com/data/QmRtGb5aByKD6i5SsxfS1JCJPKpC1kW5wbGvmT1h6awyB9?content-type=video/mp4"
         :poster-frame-src "https://cdn.nextjournal.com/data/QmcebVp6BGydcUUJhkjMurj5bBAdnDwbeiMjrmD6avKji4?filename=lurk.png&content-type=image/png"
         :id "interactive-log-search"
         :caption "Interactive Log Search"
         ::clerk/width :wide})
```

### Experience

Our experience as the developers and users of Clerk has been surprisingly positive, and it has seen solid adoption within our organization, including many of us also using it for side-projects.

We believe this is in large part due to the purely additive design of Clerk. When one chooses to view a namespace through Clerk, it evokes feelings of added power, without the loss of control that comes from leaving one's comforting and familiar editing environment (Emacs, Vim, VS Code). In hindsight, we now realize that we underestimated what a big ask that is for potential users of these sorts of systems.

We've chosen a few quotes from Clerk's user base to give a sense of how the Clojure community has experienced Clerk. So far, even with thousands of users, we've had mostly positive feedback, aside from the usual bug reports every project receives.

> [Clerk] is making the training of junior Clojure programmers a massive pleasure! [...]
> 
> It helps us to bypass what would otherwise be a lot of distracting UI programming. Set up your env, make a namespace, hit a keybind, hey presto, your code is running in a browser.
> 
> – Robert Stuttaford

> I'm using Clerk to visualize statistics properties from a simulation in a model checker [...] it's basically a wrapper over TLA+ [...]
>
> Amazing that Clerk just lets you focus on what really matters and nothing else!
>
> – Paulo Feodrippe

> I just wanted to express some gratitude for Clerk. It’s been a game changer for me in terms of understanding problems and communicating that understanding to other people. 
>
> – Jeffrey Simon

One often under-appreciated area in the academic discourse on programming is aesthetics. We believe the baseline level of design that is automatically inherited by any document produced with Clerk has contributed to the positive experiences our users have reported.

> Attractive things do work better—their attractiveness produces positive emotions, causing mental processes to be more creative, more tolerant of minor difficulties.
> 
> – Don Norman, Emotional Design

## Related Work

Besides the systems mentioned earlier in the paper, there are a number of other contemporary related systems. Many of them introduce the viewing layer to the editor itself, as seen in various Smalltalk experiments (Moldable Development[^moldable-tools],  Newspeak[^newspeak],  Babylonian Programming[^babylonian], etc), and in recent work on LiveLits[^filling-typed-holes] and visual syntactic extensions to the Racket programming language.[^interactive-visual-syntax]

In contrast, because we have found it more persuasive to argue for the use of such systems in the context of a user's existing tooling, we've focused on an approach that supplements existing workflows with a sidecar previewer. Other systems that have taken a similar approach include:

* [Org mode][org-mode] is a major mode for [Emacs](https://www.gnu.org/software/emacs/) supporting polyglot literate programming based on a plain text format. Org's approach is in many ways similar to our Markdown mode, where explicit code fences are used to introduce programmatic constructs to a document, and a variety of previewers are available for org files, though in a fragmented ecosystem where each kind of application requires a different kind of previewer. We were partially inspired by positive experiences using org-mode in the design of Clerk, though we focused more on our annotated source code format.

* [Streamlit][streamlit] is a Python library that eshews a custom format and enables building a web UI on regular python scripts. Its [caching system][streamlit-cache] memoizes functions that are tagged using Python's decorators. While there are several similarities between Clerk and Steamlits, the two systems have a very different focus. Streamlit is primarily a rapid application development environment, without Clerk's focus is on combining Literate Programming and Moldable Development.

We are pleased to see a number of other systems working with annotated source code, including [Pluto][pluto] and [Livebook][livebook], and consider this convergent evolution toward tools that developers are more likely to actually use as a positive trend.

## Future Work

Our goal with the development of Clerk is to _leave the toolbox open_: we want Clerk's users to be able to customize the behavior of the running system in a predictable way, often by providing functions to the system at runtime.

Clerk's viewer API is a first example of this approach, but we want to take it further by letting users:

* provide functions to control the caching e.g. to support more efficient caching of data frames
* letting the viewer API's `:pred` function opt into receiving more context like the path in the tree
* make caching more granular and support caching function invocations
* override `parse` and `eval` to support additional syntaxes with different semantics

So far we've mainly used Clerk's caching on local machines in isolation. We plan to share a distributed cache within our dev team in order to learn about the benefits and challenges this can bring. We also want to extend Clerk to better communicate caching behavior to its users (why a value could or could not be cached, if it was cached in-memory or on-disk).

We're also actively exploring different ways of bringing an exploratory Clerk notebook to production. In exploratory work one often uses global state in Clojure atoms and vars which makes a computation tangible. When serving concurrent requests in a production setting that global state can be a source of inconsistencies.

We've been discussing ways to write changes originating from controls in Clerk's view back to source files. We also believe that for this to be a good developer experience, concurrent modifications without intermediate saving should be supported, making a simple integration that works by overwriting source files insufficient. Because this is a significant chunk of work, and will require a different solution for each editor, we've avoided it until now. Since there certainly are many tasks for which direct manipulation can be more effective than editing text in a code editor, we're excited to explore this direction in the future.

## Conclusion 

We've been pleasently surprised with how useful Clerk has been in our day-to-day work, and the adoption it has received within the larger Clojure community suggests we are not the only ones to feel this way. We believe there are two key factors in Clerk's design that enable this: 

* Enhancing regular Clojure namespaces enables incremental adoption. Neither does it need initial buy-in from the whole dev team, nor does a single member need to fully switch to working with Clerk.
* Working in concert with regular editors means it meets developers where they are and does not require a radical change of workflows.

We'd love to see folks apply these design choices to other programming ecosystems and join the pursuit to free programming from the limitations of dead text. 

[book-of-clerk]:https://book.clerk.vision
[nextjournal]:https://nextjournal.com
[maria]:https://maria.cloud
[streamlit]:https://streamlit.io
[streamlit-cache]:https://docs.streamlit.io/library/get-started/main-concepts#caching
[pluto]:https://plutojl.org
[livebook]:https://livebook.dev
[org-mode]:https://orgmode.org

[^newspeak]: [Enhancing Liveness with Exemplars in the Newspeak IDE](https://newspeaklanguage.org/pubs/newspeak-exemplars.pdf) by Gilad Bracha.

[^babylonian]: [Babylonian-style Programming: Design and Implementation of an Integration of Live Examples into General-purpose Source Code](https://doi.org/10.22152/programming-journal.org/2019/3/9) by David Rauch, Patrick Rein, Stefan Ramson, Jens Lincke, Robert Hirschfeld.

[^interactive-visual-syntax]: [Adding interactive visual syntax to textual code](https://doi.org/10.1145/3428290) by Andersen, Leif and Ballantyne, Michael and Felleisen, Matthias

[^filling-typed-holes]: [Filling Typed Holes with Live GUIs](https://doi.org/10.1145/3453483.3454059) by Cyrus Omar, David Moon, Andrew Blinn, Ian Voysey, Nick Collins, and Ravi Chugh.
