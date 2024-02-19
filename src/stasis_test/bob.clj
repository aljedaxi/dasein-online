(ns stasis-test.bob
  (:require [clojure.data.json :as json]
            [clojure.xml :as xml]
            [clojure.string :as s]
            [clojure.pprint :as pprint]))


(defn filter-on-key [keyName keyVal xs]
  (filter (fn [x] (= keyVal (get x keyName))) xs))


(defn map-map [keyed-col f]
  (->> keyed-col (mapcat f) (apply hash-map)))


(defn shop-link [{:keys [id name] :as shop}]
  (str (or id name)))


(defn shop-url [{:keys [id] :as shop}]
  (format "%s/index.html" (shop-link shop)))


(defn as-data [k]
  (keyword (format "data-%s" (-> k str (subs 1)))))


(defn de-data [k] (-> k str (subs 6)))


(defn map-list-item [{:keys [name summary id] :as shop}]
  [:li
   [:h2 {:id id} [:a {:href (shop-link shop)} name]]
   summary])


(defn layout
  [{:keys [children headstuff title]}]
  [:html
   [:head
    headstuff
    [:title title]]
   [:body
    [:main children]
    [:footer 
     [:a {:href "/coffee-bob/about-me"} "about me"]
     "&nbsp;"
     [:a {:href "/coffee-bob"} "home"]]]])


(defn first-val [tag-array] (-> tag-array first (get :content) first s/trim))


(defn xml-thing-to-option [{{:keys [id]} :attrs content :content}]
  (let [mapped-tags (group-by #(get % :tag) content)
        {:keys [name coords summary impression]} mapped-tags
        impressions (-> impression first :content)
        summaries-datafied
        (map-map impressions
                 (fn [{:keys [tag] {:keys [summary]} :attrs}]
                   [(as-data tag) summary]))]
    {:name (first-val name)
     :id id
     :coords (s/split (first-val coords) #", ")
     :summary (first-val summary)
     :data-set summaries-datafied}))

(defn parse-xml [{:keys [content] :as root}]
  (let [cafes (filter-on-key :tag :cafe content)]
    (map xml-thing-to-option cafes)))

(def xml-cafes (parse-xml (xml/parse "./resources/cafes.xml")))
(pprint/pprint xml-cafes)

(def graph
  [:spider-graph
   [:datalist#cafes
    (map 
      (fn [{:keys [name data-set id]}] [:option.cafe (assoc data-set :id id) name])
      xml-cafes)]
   [:datalist#axes
    (->> xml-cafes
         (map :data-set)
         (mapcat keys)
         set
         (map de-data)
         (map (fn [val] [:option val])))]])


(def coffee-bob
  (layout
    {:title "calgary coffee bob"
     :headstuff [:script {:type "module" :src "/public/spider.js"}]
     :children 
     (seq 
       [[:h1 "the calgary coffee bob"]
        [:p "for the snob, and bob"]
        graph
        [:ul (map map-list-item xml-cafes)]])}))


(defn shop-page 
  "This generates the page that each individual shop gets"
  [{:keys [name write-up summary] :as shop}]
  (layout
    {:title name
     :children (or write-up summary)}))


(def shop-pages
  (map-map xml-cafes (fn [shop] [(shop-url shop) (shop-page shop)])))


(def about-me
  (layout
    {:title "about me"
     :children 
     (seq
       [[:h1 "here's a picture of me :3"]
        [:img {:src "/public/bob.avif"}]])}))


(def pages
  (merge
    shop-pages
    {"about-me/index.html" about-me
     "index.html" coffee-bob}))
