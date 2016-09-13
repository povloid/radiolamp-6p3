(ns r6p3s.ui.head-params
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]))



(defn render [{:keys []} params]
  (->> params
       (map (fn [{:keys [name value]}]
              (dom/tr nil
                      (dom/td #js {:style #js {:textAlign    "right"
                                               :paddingRight 8}}
                              (dom/b nil name))
                      (dom/td nil value))))
       (apply dom/tbody nil)
       (dom/table nil)))
