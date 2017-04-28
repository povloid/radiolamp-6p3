(ns r6p3s.ui.group-box
  (:require [om.dom :as dom :include-macros true]))


(defn render [{:keys [class-name title body style style-div]}]
  (dom/div #js {:className class-name :style style}
           (dom/div #js {:style style-div}
                    (dom/div nil (dom/b nil title ":"))                    
                    (or body "нет"))))
