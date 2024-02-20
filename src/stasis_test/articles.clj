(ns stasis-test.articles
  (:require [clojure.java.io :refer [as-url]]
            [clojure.string :as s]
            [markdown.core :refer [md-to-html-string]]))

(def socials ["https://github.com/aljedaxi" "mailto:alje@daxi.ml"])

(defn site [{:keys [children head title]}]
  [:html
   [:head
    ; [:link {:rel "webmention" :href "https://webmention.io/daxi.ml/webmention"}]
    ; [:link {:rel "pingback"   :href "https://webmention.io/daxi.ml/xmlrpc"}]
    [:link {:rel "stylesheet" :href "/daxi.css"}]
    [:meta {:charset "utf-8"}]
    [:link {:rel "icon" :href "/favicon.ico" :sizes "any"}]
    [:title (or title "daxi.ml")]
    ; (for [s socials] [:link {:rel "me" :href s}])
    head]
   [:body children]])

(defn layout [{:keys [children head title header]}]
  (site
    {:head head
     :children [:body [:main.wrapper
                       [:header header]
                       [:article children]]]}))

(defn restructure-article
  [[{uuid :uuid
     {the-type :type
      tags :tags
      action :action
      ver :ver
      published :published
      title :title} :properties}
    &
    content]]
  (merge {:uuid uuid :action action :tags tags :published published :type the-type :title title :content content}))

(defn dl [theMap] [:dl (mapcat (fn [[k v]] [[:dt k] [:dd v]]) theMap)])

(defn handleContent [content]
  (map
    (fn [{:keys [uuid content]}]
      [:section {:id uuid} (md-to-html-string content)])
    content))

(defn post [{:keys [title content tags published updated ver]}]
  (layout {:title title
           :head (seq [[:link {:rel "stylesheet" :href "/article.css"}]])
           :header (dl {:tags (s/join ", " tags)
                        :published published
                        :updated updated})
           :children (conj (handleContent content) [:h1 title])}))

(defn article2hiccup [article]
  (let [{action :action the-type :type :as all} article]
    (condp = action
      "ci/keep" (post all)
      "ci/revise" (post all)
      nil nil
      nil)))

