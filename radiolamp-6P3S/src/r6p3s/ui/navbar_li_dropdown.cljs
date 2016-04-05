(ns r6p3s.ui.navbar-li-dropdown
  (:require [om.dom :as dom :include-macros true]))




(defn render [{:keys [glyphicon text]} & body]
  (dom/li #js {:className "dropdown"}
          (dom/a #js {:href          "#"
                      :className     "dropdown-toggle"
                      :data-toggle   "dropdown"
                      :role          "button"
                      :aria-haspopup "true"
                      :aria-expanded "false"}
                 (when glyphicon
                   (dom/span #js {:style       #js {:paddingRight 4}
                                  :className   (str "glyphicon " glyphicon)
                                  :aria-hidden "true"}))
                 text
                 (dom/span #js {:className "caret"}))
          (apply dom/ul #js {:className "dropdown-menu"} body)))
