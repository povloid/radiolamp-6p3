(ns r6p3s.cpt.data-paginator
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [r6p3s.core :as c]
            [r6p3s.cpt.paginator :as paginator]))


(def app-init
  {:data      []
   :data-view []
   :paginator paginator/app-init})


(defn- step [{:keys [data paginator] :as app}]
  (let [{:keys [page page-size]} paginator
        start-i                  (* (dec page) page-size)
        end-i                    (+ start-i page-size)]
    (->> data
         (map-indexed (fn [i row] [i row]))
         (filter (fn [[i _]] (and (<= start-i i) (< i end-i))))
         (map second)
         vec
         (assoc app :data-view))))


(defn set-data [app data & [{:keys [not-clean-pagination?]}]]
  (-> app
      (assoc :data data)
      (as-> app
          (if not-clean-pagination?
            app (assoc app :paginator paginator/app-init)))
      (assoc-in [:paginator :count-all] (count data))
      step))



(defn component [app own {:keys [] :as opts}]
  (reify
    om/IRenderState
    (render-state [_ {:keys []}]
      (om/build
       paginator/component (app :paginator)
       {:opts {:on-click-fn #(om/transact! app step)}}))))
