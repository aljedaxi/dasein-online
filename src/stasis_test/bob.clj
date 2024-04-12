(ns stasis-test.bob
  (:require [clojure.data.json :as json]
            [clojure.xml :as xml]
            [clojure.string :as s]
            [markdown-to-hiccup.core :as m]
            [stasis-test.util :as u]
            [stasis-test.xml :as stxml]
            [clojure.java.io :as io]
            [stasis-test.bob-utils
             :refer [cafe->option map-map depn feature->option cafe->option-ns parse-features parse-cafes
                     feature-url
                     parse-cafe
                     ]]
            [stasis-test.html-utils :as h]
            [clojure.edn :as edn]
            [stasis.core :as stasis]
            [clojure.pprint :as pprint]))


(def spider-stuff
  (list [:style "svg > text {fill: var(--fg);}"]
        [:script {:type "module" :src "/public/spider.js"}]))


(def bottom-links
  [[:a {:href "about"} "about"]
   [:a {:href "about-me"} "about me"]
   [:a {:href "/coffee-bob"} "home"]
   [:a {:href "https://wiki.p2pfoundation.net/Peer_Production_License"} "PPL"]
   [:button {:type "button" :style "margin: 0" :onclick "void dispenseMittens()"} "coffee"]
   [:span "v0.1.0"]])


(defn layout [{:keys [children headstuff title]}]
  [:html
   [:head
    headstuff
    (h/stylesheet "https://unpkg.com/normalize.css@8.0.1/normalize.css")
    (h/stylesheet "https://unpkg.com/concrete.css@2.1.1/concrete.css")
    [:meta {:charset "utf-8"}]
    [:link {:rel "icon" :href "/public/favicon.ico" :sizes "any"}]
    [:base {:href "/coffee-bob/"}]
    [:script {:src "/public/mittens.js"}]
    [:style "header {padding: 8rem 0}"]
    [:title title]]
   [:body
    [:main
     children
     [:hr]
     [:footer (interpose " ❧ " bottom-links)]]]])


(def cafes
  (concat 
    (stxml/get-cafes "resources/bobbing")
    (->> "./resources/cafes.xml"
       xml/parse
       ((fn [{:keys [content] :as root}]
         (let [{:keys [cafe]} (group-by :tag content)]
           (map parse-cafe cafe)))))))


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
  (let [h1 (list "the calgary " [:a {:href (feature-url "coffee")} "coffee "]
                  [:a {:href "about/"} "bob"])]
    (layout
      {:title "calgary coffee bob"
       :headstuff spider-stuff
       :children 
       (list
         (h/header h1 "a celebration of any aspect of anywhere that serves coffee")
         [:section graph])})))


(defn value-for-feature [feature-id cafe]
  (or (some->> cafe :features (filter #(= feature-id (:tag %))) first :value read-string)
      0))


(defn feature-page [{:keys [id label summary class sub-features class] :as feature}]
  (let [head (h/header label summary)
        all-sub-features 
        (if class
          (cond->             sub-features 
            true              (conj {:id "value" :label "summary"})
            (class "priced")  (conj {:id "price"})
            (class "various") (conj {:id "variety"})))
        sorted-features
        (sort #(> (value-for-feature (keyword id) %1)
                  (value-for-feature (keyword id) %2))
              cafes)
        cafe-options [:datalist#cafes (map #(cafe->option-ns id %) sorted-features)]
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


(defn link2feature [keywd] (feature-url (subs (str keywd) 1)))


(def feature-map (group-by :id features))
(depn tag->feature some->> (get feature-map))
(depn kw->str some-> str (subs 1))
(depn cup some->> m/md->hiccup h/without-div)

(def breaker-style ".centered { text-align: center }")
(defn breaker [& stuff] (list [:hr] [:div.centered stuff] [:hr]))
(defn of-three [value]
  [:span [:sup value] "&frasl;" [:sub (h/a "3" "about/methodology/#nps")]])


(defn shop-page [{:keys [name write-up summary coords features] :as shop}]
  (def graph
    (default-graph {}
      [:datalist#cafes (cafe->option shop)]
      [:datalist#features feature-options]))

  (def location-link (some-> coords (coord->link "location")))

  (defn feature->section [{:keys [value summary write-up tag sub-features]} head-level]
    (let [feature (some-> tag kw->str tag->feature first)
          rating (if (some-> value read-string (> 0)) (of-three value))
          title [(keyword (format "h%d" head-level)) [:a {:href (link2feature tag)} tag]]
          show-details (and summary write-up)]
      (list
        [:div.golden-ratio title rating]
        (cond
          show-details [:details [:summary (drop 2 (first (cup summary)))]
                        (cup write-up)
                        (if (seq sub-features)
                          [:section.dented
                           (map #(feature->section % (+ 1 head-level))
                                sub-features)])]
          summary      (cup summary)
          write-up     (cup write-up)))))

  (layout
    {:title name
     :headstuff (list
                  spider-stuff
                  (h/stylesheet "/silly-details.css")
                  [:style breaker-style ".golden-ratio {display: grid;grid-template-columns: 1.618033988749894fr 1fr;align-items: center;} .dented {margin-inline-start: 20.75px; padding: 0} .golden-ratio + p {margin: 0}"])
     :children
     (list
       (h/header name summary)
       graph
       (cup write-up)
       (breaker location-link)
       (map #(feature->section % 2) features))}))


(depn indexify -> :url (format "%index.html"))


(def shop-pages
  (map-map cafes (fn [shop] [(indexify shop) (shop-page shop)])))


(def feature-pages
  (map-map features (fn [feature] [(indexify feature) (feature-page feature)])))


(def glossary
  (layout
    {:title "glossary"
     :children
     (list
       (h/header "glossary" "strange and technical terms"))}))


(def about-me
  (layout
    {:title "about me"
     :children 
     (seq
       [[:h1 "here's a picture of me :3"]
        [:img {:src "/public/bob.avif"}]])}))


(def about
  (with-open [rdr (io/reader "./resources/bobbing/about.md")]
    (let [[head markdown]  (split-with #(not= "---" %) (line-seq rdr))
          {:keys [header] :as stuff} (->> head (s/join "\n") edn/read-string)
          body             (some->> markdown (s/join "\n") m/md->hiccup h/without-div)]
      (layout
        {:title "about the bob"
         :headstuff [:style breaker-style]
         :children
         (seq
           [[:header (seq header)]
            (breaker (h/a "methodology" "about/methodology"))
            body])}))))


(def methodology
  (layout
    {:title "methodology"
     :children
     (list
       (h/header "methodology"
                 "am i using methodology right? this is an " (h/a "account" "https://en.wikipedia.org/wiki/Fundamental_ontology") " of my methods")
       [:h2#nps "the 0-3 scale"]
       [:p
        "the 0-3 scale is an abuse of " (h/a "Net Promoter Scores" "https://apenwarr.ca/log/20231204") ". the basic idea is that you have three kinds of customers:"
       [:ol
        [:li "people that love your stuff: your " [:em "Promoters"]]
        [:li "people that don't feel any particular way about your stuff."]
        [:li "and people that hate your stuff: your " [:em "Detractors"]]]
       "because the average rating on google maps is 4 stars, 1/5 to 3/5 are your detractors; 5/5 are your promoters; 4/5 doesn't matter."]
       [:p "i like the system RE: this website because it's about " [:em "promotion"] ". i made this website because i want to promote some coffee shops (and kinda because i want to hate on some others). " [:abbr {:title "Net Promoter Score"} "NPS"] " is designed to express exactly that: the desire to " [:em "actively"] " bring up products/companies/pillars of the community."]
       [:p
        "the act of flattening the standard 1-10 scale into a 1-3 scale is a " (h/a "useful analytical tool" "https://existentialcomics.com/comic/290") " if you're someone in marketing that needs a good, single metric. because there's only one of " (h/a "me" "about-me/") ", it's not super important " [:em "here"] "."]
       [:p
        "this ignores that a score is more than a score: it's an index. when " (h/a "Anthony Fantano" "https://www.youtube.com/@theneedledrop") " gives " (h/a "an album you thought was great a 6" "https://www.youtube.com/watch?v=OelpOL9bLTY") ", there's this understanding that he thought, idk, " [:em "insert whatever 7/10 album here"] " was better than that thing you liked. a ranking emerges. everything is relative to everything else on the stack."]
       [:p "this is especially relevant when you're me, and you want to help, say, travellers who'll only be able to visit one cafe find the best cafe possible. you need to be able to sort these things, and have the best of the best be visibly skimmable."]
       [:aside "(as a teen, i watched a " (h/a "negative review of an anime" "https://www.youtube.com/watch?v=_1_T6XJKkSQ") " and understood that i would love it. i did love it. i could never have had an experience like that if the video didn't go into every aspect of the anime, in the same way i would have never heard about aubade if the snob didn't go into every aspect of each cafe. what i like about the format of this site is that we can losslessly surface those little facets of the experience.)"]
       [:h2#writing "how to write good"]
       [:p "write drunk; edit————never; XDDDDDDDDDDDDD"]
       )}))


(def pages
  (merge
    shop-pages
    feature-pages
    {"about-me/index.html" about-me
     "about/index.html" about
     "about/methodology/index.html" methodology
     "glossary/index.html" glossary
     "index.html" coffee-bob}))
