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
  (:import (javax.imageio ImageIO)))

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
;; - [ ] Use `teaserfigure` for figures spanning the whole width
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
   :image (fn [{:as node :keys [attrs]}]
            (let [{:keys [src]} attrs
                  caption (md.transform/->text node)]
              {:t "Image",
               :c [["" [] [["width" "linewidth"]]]
                   [{:t "Str" :c caption}]
                   ;; a fig: will wrap the resulting \includegraphics in a figure environment
                   [src (str (when (not-empty caption) "fig:"))]]}))
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

(defn pandoc-> [pandoc-data format & {:keys [template] :or {template "template-sigconf.tex"}}]
  (let [{:keys [exit out err]}
        (apply sh (filter some?
                          ["pandoc" "--from" "json" "--to" format
                           (when template (str "--template=" template))
                           "--pdf-engine=tectonic"
                           ;;"--no-highlight"
                           (when (= "pdf" format) "--output=README.pdf")
                            "--listings" ;; code via listings package (https://ctan.org/pkg/listings)
                           :in (json/write-str pandoc-data)]))]
    (if (zero? exit) out err)))

(defn pandoc<- [input format]
  (-> (sh "pandoc" "-f" format "-t" "json" :in input)
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

(defn store-image-src! [{:keys [id src]}]
  ;; CDN url responds with 451 (Unavailable for legal reasons)
  (let [url (java.net.URL. (str/replace src "cdn." ""))
        path (fs/path "images" (str id ".png"))]
    (fs/create-dirs "images")
    (when-not (fs/exists? path)
      (ImageIO/write (ImageIO/read url) "png" (fs/file path)))
    (str path)))

(defn convert-result [{:as block :keys [id result]}]
  (let [{:as opts :keys [src caption]} (v/->value result)
        result-screenshot-path (str "images/result-" (name id) ".png")]
    (cond
      src
      {:type :paragraph ;; NOTE: Pandoc doesn't support images at block level
       :content [{:type :image
                  :attrs {:src (store-image-src! opts)}
                  :content [{:type :text :text caption}]}]}
      (fs/exists? result-screenshot-path)
      {:type :paragraph
       :content [{:type :image
                  :attrs {:src result-screenshot-path}}]})))

(defn conj-some [xs x] (cond-> xs x (conj x)))

(defn clerk->pandoc [file]
  (let [{:as clerk-doc :keys [title footnotes blocks]} (clerk.eval/eval-file file)]
    (-> {:type :doc
         :title title
         :content (mapcat (fn [{:as block :keys [type doc visibility text-without-meta]}]
                            (let [{code-visibility :code result-visibility :result} visibility]
                              (case type
                                :markdown (:content doc)
                                :code (cond-> []
                                        (= :show code-visibility)
                                        (conj {:type :code
                                               :language "clojure"
                                               :content [{:type :text :text text-without-meta}]})
                                        (= :show result-visibility)
                                        (conj-some (convert-result block))))))
                          ;; drop custom abstract and helpers
                          (drop 5 blocks))}
        promote-headings
        md->pandoc
        (assoc-in [:meta :title] (meta-content title))
        (assoc-in [:meta :abstract] (meta-content (get-abstract clerk-doc)))
        (assoc-in [:meta :keyword] (meta-list (map meta-content ["Literate Programming" "Moldable Development"])))
        (add-authors {:name "Martin Kavalar"
                      :email "martin@nextjournal.com"}
                     {:name "Philippa Markovics"
                      :email "philippa@nextjournal.com"}
                     {:name "Jack Rusher"
                      :email "jack@nextjournal.com"}))))

(defn manual-fixes [latex]
  (-> latex
      ;; Force images to be non-floating
      (str/replace "\\begin{figure}" "\\begin{figure}[H]")))

(comment

  ;; to latex
  (-> (clerk->pandoc "README.md")
      (pandoc-> "latex") #_(subs 5000 8000)
      manual-fixes
      (->> (spit "README.tex")))

  ;; to pdf
  (do
    (sh "tectonic" "README.tex")
    (sh "open" "README.pdf"))

  ;; capture screenshots (needs ../clerk at
  (sh "yarn" "nbb" "-m" "screenshots" "--url" "http://localhost:7676" "--out-dir" "../../clerk-px23/images"
      :dir "../clerk/ui_tests")

  ;; get Pandoc AST for testing
  (-> "![An Alt Text](real-image.png 'A title'){width=100%}"
      (pandoc<- "markdown+footnotes")
      #_(pandoc-> "latex" :template nil))

  (-> (clerk.eval/eval-file "README.md")
      :blocks
      (->> (drop 6))
      first))
