(ns r6p3s.ui.group-box
  (:require [om.dom :as dom :include-macros true]))


(defn render [{:keys [class-name title body]}]
  (dom/div #js {:className class-name}
           (dom/div nil 
                    (dom/div nil (dom/b nil title ":"))                    
                    (or body "нет"))))
