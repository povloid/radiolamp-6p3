(ns r6p3s.cpt.input-date-interval
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as rc]
            [r6p3s.common-form :as common-form]
            [r6p3s.cpt.input-date :as input-date]
            [r6p3s.ui.glyphicon :as glyphicon]))



(defn app-init-f [{:keys [default-days-interval]
                   :or   {default-days-interval (* 365 3)}}]
  (let [d (new js/Date)]
    {:from-date (->> (-> d
                         .getTime
                         (- (* default-days-interval 24 60 60 1000))
                         (js/Date.))
                     (input-date/set-date! input-date/app-init))
     :to-date   input-date/app-init}))






(defn component [app own
                 {:keys [selected-fn
                         class-name style]
                  :as   opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-update (chan)})

    om/IWillMount
    (will-mount [this]
      (let [{:keys [chan-update]} (om/get-state own)]
        (go
          (while true
            (let [_ (<! chan-update)]
              (when selected-fn
                (let [app-v     @app
                      from-date (-> app-v :from-date input-date/date)
                      to-date   (-> app-v :to-date   input-date/date)]

                  (when (and from-date to-date) ;; если оба распознанны как даты то тогда можем говорить что даты выбраны
                    (let [selected-interval [from-date to-date]]
                      (println "OM: date-paginator -> call selected-fn -> " selected-interval)
                      (selected-fn selected-interval))))))))))


    om/IRenderState
    (render-state [_ {:keys [chan-update]}]
      (let [app-v @app]
        (dom/div #js {:className class-name :style style}

                 (println '>>> @app)
                 
                 (dom/div #js {:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                               :style     #js {:paddingRight 0 :paddingLeft 0}}
                          (dom/div #js {:className "col-xs-12 col-sm-6 col-md-6 col-lg-6"
                                        :style     #js {:paddingRight 0 :paddingLeft 0}}
                                   (om/build input-date/component-form-group (app :from-date)
                                             {:opts {:label       "с даты"
                                                     :label-style #js {:textAlign "right"}
                                                     :spec-input  {:onChange-updated-valid-fn selected-fn}}}))
                          (dom/div #js {:className "col-xs-12 col-sm-6 col-md-6 col-lg-6"
                                        :style     #js {:paddingRight 0 :paddingLeft 0}}
                                   (om/build input-date/component-form-group (app :to-date)
                                             {:opts {:label       "по дату"
                                                     :label-style #js {:textAlign "right"}
                                                     :spec-input  {:onChange-updated-valid-fn selected-fn}}})))

                 (when (or
                        (get-in app-v [:from-date :text-warning])
                        (get-in app-v [:to-date :text-warning]))
                   (dom/p #js {:className "text-info"
                               :style     #js {:textAlign "center"}}
                          (glyphicon/render "info-sign")
                          " Необходиме вводить в дату в формате yyyy-MM-dd, наример 2016-11-01")))))))
