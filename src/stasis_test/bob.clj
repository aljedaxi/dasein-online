(ns stasis-test.bob
  (:require [clojure.data.json :as json]
            [clojure.xml :as xml]
            [clojure.string :as s]
            [markdown-to-hiccup.core :as m]
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


(defn first-val [tag-array] (some-> tag-array first (get :content) first s/trim))


(defn xml-thing-to-option [{{:keys [id]} :attrs content :content}]
  (let [mapped-tags (group-by #(get % :tag) content)
        {:keys [name coords summary impression write-up]} mapped-tags
        impressions (-> impression first :content)
        summaries-datafied
        (map-map impressions
                 (fn [{:keys [tag] {:keys [summary]} :attrs}]
                   [(as-data tag) summary]))]
    {:name (first-val name)
     :id id
     :write-up (first-val write-up)
     :url (format "%s/" id)
     :coords (if coords (s/split (first-val coords) #", ") coords)
     :summary (first-val summary)
     :data-set summaries-datafied}))


(defn parse-xml [{:keys [content] :as root}]
  (defn rejig-impression-feature
    [{[content] :content {:keys [value title]} :attrs}]
    {:value (or value content) :label content :title title})
  (let [{:keys [cafe impression-features]} (group-by :tag content)
        cafes (map xml-thing-to-option cafe)
        features (mapcat 
                   (fn [{:keys [content]}]
                     (map rejig-impression-feature content))
                   impression-features)]
    {:cafes cafes :features features}))

(def file-data (xml/parse "./resources/cafes.xml"))
(def stuff (parse-xml file-data))
(def xml-cafes (:cafes stuff))
(def features (:features stuff))
(def feature-options
  (map (fn [{:keys [label value title]}] [:option {:value value :title title} label])
       features))


(def graph
  (seq
    [[:spider-graph
      {:width 660
       :label "radar graph of coffee shops by feature"
       :features "datalist#features"
       :data "datalist#cafes"}]
     [:spider-legend {:datalist "datalist#cafes"}]
     [:datalist#cafes
      (map 
        (fn [{:keys [name data-set id url summary]}]
          [:option.cafe
           (assoc data-set :value id :data-href url :data-summary summary)
           name])
        xml-cafes)]
     [:datalist#features feature-options]]))


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
  (def graph
    (let [{:keys [name data-set id url summary]} shop]
      [:spider-graph
       {:width 660
        :label "radar graph of coffee shops by feature"
        :features "datalist#features"
        :data "datalist#cafes"}
      [:datalist#cafes
       [:option.cafe
        (assoc data-set :value id :data-href url :data-summary summary)
        name]]
     [:datalist#features feature-options]]))

  (layout
    {:title name
     :headstuff [:script {:type "module" :src "/public/spider.js"}]
     :children
     (seq
       [[:header [:h1 name]
         summary]
        graph
        (some->> write-up m/md->hiccup m/component)])}))


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
