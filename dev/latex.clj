(ns latex
  {:nextjournal.clerk/visibility {:code :hide :result :hide}}
  (:require [babashka.fs :as fs]
            [clojure.data.json :as json]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.zip :as z]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.eval :as clerk.eval]
            [nextjournal.clerk.viewer :as v]
            [nextjournal.markdown :as md]
            [nextjournal.markdown.parser :as md.parser]
            [nextjournal.markdown.transform :as md.transform])
  (:import (java.net.http HttpClient HttpClient$Redirect HttpRequest HttpResponse$BodyHandlers)
           (javax.imageio ImageIO)
           (java.net URL URI)))

;; # $\LaTeX$ Conversion
;; We're using [Pandoc](pandoc.org) and [Tectonic]() following [submission guidelines](https://2023.programming-conference.org/home/px-2023#submissions).
;;
;; Instead of transforming markdown to latex directly, we're going over Clerk's evaluated document, so we have a chance to get hold of visibility and generated results.
;;
;; ## Prerequisites
;; - pandoc 3.x (`brew install pandoc`)
;; - tectonic (`brew install tectonic`)
;; - [ACM LaTeX Package](https://www.acm.org/publications/proceedings-template) (relevant .cls files added to repo)
;;
;; ## Pandoc Template Tweaks
;; The following changes were needed for `sample-sigconf.tex` to be used as a Pandoc template
;; - well, `$body` somewhere
;; - \tightlist command
;; - Insert `$title$`, `$abstract$, `$authors$` variable placeholders
;;
;; ## Todos
;; - [x]  Title
;; - [ ] ~Use `teaserfigure` for figures spanning the whole width~
;; - [x] Authors (_Note that authors' addresses are mandatory for journal articles._, aggregate affiliation (?)
;; - [x] Bibliography (Bibtex vs. ~~Biblatex~~) via DOI-links to bib entry conversions (e.g https://www.doi2bib.org/)
;; - [x] Decide which template to use (e.g. `sample-sigconf`)
;; - [x] Adapt Heading (Sections) Hierarchy
;; - [x] Results to Images or Pdf (?) (https://reachtim.com/include-html-in-latex.html)
;; - [ ] Disable table sticky headers when taking result snapshots
;; - [x] Improve code listings (or ensure they fit into column)
;; - [x] Footnotes

(def ^:dynamic *footnotes* nil)
(def ^:dynamic *bib-entries* nil)

(declare md->pandoc)

(def doi->bib
  (memoize (fn [doi]
             (.body (.send (.. (HttpClient/newBuilder) (followRedirects HttpClient$Redirect/ALWAYS) build)
                           (.. (HttpRequest/newBuilder)
                               (header "accept" "application/x-bibtex")
                               (uri (URI. doi))
                               build) (HttpResponse$BodyHandlers/ofString))))))

#_(doi->bib "https://doi.org/10.1145/2846680.2846684")

(defn bib-entry->key [bib] (second (re-find #"\{([^,]+)," bib)))
(defn reset-bib-entries! [] (spit (fs/file "bibliography.bib")
                                  (str (first (str/split (slurp "bibliography.bib") #"%Entries"))
                                       "%Entries\n")))
#_(reset-bib-entries!)
(defn append-bib-entry! [entry] (fs/write-lines "bibliography.bib" ["" entry] {:append true}))

(defn find-doi [node]
  (loop [z (md.parser/->zip node)]
    (let [{:keys [attrs type]} (z/node z)]
      (cond
        (z/end? z) nil
        (= :link type)
        (if (str/starts-with? (:href attrs) "https://doi.org")
          (:href attrs)
          (recur (z/next z)))
        :else (recur (z/next z))))))

(def md->pandoc-transform
  {:doc (fn [{:as doc :keys [content footnotes !bib-entries]}]
          (binding [*footnotes* footnotes
                    *bib-entries* !bib-entries]
            {:blocks (into [] (keep md->pandoc) content)
             :pandoc-api-version [1 23]
             :meta {}}))

   :heading (fn [{:as node :keys [content heading-level]}]
              {:t "Header"
               :c [heading-level
                   [(:id (md.parser/text->id+emoji (md.transform/->text node))) [] []]
                   (into [] (keep md->pandoc) content)]})

   :paragraph (fn [{:keys [content]}] {:t "Para" :c (into [] (keep md->pandoc) content)})
   :plain (fn [{:keys [content]}] {:t "Plain" :c (into [] (keep md->pandoc) content)})
   :blockquote (fn [{:keys [content]}] {:t "BlockQuote" :c (into [] (keep md->pandoc) content)})
   :code (fn [{:as node :keys [language]}] {:t "CodeBlock" :c [["" [language "code"] []] (md.transform/->text node)]})
   :block-formula (fn [{:keys [text]}] {:t "Para" :c [{:t "Math" :c [{:t "DisplayMath"} text]}]})
   :formula (fn [{:keys [text]}] {:t "Math" :c [{:t "InlineMath"} text]})
   :ruler (fn [_] {:t "HorizontalRule"})
   :raw-inline (fn [{:keys [kind text]}] {:t "RawInline", :c [kind text]})
   :figure (fn [{:as node :keys [content attrs]}]
             {:t "Figure"
              :c [["" [] []]
                  [nil [{:t "Plain"
                         :c (into [] (keep md->pandoc) content)}]]
                  [{:t "Plain"
                    :c [{:t "Image"
                         :c [["" [] []]
                             (into [] (keep md->pandoc) content)
                             [(:src attrs) (md.transform/->text node)]]}]}]]})
   :image (fn [{:as node :keys [content attrs]}]
            (let [{:keys [src]} attrs
                  caption (md.transform/->text node)]
              {:t "Image",
               :c [["" [] [["width" "linewidth"]]]
                   (into [] (keep md->pandoc) content)
                   ;; a fig: will wrap the resulting \includegraphics in a figure environment
                   [src (str (when (not-empty caption) "fig:"))]]}))
   :softbreak (fn [_] {:t "SoftBreak"})
   :em (fn [{:keys [content]}] {:t "Emph" :c (into [] (keep md->pandoc) content)})
   :strong (fn [{:keys [content]}] {:t "Strong" :c (into [] (keep md->pandoc) content)})
   :strikethrough (fn [{:keys [content]}] {:t "Strikeout" :c (into [] (keep md->pandoc) content)})
   :link (fn [{:keys [attrs content]}] {:t "Link" :c [["" [] []] (into [] (keep md->pandoc) content) [(:href attrs) ""]]})

   :monospace (fn [node] {:t "Code" :c [["" [] []] (md.transform/->text node)]})

   :list-item (fn [{:keys [content]}] (into [] (keep md->pandoc) content))
   :bullet-list (fn [{:keys [content]}] {:t "BulletList" :c (into [] (keep md->pandoc) content)})

   :text (fn [{:keys [text]}] {:t "Str" :c text})

   ;; TODO: https://pandoc.org/MANUAL.html#footnotes
   :footnote-ref (fn [{:keys [ref]}]
                   (let [{:as footnote :keys [content]} (get *footnotes* ref)]
                     (when-not footnote (throw (ex-info (str "Can't find footnote #" ref) {:ref ref :footnotes *footnotes*})))
                     (if-some [doi (find-doi footnote)]
                       (let [bib-entry (doi->bib doi)
                             bib-key (bib-entry->key bib-entry)]
                         (swap! *bib-entries* assoc bib-key bib-entry)
                         {:t "RawInline", :c ["tex" (str " \\cite{" bib-key "}")]})
                       {:t "Note" :c (into [] (keep md->pandoc) content)})))})

(defn md->pandoc
  [{:as node :keys [type]}]
  (if-some [xf (get md->pandoc-transform type)]
    (try (xf node)
         (catch Exception e (throw (ex-info (str "Cannot convert node: " (pr-str node)) node e))))
    (throw (ex-info (str "Not implemented: '" type "'.") node))))

(def pandoc-exec
  "pandoc"
  #_"/usr/local/Cellar/pandoc/3.1/bin/pandoc"
  #_"/opt/old-versions/pandoc")

(defn assert-pandoc-3! []
  (let [version (:out (sh pandoc-exec "--version"))]
    (when-not (re-find #"^pandoc 3\.\d" version)
      (throw (ex-info (str "LaTeX Conversion needs pandoc 3.x, found: " version)
                      {:version version})))))
#_(assert-pandoc-3!)

(defn pandoc-> [pandoc-data format & {:keys [template] :or {template "template-sigconf.tex"}}]
  (assert-pandoc-3!)
  (let [{:keys [exit out err]}
        (apply sh (filter some?
                          [pandoc-exec "--from" "json" "--to" format
                           (when template (str "--template=" template))
                           "--pdf-engine=tectonic"
                           ;;"--no-highlight"
                           (when (= "pdf" format) "--output=README.pdf")
                            "--listings" ;; code via listings package (https://ctan.org/pkg/listings)
                           :in (json/write-str pandoc-data)]))]
    (if (zero? exit) out err)))

(defn pandoc<- [input format]
  (assert-pandoc-3!)
  (-> (sh pandoc-exec "-f" format "-t" "json" :in input)
      :out (json/read-str :key-fn keyword)))

(defn meta-list [content] {:t "MetaList" :c content})
(defn meta-content [content] {:t "MetaInlines" :c [{:t "Str" :c content}]})

(defn add-authors [pandoc & authors]
  (assoc-in pandoc
            [:meta :author]
            (meta-list
             (map (fn [author]
                    {:t "MetaMap"
                     :c (into {} (map (fn [[k v]] [k (meta-content v)])) author)}) authors))))

#_(add-authors {} {:name "X" :affiliation "Penguin Village University"})

(defn promote-headings [doc]
  (loop [z (md.parser/->zip doc)]
    (if (z/end? z)
      (z/root z)
      (recur (z/next (cond-> z
                       (= :heading (:type (z/node z)))
                       (z/edit update :heading-level dec)))))))

(defn get-abstract [{:keys [blocks]}]
  (-> blocks (nth 4)
      :result v/->value v/->value
      z/vector-zip
      (->> (iterate z/next) (take 8))
      last z/node))

(defn store-image-src! [{:keys [id src poster-frame-src]}]
  ;; CDN url responds with 451 (Unavailable for legal reasons)
  (assert id)
  (let [url (URL. (str/replace (or poster-frame-src src) "cdn." ""))
        path (fs/path "images" (str id ".png"))]
    (fs/create-dirs "images")
    (when-not (fs/exists? path)
      (ImageIO/write (ImageIO/read url) "png" (fs/file path)))
    (str path)))

(defn convert-result [{:as block :keys [id result]}]
  (let [{:as opts :keys [src poster-frame-src caption]} (v/->value result)
        result-screenshot-path (str "images/" (name id) "-result.png")]
    (cond
      src
      {:type :figure
       :attrs {:src (store-image-src! opts)}
       :content (let [caption-text [{:type :text :text (str caption)}]]
                  (if poster-frame-src
                    [{:type :link
                      :attrs {:href src}
                      :content caption-text}]
                    caption-text))}
      (fs/exists? result-screenshot-path)
      {:type :paragraph
       :content [{:type :image
                  :attrs {:src result-screenshot-path}}]})))

(defn conj-some [xs x] (cond-> xs x (conj x)))

(defn add-interactivity-notice [doc]
  (-> doc md.parser/->zip z/down
      (z/insert-left {:type :blockquote
                      :content [{:type :plain
                                 :content [{:type :em
                                            :content [{:type :text :text "If you can, we’d prefer you read the interactive website version of this essay at "}
                                                      {:type :link :attrs {:href "https://px23.clerk.vision"}
                                                       :content [{:type :text :text "px23.clerk.vision"}]}]}]}]})
      z/root))

(defn clerk->pandoc [file]
  (reset-bib-entries!)
  (let [{:as clerk-doc :keys [title footnotes blocks]} (clerk.eval/eval-file file)
        !bib-entries (atom {})
        result (-> {:type :doc
                    :title title
                    :footnotes footnotes
                    :!bib-entries !bib-entries
                    :content (mapcat (fn [{:as block :keys [type doc visibility text-without-meta]}]
                                       (let [{code-visibility :code result-visibility :result} visibility]
                                         (case type
                                           :markdown (:content doc)
                                           :code (cond-> []
                                                   (= :show code-visibility)
                                                   (conj {:type :plain
                                                          :content [{:type :raw-inline
                                                                     :kind "tex"
                                                                     :text (str "\\begin{minipage}{\\linewidth}\n\\begin{lstlisting}\n"
                                                                                text-without-meta
                                                                                "\n\\end{lstlisting}\n\\end{minipage}")}]})
                                                   (= :show result-visibility)
                                                   (conj-some (convert-result block))))))
                                     ;; drop custom abstract and helpers
                                     (drop 5 blocks))}
                   promote-headings
                   add-interactivity-notice
                   md->pandoc
                   (assoc-in [:meta :title] (meta-content title))
                   (assoc-in [:meta :abstract] (meta-content (get-abstract clerk-doc)))
                   (assoc-in [:meta :keyword] (meta-list (map meta-content ["Literate Programming," "Moldable Development"])))
                   (add-authors {:name "Martin Kavalar"
                                 :email "martin@nextjournal.com"}
                                {:name "Philippa Markovics"
                                 :email "philippa@nextjournal.com"}
                                {:name "Jack Rusher"
                                 :email "jack@nextjournal.com"}))]

    (doseq [[_ entry] @!bib-entries] (append-bib-entry! entry))
    result))

(defn clerk->latex! [{:keys [file] :or {file "README.md"}}]
  (let [out-file (str (first (fs/split-ext file)) ".tex")]
    (-> (clerk->pandoc file)
        (pandoc-> "latex")
        (->> (spit out-file)))))

#_(clerk->latex! {})

;; # Usage:
;; 1. Take screenshots of results (only when results change, snapshots are tracked in git under `/images`. Notebook needs to be shown at localhost:7676)
;; 2. run `(clerk->latex! {})
;; 3. run `(sh "tectonic" "--keep-intermediates" "README.tex")` to produce a pdf
;;
;; See comments below:
(comment

  ;; to latex
  (-> (clerk->pandoc "README.md")
      (pandoc-> "latex") #_(subs 5000 8000)
      (->> (spit "README.tex")))

  ;; to pdf
  (do
    (sh "tectonic" "--keep-intermediates" "README.tex")
    (sh "open" "README.pdf"))

  ;; capture screenshots
  (sh "yarn" "nbb" "-m" "screenshots" "--url" "http://localhost:7676" "--out-dir" "../../clerk-px23/images"
      :dir "../clerk/ui_tests")

  ;; pandoc version
  (sh pandoc-exec "--version")

  ;; get Pandoc AST for testing
  (-> "should cite \\cite{thorsten93} ok"
      #_md/parse #_md->pandoc
      (pandoc<- "markdown+footnotes+implicit_figures+raw_tex")
      #_(pandoc-> "latex" :template nil)))
