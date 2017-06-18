(ns r6p3s.ui.collapser
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.core :as rc]
            [r6p3s.ui.button :as button]))


                                        ;TODO: Перемиеноывать в ui-hidder !!!!!
(defn render [app text k show? collapsed-body]
  (dom/div #js {:className "ui-collapser"
                :style #js {:display (if show? "" "none")}}
           (button/render {:text     text
                           :type     :info
                           :active?  (not (rc/omut-row-collapsed? @app k))
                           :on-click #(rc/omut-row-set-collapsed-not!! app k)})
           (dom/div #js {:style #js {:display (if (rc/omut-row-collapsed? @app k) "none" "")}}
                  collapsed-body)))
