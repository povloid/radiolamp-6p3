(ns r6p3s.ui.navbar-li
  (:require [om.dom :as dom :include-macros true]))

(defn render [{:keys [glyphicon glyphicon-class+ a-class+ text href]}]
  (dom/li nil
          (dom/a #js {:className a-class+ :href href}
                 (when glyphicon
                   (dom/span #js {:style       #js {:paddingRight 4}
                                  :className   (str "glyphicon " glyphicon " " glyphicon-class+)
                                  :aria-hidden "true"}))
                 text)))
