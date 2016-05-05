(ns r6p3s.ui.progress
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]))





(defn- translate-into-percentages [max-value progress-bars]
  (let [max-value (or max-value (reduce (fn [a {:keys [value]}] (max a value)) 0.1 progress-bars))
        one-percent (/ max-value 100)]
    (map
     (fn [{value :value :as row}]
       (assoc row :percent (/ value one-percent)))
     progress-bars)))



(defn render [{:keys [max-value]} progress-bars]
  (->> progress-bars
       (translate-into-percentages max-value)
       (map (fn [{:keys [percent value title type] :or {title ""}}]
              (dom/div #js {:className (str "progress-bar "
                                            (condp = type
                                              :primary "progress-bar-primary"
                                              :success "progress-bar-success"
                                              :info    "progress-bar-info"
                                              :warning "progress-bar-warning"
                                              :danger  "progress-bar-danger"
                                              (name type)))
                            :style     #js {:width (str percent "%")}}
                       (str title " " value))))
       (apply dom/div #js {:className "progress"})))



