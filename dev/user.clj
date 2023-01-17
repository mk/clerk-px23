(ns user
  (:require [nextjournal.clerk :as clerk]))

(clerk/serve! {:port 7676 :browse true})

(clerk/show! "README.md")
