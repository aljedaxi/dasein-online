(ns stasis-test.core
  (:require [stasis.core :as stasis]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [optimus.prime :as optimus]
            [optimus.assets :as assets]
            [optimus.export]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [optimus.optimizations :as optimizations]
            [optimus.strategies :refer [serve-live-assets]]
            [stasis-test.util :refer [parse-ndjson trace mapkeys mapmap]]
            [stasis-test.articles :refer [site layout restructure-article article2hiccup]]
            [stasis-test.bob]
            [hiccup.page :refer [html5]]))

(def quips ["i am not",
	          "welcome to my homepage! you can have fun here!",
	          "encoding programs into javascript, epic style",
	          "made with <a href=\"https://github.com/magnars/stasis\">stasis</a>, for fun",
	          "view the source code <a href=\"https://github.com/aljedaxi/daxi.ml\">here</a>",
	          "i wish i could write something that wasn't secretly an ad"])


(defn get-assets []
  (->> (assets/load-assets "public" [#"/.*\.(avif|ico|js|gif)"])
       (map #(assoc % :path (format "/public%s" (:path %))))))


(def seqed-pages
  (->> (stasis/slurp-directory "resources/logseqing/" #"\.ndjson$")
       (mapmap
         #(try
            (vector
              (-> %1 (s/replace #"\s+" "-") (s/replace #"\.ndjson" "/index.html"))
              (some->> %2 parse-ndjson restructure-article article2hiccup html5))
            (catch Throwable e
              (prn (format "Error in file %s" %1)))))))


(def children 
  (seq [[:header#quip "swag"]
        [:main 
         [:h2 "good articles:"]
         [:ul
          (for [[k v] seqed-pages :when v]
            [:li
             [:a {:href k} (-> k (s/split #"/") (nth 1) (s/replace  #"-" " "))]])]
         [:h2 "other things:"]
         [:ul
          [:li "this is a personal website"]]
         [:datalist#quips (for [s quips] [:option s])]]]))

(def quiper "const options = document.querySelector('#quips').options;\nconst option = options[Math.floor(Math.random() * options.length)];\ndocument.querySelector('#quip').innerHTML = option.innerHTML;")

(def pages
  {"/index.html" (html5
                   (site
                     {:children children
                      :head (seq [[:script {:type "module"} quiper]])
                      :title "daxi.ml landing page"}))})


(defn get-pages [] 
  (merge pages
         seqed-pages
         (apply
           hash-map
           (mapcat
             (fn [[page-path page]] [(format "/coffee-bob/%s" page-path) (html5 page)])
             stasis-test.bob/pages))
         (stasis/slurp-directory "resources/public" #"\.css$")))

(def app (-> (stasis/serve-pages get-pages {:stasis/ignore-nil-pages? true})
             (optimus/wrap get-assets optimizations/none serve-live-assets)
             wrap-content-type))


(defn export [target-dir]
  (stasis/empty-directory! target-dir)
  (optimus.export/save-assets (get-assets) target-dir)
  (stasis/export-pages (get-pages) target-dir {:stasis/ignore-nil-pages? true}))
