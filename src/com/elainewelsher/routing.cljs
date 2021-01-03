(ns com.elainewelsher.routing
  "Routing utilities for elainewelsher.com"
  (:require
   [clojure.string :as string]
   [com.elainewelsher.content :as content]))

(defn- hash-project
  "Given a hash, return the specific project it points to, else nil."
  [projects hash]
  (let [[_ project-id] (string/split hash #"#projects/")]
    (when project-id
      (some #(when (= project-id (:id %)) %) projects))))

(defn hash-route
  "Given a hash, return a tagged tuple representing the route or nil if
  the hash does not resolve to a valid route."
  [hash]
  (if-let [project (hash-project content/projects hash)]
    [:project project]
    [:projects content/projects]))
