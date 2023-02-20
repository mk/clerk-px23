(ns latex
  {:nextjournal.clerk/visibility {:code :hide :result :hide} }
  (:require [clojure.data.json :as json]
            [clojure.java.shell :refer [sh]]
            [clojure.zip :as z]
            [nextjournal.clerk :as clerk]
            [nextjournal.clerk.eval :as clerk.eval]
            [nextjournal.markdown :as md]
            [nextjournal.markdown.parser :as md.parser]
            [nextjournal.markdown.transform :as md.transform]))

;; # $\LaTeX$ Conversion
;; We're using [Pandoc](pandoc.org) and [Tectonic]() following [submission guidelines](https://2023.programming-conference.org/home/px-2023#submissions).
;;
;; Instead of transforming markdown to latex directly, we're going over Clerk's evaluated document, so we have a chance to get hold of visibility and generated results.
;;
;; ## Prerequisites
;; - pandoc (`brew install pandoc`)
;; - tectonic (`brew install tectonic`)
;; - [ACM LaTeX Package](https://www.acm.org/publications/proceedings-template) (relevant .cls files added to repo)
;;
;; ## Pandoc Template Tweaks
;; The following changes are needed for `sample-sigconf.tex` to be used as a Pandoc template
;; - well, `$body` somewhere
;; - \tightlist command
;; - `$title$` and `$authors$` variables
;;
;; ## Todos
;; - [x]  Title
;; - [ ] Authors (_Note that authors' addresses are mandatory for journal articles._, aggregate affiliation (?)
;; - [ ] Bibliography (Bibtex vs. ~~Biblatex~~)
;; - [ ] Decide which template to use (e.g. `sample-sigconf`)
;; - [x] Adapt Heading (Sections) Hierarchy
;; - [ ] Results to Images or Pdf (?) (https://reachtim.com/include-html-in-latex.html)

(declare md->pandoc)
(def md->pandoc-transform
  {:doc (fn [{:as doc :keys [content]}]
          {:blocks (into [] (map md->pandoc) content)
           :pandoc-api-version [1 22]
           :meta {}})

   :heading (fn [{:keys [content heading-level]}] {:t "Header" :c [heading-level ["id" [] []] (keep md->pandoc content)]})
   :paragraph (fn [{:keys [content]}] {:t "Para" :c (keep md->pandoc content)})
   :plain (fn [{:keys [content]}] {:t "Plain" :c (keep md->pandoc content)})
   :blockquote (fn [{:keys [content]}] {:t "BlockQuote" :c (keep md->pandoc content)})
   :code (fn [{:as node :keys [language]}] {:t "CodeBlock" :c [["" [language "code"] []] (md.transform/->text node)]})
   :block-formula (fn [{:keys [text]}] {:t "Para" :c [{:t "Math" :c [{:t "DisplayMath"} text]}]})
   :formula (fn [{:keys [text]}] {:t "Math" :c [{:t "InlineMath"} text]})
   :ruler (fn [_] {:t "HorizontalRule"})

   :softbreak (fn [_] {:t "SoftBreak"})

   :em (fn [{:keys [content]}] {:t "Emph" :c (keep md->pandoc content)})
   :strong (fn [{:keys [content]}] {:t "Strong" :c (keep md->pandoc content)})
   :strikethrough (fn [{:keys [content]}] {:t "Strikeout" :c (keep md->pandoc content)})
   :link (fn [{:keys [attrs content]}] {:t "Link" :c [["" [] []] (keep md->pandoc content) [(:href attrs) ""]]})

   :monospace (fn [node] {:t "Code" :c [["" [] []] (md.transform/->text node)]})

   :list-item (fn [{:keys [content]}] (keep md->pandoc content))
   :bullet-list (fn [{:keys [content]}] {:t "BulletList" :c (keep md->pandoc content)})

   :text (fn [{:keys [text]}] {:t "Str" :c text})

   ;; TODO: https://pandoc.org/MANUAL.html#footnotes
   :footnote-ref (fn [_] nil)
   })

(defn md->pandoc
  [{:as node :keys [type]}]
  (if-some [xf (get md->pandoc-transform type)]
    (xf node)
    (throw (ex-info (str "Not implemented: '" type "'.") node))))

(defn pandoc-> [pandoc-data format]
  (let [{:keys [exit out err]}
        (apply sh (filter some?
                          ["pandoc" "--from" "json" "--to" format
                           "--standalone"                   ;; all the LaTeX preambolic stuff
                           ;;"--template=template-sigconf-biblatex.tex"
                           "--template=template-sigconf.tex"
                           "--pdf-engine=tectonic"
                           "--no-highlight"
                           (when (= "pdf" format) "--output=README.pdf")
                           ;; "--listings" ;; code via listings package (https://ctan.org/pkg/listings)
                           :in (json/write-str pandoc-data)]))]
    (if (zero? exit) out err)))

(defn pandoc<- [input format]
  (-> (sh "pandoc" "-f" format "-t" "json" :in input)
      :out (json/read-str :key-fn keyword)))

(defn meta-content [content] {:t "MetaInlines" :c [{:t "Str" :c content}]})

(defn add-authors [pandoc & authors]
  (assoc-in pandoc
            [:meta :author]
            {:t "MetaList"
             :c (map (fn [author]
                       {:t "MetaMap"
                        :c (into {} (map (fn [[k v]] [k (meta-content v)])) author)}) authors)}))

#_(add-authors {} {:name "X" :affiliation "Penguin Village University"})

(defn promote-headings [doc]
  (loop [z (md.parser/->zip doc)]
    (if (z/end? z)
      (z/root z)
      (recur (z/next (cond-> z
                       (= :heading (:type (z/node z)))
                       (z/edit update :heading-level dec)))))))

(defn clerk->pandoc [file]
  (let [{:keys [title footnotes blocks]} (clerk.eval/eval-file file)]
    (-> {:type :doc
         :title title
         :content (mapcat (fn [{:keys [type doc visibility text-without-meta]}]
                            (let [{:keys [code result]} visibility]
                              (case type
                                :markdown (:content doc)
                                ;; TODO: extract results (e.g.abstract)
                                :code (cond-> []
                                        (= :show code)
                                        (conj {:type :code
                                               :language "clojure"
                                               :content [{:type :text
                                                          :text text-without-meta}]}))))) blocks)}
        promote-headings
        (update :content (partial drop 2))
        md->pandoc
        (assoc-in [:meta :title] (meta-content title))
        (add-authors {:name "Martin Kavalar"
                      :email "martin@nextjournal.com"}
                     {:name "Philippa Markovics"
                      :email "philippa@nextjournal.com"}
                     {:name "Jack Rusher"
                      :email "jack@nextjournal.com"}))))

(comment
  ;; to latex
  (-> (clerk->pandoc "README.md")
      ;;:blocks (->> (take 2))
      (pandoc-> "pdf")
      ;;(pandoc-> "latex") #_ (subs 2000 5000)
      ;;(->> (spit "README.tex"))
      )

  (sh "open" "README.pdf")

  ;; debug tectonic
  (sh "tectonic" "README.tex")

  ;; get Pandoc AST for testing
  (-> "
---
title: 'This is the title: it contains a colon'
author:
- name: Author One
- name: Author Two
---
# Hey

This is a paragraph[^note]

> quote me like `this`
> and that

---
[^note]: Hello Note

" (pandoc<- "markdown+footnotes") :meta))
