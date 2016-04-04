(ns ix.omut.ui.collapser
  (:require [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.ui.button :as button]))


                                        ;TODO: Перемиеноывать в ui-hidder !!!!!
(defn render [app text k show? collapsed-body]
  (dom/p #js {:style #js {:display (if show? "" "none")}}
         (button/render {:text     text
                         :type     :info
                         :active?  (not (c/omut-row-collapsed? @app k))
                         :on-click #(c/omut-row-set-collapsed-not!! app k)})
         (dom/div #js {:style #js {:display (if (c/omut-row-collapsed? @app k) "none" "")}}
                  collapsed-body)))
