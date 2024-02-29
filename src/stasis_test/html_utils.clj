(ns stasis-test.html-utils
  (:require [markdown-to-hiccup.core :as m]))


(defn header [h1 summary] (list [:header [:h1 h1] summary]))


(defn stylesheet [href] [:link {:rel "stylesheet" :href href}])


(defn without-div [hiccup]
  (let [[tag opts children] (m/component hiccup)]
    children))
