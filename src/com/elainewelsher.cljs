(ns ^:figwheel-hooks com.elainewelsher
  (:require
   [goog.dom :as gdom]
   [rum.core :as rum]))

(def default-section
  {:img-src "images/balloon.png"
   :color   "#FFFFFF"})

(def sections
  [{:img-src "images/balloon-1.png"
    :color   "#C1CDD0"
    :title   "architecture"}
   {:img-src "images/balloon-2.png"
    :color   "#D0E0E4"
    :title   "objects"}
   {:img-src "images/balloon-3.png"
    :color   "#FEF3C2"
    :title   "drawings"}
   {:img-src "images/balloon-4.png"
    :color   "#FFCDD8"
    :title   "about"}
   {:img-src "images/balloon-5.png"
    :color   "#FFB0C2"
    :title   "contact"}])

(defonce app-state
  (atom {:section      default-section
         :next-section default-section}))

(defn page-coords
  [event]
  [(.-pageX event) (.-pageY event)])

(defn top-right-coords
  [[x y]]
  [(- x (.-innerWidth js/window)) y])

(defn rotated-coords
  [angle x y]
  [(+ (* x (Math/cos angle))
      (* y (Math/sin angle)))
   (- (* y (Math/cos angle))
      (* x (Math/sin angle)))])

(defn next-section
  [event]
  (let [angle (/ Math/PI 4)
        [x y] (->> event
                   page-coords
                   top-right-coords
                   (apply rotated-coords angle))]
    (if (and (< -50 x)
             (< x 100)
             (< 275 y)
             (< y 425))
      (get sections (Math/floor (/ (- y 275) 30)))
      default-section)))

#_(defonce mouse-move-callback
  (set! (.-onmousemove js/document)
        (fn [event]
          (swap! app-state
                 assoc
                 :next-section
                 (next-section event)))))

#_(defonce mouse-click-callback
  (set! (.-onclick js/document)
        (fn [_]
          (let [next-section (:next-section @app-state)]
            (when (not= default-section next-section)
              (.scrollTo js/window #js {:top      80 
                                        :behavior "smooth"})
              (swap! app-state #(assoc % :section next-section)))))))

(rum/defc header < rum/reactive []
  (println @app-state)
  (let [{:keys [next-section]} (rum/react app-state)]
    [:#header
     [:img#header-balloon
      {:src (:img-src next-section)}]
     [:#section-links
      
      (map-indexed (fn [idx section]
                     [:button {:on-mouse-over (fn [] (println idx))}
                      (:title section)])
                   sections)]
     [:#next-section-preview
      (for [section sections]
        [:h1 {:style {:visibility (if (not= default-section next-section)
                                    "visible"
                                    "hidden")
                      :opacity    (if (= section next-section)
                                    1.0
                                    0.1)}}
         (:title section)])]]))

(defn gradient-to-white
  [color]
  (str "linear-gradient(" color ", #FFFFFF)"))

(rum/defc content < rum/reactive []
  (let [{:keys [color title]} (:section (rum/react app-state))]
    [:#content
     [:.title-page #_{:style {:background-color color}}]
     [:h1.section-title
      #_{:style {:background-color color}}
      title]
     [:p "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."]]))

(rum/defc footer []
  [:#footer
   [:img#footer-hanging
    {:src "images/hanging.png"}]
   [:img#footer-swinging
    {:src "images/swinging.png"}]])

(rum/defc page []
  [:#page
   (header)
   #_(content)
   #_(footer)])

(defn mount [el]
  (rum/mount (page) el))

(defn app-element []
  (gdom/getElement "app"))

(defn mount-app-element []
  (some-> (app-element) mount))

(mount-app-element)
(defn ^:after-load on-reload []
  (mount-app-element))

