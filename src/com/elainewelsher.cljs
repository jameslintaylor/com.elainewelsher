(ns ^:figwheel-hooks com.elainewelsher
  (:require
   [com.elainewelsher.routing :as routing]
   [goog.dom :as gdom]
   [rum.core :as rum]))

(defn- set-hash
  "Set the hash portion of the url. This triggers a browser history
  event.

  See: configure-history-navigation"
  [hash]
  (set! (.. js/window -location -hash) hash))

(defonce app-state
  (atom {:projects-page {:post-layout           false
                         :focused-project-index nil}
         :project-page  {:focused-image-index nil}}))

(defn- display-year
  "Given a year, return a string suitable for displaying to the user."
  [year]
  (let [year-str   (str year)
        [_ last-2] (split-at (- (count year-str) 2) year-str)]
    (apply str "'" last-2)))

(rum/defc project-title [project display-year?]
  `[:.project-title-container
    [:.project-title
     ~@(when project
         [[:.project-name (:name project)]
          (when display-year?
            [:.project-year (-> project :year display-year)])])]])

(rum/defc project-thumbnail
  < {:key-fn (fn [_ index _] index)}
  [is-focused index thumbnail]
  [:.thumbnail {:style {:opacity (if is-focused 1 0.2)}}
   [:img {:src thumbnail}]])

(defn- on-project-hover [index]
  (when-not (get-in @app-state [:projects-page :post-layout])
    (swap! app-state assoc-in [:projects-page :focused-project-index] index)))

(defn- on-project-hover-out [index]
  (when-not (get-in @app-state [:projects-page :post-layout])
   (swap! app-state assoc-in [:projects-page :focused-project-index] nil)))

(defn- on-project-click [project]
  (swap! app-state assoc-in [:projects-page :post-layout] true)
  (js/setTimeout
   (fn []
     (set-hash (str "#projects/" (:id project)))
     (swap! app-state assoc-in [:projects-page :post-layout] false))
   400))

(rum/defc project-column
  < {:key-fn (fn [_ index _] index)}
  [focused-project-index index project]
  (let [is-focused (= index focused-project-index)]
    [:.project-column
     {:class          (when is-focused "focus")
      :on-mouse-enter (partial on-project-hover index)
      :on-mouse-leave (partial on-project-hover-out index)
      :on-click       (partial on-project-click project)}
     (map-indexed (partial project-thumbnail is-focused)
                  (map :thumbnail (:images project)))]))

(rum/defc projects-table [focused-project-index projects]
  [:.projects-table
   {:class (when focused-project-index "focus")}
   (map-indexed (partial project-column focused-project-index) projects)])

(rum/defc projects-page
  < rum/reactive
  [projects]
  (let [{:keys [focused-project-index
                post-layout]} (:projects-page (rum/react app-state))
        focused-project       (get projects focused-project-index)]
    [:#projects-page
     {:class (when post-layout "post-layout")}
     (project-title focused-project true)
     (projects-table focused-project-index projects)]))

(rum/defc back-button [target-hash]
  [:img.back-button
   {:src      "images/back-button.png"
    :on-click (fn [_] (set-hash ""))}])

(defn- on-image-click
  [event]
  (let [element      (.-target event)
        view-height  (.-innerHeight js/window)
        image-height (.-clientHeight element)
        top-offset   (/ (- view-height image-height) 2)
        top          (+ (.-top (.getBoundingClientRect element))
                        (.-pageYOffset js/window)
                        (- top-offset))]
    (.scrollTo js/window #js {:top top :behavior "smooth"})))

(rum/defc project-header [project]
  [:.project-header
   (back-button "")
   (project-title project false)])

(rum/defc project-image
  < {:key-fn (fn [_ index _] index)}
  [focused-image-index index image]
  (let [is-focused? (= index focused-image-index)]
    [:.project-image
     {:class      (when is-focused? "focus")
      :data-index index
      :on-click   on-image-click}
     [:img.project-image-full
      {:src (:full image)}]
     [:.project-image-title
      (:title image)]]))

(rum/defc project-images [focused-image-index images]
  [:.project-images
   (map-indexed (partial project-image focused-image-index) images)])

(defonce intersection-observer
  (js/IntersectionObserver.
   (fn [entries _]
     (goog.array/forEach
      entries
      (fn [entry _ _]
        (if (.-isIntersecting entry)
          (swap! app-state
                 assoc-in
                 [:project-page :focused-image-index] 
                 (int (.. entry -target -dataset -index)))
          (swap! app-state
                 assoc-in
                 [:project-page :focused-image-index]
                 nil)))))
   #js {:threshold 1}))

(defn- observe-project-images
  ([] (observe-project-images false))
  ([unobserve?]
   (goog.array/forEach
    (.getElementsByClassName
     js/document
     "project-image")
    (fn [el index _]
      (if unobserve?
        (.unobserve intersection-observer el)
        (.observe intersection-observer el))))))

(def ^:private observe-project-images-mixin
  {:did-mount    (fn [state]
                   (observe-project-images false)
                   state)
   :will-unmount (fn [state]
                   (observe-project-images true)
                   state)})

(rum/defc project-body
  < observe-project-images-mixin
  [focused-image-index project]
  [:.project-body
   [:.project-description
    [:.description-html
     {:dangerouslySetInnerHTML {:__html (:description project)}}]]
   (project-images focused-image-index (:images project))])

(rum/defcs project-page
  < rum/reactive
  < (rum/local true ::pre-layout)
  [state project]
  (let [pre-layout (::pre-layout state)

        {:keys [focused-image-index]}
        (:project-page (rum/react app-state))]
    (when @pre-layout
      (js/setTimeout #(reset! pre-layout false) 100))
    [:#project-page
     {:class (when @pre-layout "pre-layout")}
     (project-header project)
     (project-body focused-image-index project)]))

(defn- hash-component
  "Given a hash, returns the top level component for the website."
  [hash]
  (let [[route route-data] (routing/hash-route hash)]
    (case route
      :project  (project-page route-data)
      :projects (projects-page route-data))))

(rum/defc page []
  (let [hash (.. js/window -location -hash)]
    [:#page
     (hash-component hash)]))

(defn mount [el]
  (rum/mount (page) el))

(defn app-element []
  (gdom/getElement "app"))


(defn mount-app-element []
  (some-> (app-element) mount))

;; Mount on page load, figwheel reload hook
(mount-app-element)
(defn ^:after-load on-reload []
  (mount-app-element))

;; Re-mount on navigation
(defn- configure-history-navigation []
  (set! (.-onpopstate js/window)
        #(mount-app-element)))
(configure-history-navigation)
