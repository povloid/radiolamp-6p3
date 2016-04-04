(ns ix.omut.component.modal-yes-no
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.component.modal :as modal]))


(def app-init modal/app-init)

(defn component [app owner {:keys [act-yes-fn]
                            :or   {act-yes-fn #(js/alert "Действие еще не реализовано")}
                            :as   opts}]
  (reify
    om/IRender
    (render [_]
      (om/build modal/component app
                {:opts (assoc opts :footer
                              (dom/div nil

                                       (c/ui-button {:type     :primary
                                                     :on-click (fn [_]
                                                                 (act-yes-fn)
                                                                 (modal/hide app) 1)
                                                     :text     "Да"})

                                       (c/ui-button {:on-click (fn [_] (modal/hide app) 1)
                                                     :text     "Нет"})

                                       ))
                 } ))))
