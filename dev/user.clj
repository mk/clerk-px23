(ns user
  (:require [nextjournal.clerk :as clerk]))

(clerk/show! "README.md")
(clerk/serve! {:port 7676 :browse true :watch-paths ["README.md"]})

