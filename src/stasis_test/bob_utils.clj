(ns stasis-test.bob-utils
  (:require [clojure.string :as s]))


(defmacro depn [func-name threading-macro & args]
  `(defn ~func-name [arg#] (~threading-macro arg# ~@args)))


(defn map-map [keyed-col f]
  (->> keyed-col (mapcat f) (apply hash-map)))


(depn datafy ->> (format "data-%s"))
(depn as-data -> str (subs 1) datafy keyword)
(depn first-val some-> first (get :content) first s/trim)


(defn cafe->option-base [data-set {:keys [name id url summary]}]
  [:option.cafe
   (assoc data-set :value id :data-href url :data-summary summary)
   name])


(defn cafe->option [{:keys [name features id url summary] :as cafe}]
  (let [data-set (map-map features (fn [{:keys [value tag]}] [(as-data tag) value]))]
    (cafe->option-base data-set cafe)))


(depn feature-url ->> (format "criterion/%s/"))


(defn feature->option [{:keys [id] :as feature}]
  (let [url (or (:url feature) (feature-url id))
        value (or (:value feature) id)
        label (or (:label feature) id)]
    [:option
     (cond-> {:data-href url :id id}
       (not= value label) (assoc :value value))
     (or label id)]))


(defn cafe->option-ns [data-ns {:keys [features] :as cafe}]
  (let [ns-tag (keyword data-ns)
        base-feature (some-> (filter (fn [{:keys [tag]}] (= tag ns-tag)) features) first)
        value (some-> base-feature :value)
        nested-features (or (some-> base-feature :sub-features) '())
        data-set (map-map nested-features (fn [{:keys [value tag]}] [(as-data tag) value]))]
   (cafe->option-base (assoc data-set :data-value value) cafe)))


(defn parse-features [{:keys [content] :as root}]
  (defn handle-feature [{:keys [content] {:keys [id class label value]} :attrs}]
    (defn parse-sub-features [content]
      (let [{summary nil sub-features :feature} (group-by :tag content)]
        {:summary (some-> summary first s/trim)
         :sub-features (map handle-feature sub-features)}))

    (let [{:keys [summary sub-features]} (parse-sub-features content)]
      {:id id
       :class (some-> class (s/split #" ") set)
       :label (or label value id)
       :value (or value id)
       :url (format "criterion/%s/" id)
       :summary summary
       :sub-features sub-features}))

  (let [{:keys [feature]} (group-by :tag content)]
    (map handle-feature feature)))

(defn parse-cafes [{:keys [content] :as root}]
  (defn fuck [{:keys [tag content attrs]}]
    (let [{sub-tags false maybe-summary true} (group-by string? content)
          {:keys [summary write-up]
           real-tags nil} (group-by #(-> % :tag #{:summary :write-up}) sub-tags)
          value (or (get attrs :summary) (some-> maybe-summary first s/trim) "0")
          sub-features (map fuck real-tags)]
      {:sub-features sub-features
       :value value
       :summary (first-val summary)
       :write-up (first-val write-up)
       :tag tag}))

  (defn xml-thing-to-option [{{:keys [id]} :attrs content :content}]
    (let [mapped-tags (group-by :tag content)
          {:keys [name coords summary impression write-up]} mapped-tags
          latest-impression (first impression)
          {:keys [content attrs]} latest-impression
          {:keys [timestamp]} attrs
          features (map fuck content)]
      {:name (first-val name)
       :id id
       :write-up (first-val write-up)
       :url (feature-url id)
       :coords (if coords (s/split (first-val coords) #", ") coords)
       :summary (first-val summary)
       :features features}))

  (let [{:keys [cafe]} (group-by :tag content)]
    (map xml-thing-to-option cafe)))
