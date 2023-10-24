(ns stasis-test.util
  (:require [clojure.data.json :refer [read-str]]
            [stasis-test.articles :refer [restructure-article article2hiccup]]
            [clojure.string :refer [split-lines]]
            [hiccup.page :refer [html5]]))

(defn trace [s]
  (prn s)
  s)

(defn parse-ndjson [s]
  (->> s
       split-lines
       (map #(read-str % :key-fn keyword))))

(defn mapkeys [f v]
  (->> v
       (mapcat (fn [[k v]] [k (f v)]))
       (apply hash-map)))

(defn mapmap [f v]
  (->> v
       (mapcat (fn [[k v]] (f k v)))
       (apply hash-map)))
