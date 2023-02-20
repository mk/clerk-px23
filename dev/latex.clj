(ns latex
  {:nextjournal.clerk/visibility {:code :hide :result :hide} }
  (:require [nextjournal.clerk.parser :as parser]
            [clojure.java.shell :refer [sh]]
            [nextjournal.markdown :as md]
            [clojure.data.json :as json]
            [nextjournal.markdown.transform :as md.transform]))

;; # $\LaTeX$ Conversion
;; We're using [Pandoc](pandoc.org) and [Tectonic]() following [submission guidelines](https://2023.programming-conference.org/home/px-2023#submissions)
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
;; - [ ] Authors (_Note that authors' addresses are mandatory for journal articles._)
;; - [ ] Bibliography (Bibtex vs. Biblatex)
;; - [ ] Decide which template to use (e.g. `sample-sigconf`)
;; - [ ] Adapt Heading (Sections) Hierarchy
;; - [ ] Results to Images or Pdf (?) (https://reachtim.com/include-html-in-latex.html)

(declare md->pandoc)

(defn doc->meta [{:keys [title]}]
  {:title {:t "MetaInlines" :c [{:t "Str" :c title}]}})

(defn add-authors [pandoc & authors]
  (assoc-in pandoc
            [:meta :author]
            {:t "MetaList"
             :c (map (fn [author]
                       {:t "MetaMap"
                        :c (into {}
                                 (map (fn [[k v]] [k {:t "MetaInlines" :c [{:t "Str" :c v}]}]))
                                 author)}) authors)}))
#_
(add-authors {} {:name "X" :affiliation "Penguin Village University"})

(def md-type->transform
  {:doc (fn [{:as doc :keys [title content]}]
          {:blocks (into [] (map md->pandoc) content)
           :pandoc-api-version [1 22]
           :meta (doc->meta doc)})

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
  (if-some [xf (get md-type->transform type)]
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

(comment
  ;; to latex
  (-> (md/parse (slurp "README.md"))
      md->pandoc
      (add-authors {:name "Martin Kavalar"
                    :email "martin@nextjournal.com"}
                   {:name "Philippa Markovics"
                    :email "philippa@nextjournal.com"}
                   {:name "Jack Rusher"
                    :email "jack@nextjournal.com"})

      (pandoc-> "pdf")
      ;;(pandoc-> "latex") (->> (spit "README.tex"))
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
