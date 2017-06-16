(ns r6p3s.cpt.modal-yes-no
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.cpt.modal :as modal]
            [r6p3s.ui.button :as button]))


(def app-init modal/app-init)

(defn component [app owner {:keys [act-yes-fn
                                   button-yes-text
                                   button-no-text]
                            :or   {act-yes-fn      #(js/alert "Действие еще не реализовано")
                                   button-yes-text "Да"
                                   button-no-text  "Нет"}
                            :as   opts}]
  (reify
    om/IRender
    (render [_]
      (om/build modal/component app
                {:opts (assoc opts :footer
                              (dom/div nil

                                       (button/render {:type     :primary
                                                       :on-click (fn [_]                               
                                                                   (modal/hide app)
                                                                   (act-yes-fn)
                                                                   1)
                                                       :text     button-yes-text})

                                       (button/render {:on-click (fn [_]
                                                                   (modal/hide app)
                                                                   1)
                                                       :text     button-no-text})

                                       ))
                 } ))))
