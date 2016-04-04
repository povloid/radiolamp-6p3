(ns ix.omut.component.actions-modal-button
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.component.modal :as modal]))




(defn component [app _ {:keys [text
                               btn-type
                               act-fn]
                        :or   {text     "Метка события"
                               btn-type :default}}]
  (reify
    om/IRender
    (render [_]
      (c/ui-button {:text     text
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
