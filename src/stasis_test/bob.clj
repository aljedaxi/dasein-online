(ns stasis-test.bob
  (:require [clojure.data.json :as json]
            [clojure.xml :as xml]
            [clojure.string :as s]
            [markdown-to-hiccup.core :as m]
            [stasis-test.util :as u]
            [clojure.java.io]
            [stasis-test.bob-utils
             :refer [cafe->option map-map depn feature->option cafe->option-ns parse-features parse-cafes]]
            [stasis-test.html-utils :as h]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]))


(def spider-stuff
  (list [:style "svg > text {fill: var(--fg);}"]
        [:script {:type "module" :src "/public/spider.js"}]))


(defn layout [{:keys [children headstuff title]}]
  [:html
   [:head
    headstuff
    (h/stylesheet "https://unpkg.com/normalize.css@8.0.1/normalize.css")
    (h/stylesheet "https://unpkg.com/concrete.css@2.1.1/concrete.css")
    [:meta {:charset "utf-8"}]
    [:link {:rel "icon" :href "/public/favicon.ico" :sizes "any"}]
    [:base {:href "/coffee-bob/"}]
    [:style "header {padding: 8rem 0}"]
    [:title title]]
   [:body
    [:main
     children
     [:hr]
     [:footer 
      [:a {:href "/coffee-bob/about-me"} "about me"]
      "&nbsp;"
      [:a {:href "/coffee-bob"} "home"]]]]])


(def cafes (->> "./resources/cafes.xml" xml/parse parse-cafes))


(def features (->> "./resources/specs.xml" xml/parse parse-features))


(def feature-options
  (->> features
       (filter (fn [{:keys [class]}] (not ((or class #{}) "hidden"))))
       (map feature->option)))


(defn default-graph [{:keys [width label features data]} & children]
  [:spider-graph
   {:width (or width 660)
    :label (or label "radar graph of coffee shops by feature")
    :features (or features "datalist#features")
    :data (or data "datalist#cafes")}
   children])


(def graph
  (list
    (default-graph {})
    [:spider-legend {:datalist "datalist#cafes"}]
    [:datalist#cafes (map cafe->option cafes)]
    [:datalist#features feature-options]))


(def coffee-bob
  (let [h1 (list "the calgary " [:a {:href "criterion/coffee"} "coffee "]
                  [:a {:href "about/"} "bob"])]
    (layout
      {:title "calgary coffee bob"
       :headstuff spider-stuff
       :children 
       (list
         (h/header h1 "a celebration of any aspect of anywhere that serves coffee")
         [:section graph])})))


(defn feature-page [{:keys [id label summary class sub-features class] :as feature}]
  (let [head (h/header label summary)
        all-sub-features 
        (if class (cond-> sub-features
                    (class "priced") (conj {:id "price"})
                    (class "various") (conj {:id "variety"})))
        cafe-options [:datalist#cafes (map #(cafe->option-ns id %) cafes)]
        feature-list [:datalist#features (map feature->option all-sub-features)]
        graph (list
                (default-graph {})
                [:spider-legend {:datalist "datalist#cafes"}]
                cafe-options
                feature-list)]
    (layout
      {:title label
       :headstuff spider-stuff
       :children 
       (list
         head
         graph)})))


(defn coord->link [[x y] text]
  [:a {:href (format "https://www.openstreetmap.org/#map=20/%s/%s" x y)} text])


(defn link2feature [keywd]
  (format "criterion/%s" (subs (str keywd) 1)))


(def feature-map (group-by :id features))
(depn tag->feature some->> (get feature-map))
(depn kw->str some-> str (subs 1))

(defn shop-page [{:keys [name write-up summary coords features] :as shop}]
  (def graph
    (default-graph {}
      [:datalist#cafes (cafe->option shop)]
      [:datalist#features feature-options]))

  (def location-link (some-> coords (coord->link "location")))

  (defn feature->section [{:keys [value summary write-up tag sub-features]}]
    (let [feature (some-> tag kw->str tag->feature first)
          rating (if (some-> value read-string (> 0))
                   [:span [:sup value] "&frasl;" [:sub "3"]])
          title [:h2 [:a {:href (link2feature tag)} tag]]]
      (list
        [:div.golden-ratio title rating]
        (cond
          (and summary write-up) [:details [:summary summary] [:p write-up]]
          summary                [:p summary]
          write-up               [:p write-up]))))

  (layout
    {:title name
     :headstuff (list
                  spider-stuff
                  [:style ".centered { text-align: center } .golden-ratio {display: grid;grid-template-columns: 1.618033988749894fr 1fr;align-items: center;}"])
     :children
     (list
       (h/header name summary)
       graph
       (some->> write-up m/md->hiccup h/without-div)
       [:hr]
       [:div.centered location-link]
       [:hr]
       (map feature->section features))}))


(depn indexify -> :url (format "%index.html"))


(def shop-pages
  (map-map cafes (fn [shop] [(indexify shop) (shop-page shop)])))


(def feature-pages
  (map-map features (fn [feature] [(indexify feature) (feature-page feature)])))


(def about-me
  (layout
    {:title "about me"
     :children 
     (seq
       [[:h1 "here's a picture of me :3"]
        [:img {:src "/public/bob.avif"}]])}))


(def about
  (with-open [rdr (clojure.java.io/reader "./resources/bobbing/about.md")]
    (let [[head markdown]  (split-with #(not= "---" %) (line-seq rdr))
          {:keys [header] :as stuff} (->> head (s/join "\n") edn/read-string)
          body             (some->> markdown (s/join "\n") m/md->hiccup h/without-div)]
      (layout
        {:title "about the bob"
         :children
         (seq
           [[:header (seq header)]
            body])}))))


(def pages
  (merge
    shop-pages
    feature-pages
    {"about-me/index.html" about-me
     "about/index.html" about
     "index.html" coffee-bob}))
