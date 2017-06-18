(ns r6p3s.ui.progress-mini
  (:require [om.dom :as dom :include-macros true]))


(defn render [{:keys [width height val color bg-color from-right? float]
               :or   {width    100
                      height   4
                      bg-color "#eee"
                      color    "blue"}}]
  (dom/div #js {:style #js {:width           width
                            :height          height
                            :backgroundColor bg-color
                            :float           float}}
           (dom/div #js {:style #js {:width           (if (and (< 0 val) (< val 1)) 1 val)
                                     :height          height
                                     :backgroundColor color
                                     :float           (when from-right? "right")}})))
