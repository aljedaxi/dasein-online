(ns stasis-test.bob
  (:require [clojure.data.json :as json]
            [clojure.xml :as xml]
            [clojure.string :as s]
            [markdown-to-hiccup.core :as m]
            [clojure.pprint :as pprint]))

(defmacro depn [func-name threading-macro & args]
  `(defn ~func-name [arg#] (~threading-macro arg# ~@args)))


(defn filter-on-key [keyName keyVal xs]
  (filter (fn [x] (= keyVal (get x keyName))) xs))


(defn map-map [keyed-col f]
  (->> keyed-col (mapcat f) (apply hash-map)))


(depn datafy ->> (format "data-%s"))
(depn as-data -> str (subs 1) datafy keyword)
(depn first-val some-> first (get :content) first s/trim)


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
    [:base {:href "/coffee-bob/"}]
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


(defn xml-thing-to-option [{{:keys [id]} :attrs content :content}]
  (let [mapped-tags (group-by :tag content)
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

  (let [{:keys [cafe]} (group-by :tag content)
        cafes (map xml-thing-to-option cafe)]
    cafes))


(def file-data (xml/parse "./resources/cafes.xml"))
(def xml-cafes (parse-xml file-data))

(defn parse-features [{:keys [content] :as root}]
  (defn parse-sub-features [content]
    (let [{summary nil sub-features :feature} (group-by :tag content)]
      {:summary (some-> summary first s/trim)
       :sub-features (map handle-feature sub-features)}))

  (defn handle-feature [{:keys [content] {:keys [id class label value]} :attrs}]
    (let [{:keys [summary sub-features]} (parse-sub-features content)]
      {:id id
       :class (some-> class (s/split #" ") set)
       :label (or label value id)
       :value (or value id)
       :summary summary
       :sub-features sub-features}))

  (let [{:keys [feature]} (group-by :tag content)]
    (map handle-feature feature)))

(def new-features (->> "./resources/specs.xml" xml/parse parse-features))
(def features
  (->> new-features
       (filter (fn [{:keys [class]}] (not ((or class #{}) "hidden"))))
       (map (fn [{:keys [value label summary id]}]
              {:value value :label label :title summary :id id}))))

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
