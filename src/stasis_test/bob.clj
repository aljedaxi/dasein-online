(ns stasis-test.bob
  (:require [clojure.data.json :as json]
            [clojure.xml :as xml]
            [clojure.string :as s]
            [clojure.pprint :as pprint]))


(defn filter-on-key [keyName keyVal xs]
  (filter (fn [x] (= keyVal (get x keyName))) xs))


(defn map-map [keyed-col f]
  (->> keyed-col (mapcat f) (apply hash-map)))


(defn as-data [k]
  (keyword (format "data-%s" (-> k str (subs 1)))))


(defn de-data [k] (-> k str (subs 6)))


(defn map-list-item [{:keys [name summary id] :as shop}]
  [:li
   [:h2 {:id id} [:a {:href id} name]]
   summary])


(defn stylesheet [href] [:link {:rel "stylesheet" :href href}])


(defn layout
  [{:keys [children headstuff title]}]
  [:html
   [:head
    headstuff
    (stylesheet "https://unpkg.com/normalize.css@8.0.1/normalize.css")
    (stylesheet "https://unpkg.com/concrete.css@2.1.1/concrete.css")
    [:style "svg > text {fill: var(--fg);} figure {margin: 0} header {padding: 8rem 0}"]
    [:title title]]
   [:body
    [:main
     children
     [:hr]
     [:footer 
      [:a {:href "/coffee-bob/about-me"} "about me"]
      "&nbsp;"
      [:a {:href "/coffee-bob"} "home"]]]]])


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
     :url (format "%s/index.html" id)
     :coords (s/split (first-val coords) #", ")
     :summary (first-val summary)
     :data-set summaries-datafied}))




(defn parse-xml [{:keys [content] :as root}]
  (let [{:keys [cafe impression-features]} (group-by :tag content)
        cafes (map xml-thing-to-option cafe)
        features (mapcat 
                   (fn [{:keys [content]}]
                     (map (fn [{[content] :content}] content) content))
                   impression-features)]
    {:cafes cafes :features features}))

(def file-data (xml/parse "./resources/cafes.xml"))
(def stuff (parse-xml file-data))
(def xml-cafes (:cafes stuff))
(def features (:features stuff))


(def graph
  (seq
    [[:spider-graph
      {:width 660 :title "radar graph of coffee shops by feature" :features "datalist#axes" :data "datalist#cafes"}]
     [:spider-legend {:datalist "datalist#cafes"}]
     [:datalist#cafes
      (map 
        (fn [{:keys [name data-set id url summary]}]
          [:option.cafe
           (assoc data-set :value id :data-href url :data-summary summary)
           name])
        xml-cafes)]
     [:datalist#axes
      (map (fn [val] [:option val]) features)]]))


(def coffee-bob
  (layout
    {:title "calgary coffee bob"
     :headstuff [:script {:type "module" :src "/public/spider.js"}]
     :children 
     (seq 
       [[:header [:h1 "the calgary coffee bob"]
         [:p "a celebration of any aspect of anywhere that serves coffee"]]
        [:section graph]])}))


(defn shop-page [{:keys [name write-up summary] :as shop}]
  (layout
    {:title name
     :children (or write-up summary)}))


(def shop-pages
  (map-map xml-cafes (fn [shop] [(:url shop) (shop-page shop)])))


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
