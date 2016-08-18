(ns r6p3s.ui.navbar-li-dropdown
  (:require [om.dom :as dom :include-macros true]))




(defn render [{:keys [glyphicon glyphicon-class+ text a-class+
                      style style-max-height style-overflow]} & body]
  (dom/li #js {:className "dropdown"}
          (dom/a #js {:href          "#"
                      :className     (str "dropdown-toggle " a-class+)
                      :data-toggle   "dropdown"
                      :role          "button"
                      :aria-haspopup "true"
                      :aria-expanded "false"}
                 (when glyphicon
                   (dom/span #js {:className   (str "glyphicon " glyphicon " " glyphicon-class+)
                                  :aria-hidden "true"}))
                 (str " " text)
                 (dom/span #js {:className "caret"}))
          (apply dom/ul #js {:className "dropdown-menu"
                             :style     (or style
                                            #js {:maxHeight (or style-max-height 300)
                                                 :overflow  (or style-overflow "auto")})}
                 body)))
