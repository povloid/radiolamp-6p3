(ns r6p3s.ui.th-title
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.ui.font-icon :as font-icon]))


(defn render [{:keys [colspan icon title style class-name
                      rigth-part]}]
  (dom/th #js {:className class-name
               :colSpan   colspan
               :style     style}
          (dom/div #js {:className "th-title-left"
                        :style #js {:float "left"}}
                   (when icon
                     (dom/span #js
                               {:className "th-title-left-icon"}
                               (font-icon/render icon)))
                   (when icon " ")
                   title)
          (when rigth-part
            (dom/div #js {:className "th-title-right"
                          :style #js {:float "right"}}
                     rigth-part))))
