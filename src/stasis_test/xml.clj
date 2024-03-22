(ns stasis-test.xml
  (:require [clojure.xml :as xml]
            [stasis-test.bob-utils :refer [depn parse-cafe]]
            [clojure.java.io :as io]))

;<?xml version='1.0' encoding='utf-8'?>

(depn folder-file-names ->> io/as-file file-seq (map str))
(depn cafe-files ->> folder-file-names (filter #(re-find #"\.cafe\.xml" %)))
(depn get-cafes ->> cafe-files (map xml/parse) (map parse-cafe))
