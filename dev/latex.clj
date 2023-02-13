(ns latex
  {:nextjournal.clerk/visibility {:code :hide :result :hide} }
  (:require [nextjournal.clerk.parser :as parser]
            [clojure.java.shell :refer [sh]]
            [nextjournal.markdown :as md]
            [clojure.data.json :as json]
            [nextjournal.markdown.transform :as md.transform]))

(declare md->pandoc)

(def md-type->transform
  {:doc (fn [{:keys [content]}]
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
  (if-some [xf (get md-type->transform type)]
    (xf node)
    (throw (ex-info (str "Not implemented: '" type "'.") node))))

(defn pandoc-> [pandoc-data format]
  (let [{:keys [exit out err]} (sh "pandoc" "--from" "json" "--to" format
                                   "--standalone" ;; all the LaTeX preambolic stuff
                                   "--no-highlight" "--listings" ;; code via listings package (https://ctan.org/pkg/listings)
                                   :in (json/write-str pandoc-data))]
    (if (zero? exit) out err)))

(defn pandoc<- [input format]
  (-> (sh "pandoc" "-f" format "-t" "json" :in input)
      :out (json/read-str :key-fn keyword)))

(comment

  ;; to latex
  (-> (md/parse (slurp "README.md"))
      md->pandoc
      (pandoc-> "latex")
      #_ (->> (spit "README.tex")))

  ;; to pdf
  (sh "tectonic" "README.tex")

  ;; get Pandoc AST for testing
  (-> "# Hey

This is a paragraph[^note]

> quote me like `this`
> and that

---
[^note]: Hello Note

" (pandoc<- "markdown+footnotes")))
