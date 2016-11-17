(ns r6p3s.ui.progress-mini
  (:require [om.dom :as dom :include-macros true]))


(defn render [{:keys [width val color bg-color]
               :or   {width 100                    
                      color "blue"}}]
  (dom/div #js {:style #js {:width  width
                            :height 4
                            :backgroundColor bg-color}}
           (dom/div #js {:style #js {:width           val
                                     :height          4
                                     :backgroundColor color}})))
