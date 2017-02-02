(ns r6p3s.cpt.input-date-interval
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as rc]
            [r6p3s.common-form :as common-form]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.input-datetime :as input-datetime]
            [r6p3s.ui.button :as button]
            [r6p3s.ui.glyphicon :as glyphicon]))



(defn app-init-f [{:keys [default-days-interval]
                   :or {default-days-interval (* 365 3)}}]
  {:from-date (assoc input/app-init :value
                     (rc/format-date
                      "yyyy-MM-dd"
                      (-> (js/Date.)
                          .getTime
                          (- (* default-days-interval 24 60 60 1000))
                          (js/Date.))))
   :to-date   (assoc input/app-init :value (rc/format-date "yyyy-MM-dd" (new js/Date))) })






(defn component [app own
                 {:keys [selected-fn
                         class-name style]
                  :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-update (chan)})

    om/IWillMount
    (will-mount [this]
      (let [{:keys [chan-update]} (om/get-state own)]
        (println "OM: date-paginator -> will-mount")

        (go
          (while true
            (let [_ (<! chan-update)]
              (println "OM: date-paginator -> chan-update -> run! ")

              (when selected-fn
                (let [app-v     @app
                      from-date (-> app-v :from-date input/value rc/str-to-date)
                      to-date   (-> app-v :to-date   input/value rc/str-to-date)]

                  (when (and from-date to-date) ;; если оба распознанны как даты то тогда можем говорить что даты выбраны
                    (let [selected-interval [from-date to-date]]
                      (println "OM: date-paginator -> call selected-fn -> " selected-interval)
                      (selected-fn selected-interval))))))))))

    om/IRenderState
    (render-state [_ {:keys [chan-update]}]
      (let [app-v @app]
        (dom/div #js {:className class-name :style style}

                 (dom/div #js {:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                               :style     #js {:paddingRight 0 :paddingLeft 0}}
                          (dom/div #js {:className "col-xs-12 col-sm-6 col-md-6 col-lg-6"
                                        :style     #js {:paddingRight 0 :paddingLeft 0}}
                                   (om/build input/component-form-group (app :from-date)
                                             {:opts {:label       "с даты"
                                                     :label-style #js {:textAlign "right"}
                                                     :spec-input  {:type                      "date"
                                                                   :placeholder               "yyyy-MM-dd"
                                                                   :onChange-valid?-fn        common-input/vldfn-not-empty-date
                                                                   :onChange-updated-valid-fn #(put! chan-update 1)}}}))
                          (dom/div #js {:className "col-xs-12 col-sm-6 col-md-6 col-lg-6"
                                        :style     #js {:paddingRight 0 :paddingLeft 0}}
                                   (om/build input/component-form-group (app :to-date)
                                             {:opts {:label       "по дату"
                                                     :label-style #js {:textAlign "right"}
                                                     :spec-input  {:type                      "date"
                                                                   :placeholder               "yyyy-MM-dd"
                                                                   :onChange-valid?-fn        common-input/vldfn-not-empty-date
                                                                   :onChange-updated-valid-fn #(put! chan-update 1)}}})))

                 (when (or
                        (get-in app-v [:from-date :text-warning])
                        (get-in app-v [:to-date :text-warning]))
                   (dom/p #js {:className "text-info"
                               :style     #js {:textAlign "center"}}
                          (glyphicon/render "info-sign")
                          " Необходиме вводить в дату в формате yyyy-MM-dd, наример 2016-11-01")))))))
