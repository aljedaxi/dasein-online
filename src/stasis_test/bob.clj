(ns stasis-test.bob)

; (def coffee-qualities
;   #{:petrolium :dont :whatever :par :good! :baller})

; (def shops
;   [{:name "European Bakery"
;     :id :european-bakery
;     :good-sit? :no-seating
;     :coords [51.03766612184806, -114.07219196898996]
;     :has-turkish? true
;     :has-good-food? true
;     :has-cheap-food? true
;     :coffee-quality :good!
;     :summary [:p "a bakery that serves surprisingly good turkish coffee"]
;     :write-up [:p "they only serve turkish coffee here."]}
;    {:name "T2722"
;     :id :t2722
;     :coords [51.04302902799366, -114.03895497405416]
;     :coffee-quality :baller
;     :summary [:p "the absolute best of the best"]}
;    {:name "Treno"
;     :id :treno
;     :coords [51.04390259926328, -114.07166748754675]
;     :summary [:p "prosecco bar. don't get the coffee lol"]
;     :coffee-quality :petrolium}
;    {:name "MobSquad Cafe"
;     :id :mobsquad
;     :coords [51.045022338076215, -114.06522155050115]
;     :summary [:p "a nice view. don't get the coffee lol"]
;     :coffee-quality :petrolium}
;     ])

; (defn shop-url [{:keys [id] :as shop}]
;   (format "%s/index.html" (subs (str id) 1)))

; (defn shop-link [{:keys [id] :as shop}]
;   (subs (str id) 1))

; (defn map-list-item [{:keys [name summary id] :as shop}]
;   [:li
;    [:h2 {:id id} [:a {:href (shop-link shop)} name]]
;    summary])

; (defn layout
;   [{:keys [children headstuff title]}]
;   [:html
;    [:head
;     headstuff
;     [:title title]]
;    [:body
;     [:main children]
;     [:footer 
;      [:a {:href "/coffee-bob/about-me"} "about me"]
;      "&nbsp;"
;      [:a {:href "/coffee-bob"} "home"]]]])

; (def coffee-bob
;   (layout
;     {:title "calgary coffee bob"
;      :headstuff
;        [:script {:type "module" :src "/public/spider.js"}]
;      :children 
;      (seq 
;        [[:h1 "the calgary coffee bob"]
;         [:p "for the snob, and bob"]
;         [:div#container]
;         [:ul (map map-list-item shops)]])}))

; (defn shop-page 
;   "This generates the page that each individual shop gets"
;   [{:keys [name write-up summary] :as shop}]
;   (layout
;     {:title name
;      :children (or write-up summary)}))

; (def shop-pages
;   (apply
;     hash-map
;     (mapcat
;       (fn [{:keys [id write-up] :as shop}]
;         [(shop-url shop) (shop-page shop)])
;       shops)))

; (def about-me
;   (layout
;     {:title "about me"
;      :children 
;      (seq
;        [[:h1 "here's a picture of me :3"]
;         [:img {:src "/public/bob.avif"}]])}))

; (def pages
;   (merge
;     shop-pages
;     {"about-me/index.html" about-me
;      "index.html" coffee-bob}))
