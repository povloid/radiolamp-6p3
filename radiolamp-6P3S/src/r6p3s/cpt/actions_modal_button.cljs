(ns r6p3s.cpt.actions-modal-button
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.cpt.modal :as modal]
            [r6p3s.ui.button :as button]))




(defn component [app _ {:keys [text
                               btn-type
                               act-fn]
                        :or   {text     "Метка события"
                               btn-type :default}}]
  (reify
    om/IRender
    (render [_]
      (button/render {:text     text
                      :type     btn-type
                      :block?   true
                      :size     :lg
                      :on-click (fn [_]
                                  (modal/hide app)
                                  (if act-fn
                                    (act-fn)
                                    (println "Действие для '" text "' еще не определено"))
                                  1)
                      }))))
