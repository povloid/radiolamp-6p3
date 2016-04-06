(ns r6p3s.cpt.modal-yes-no
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.cpt.modal :as modal]
            [r6p3s.ui.button :as button]))


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

                                       (button/render {:type     :primary
                                                       :on-click (fn [_]
                                                                   (act-yes-fn)
                                                                   (modal/hide app) 1)
                                                       :text     "Да"})

                                       (button/render {:on-click (fn [_] (modal/hide app) 1)
                                                       :text     "Нет"})

                                       ))
                 } ))))
