(ns r6p3s.ui.progress-mini
  (:require [om.dom :as dom :include-macros true]))


(defn render [{:keys [width val color bg-color from-right? float]
               :or   {width    100
                      bg-color "#eee"
                      color    "blue"}}]
  (dom/div #js {:style #js {:width           width
                            :height          4
                            :backgroundColor bg-color
                            :float           float}}
           (dom/div #js {:style #js {:width           val
                                     :height          4
                                     :backgroundColor color
                                     :float           (when from-right? "right")}})))
