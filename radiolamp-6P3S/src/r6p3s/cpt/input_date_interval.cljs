(ns r6p3s.cpt.input-date-interval
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as rc]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.input-datetime :as input-datetime]
            [r6p3s.ui.button :as button]))



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






(defn component [app own {:keys [selected-fn style] :as opts}]
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
                (let [app-v             @app
                      selected-interval [(-> app-v :from-date input/value rc/str-to-date)
                                         (-> app-v :to-date   input/value rc/str-to-date)]]
                  (println "OM: date-paginator -> call selected-fn -> " selected-interval)
                  (selected-fn selected-interval))))))))

    om/IDidMount
    (did-mount [_]
      (let [{:keys []} (om/get-state own)]
        (println "OM: date-paginator -> did-mount")))

    om/IWillUnmount
    (will-unmount [_]
      (let [{:keys []} (om/get-state own)]
        (println "OM: date-paginator -> will-unmount")))

    
    om/IRenderState
    (render-state [_ {:keys [chan-update]}]
      (dom/div #js {:className "form-inline" :style style}

               (om/build input/component-form-group (app :from-date)
                         {:opts {:label      "с даты"
                                 :spec-input {:type                      "date" :placeholder "yyyy-MM-dd"
                                              :onChange-valid?-fn        common-input/vldfn-not-empty-date
                                              :onChange-updated-valid-fn #(put! chan-update 1)}}})

               (om/build input/component-form-group (app :to-date)
                         {:opts {:label      "по дату"
                                 :spec-input {:type                      "date" :placeholder "yyyy-MM-dd"
                                              :onChange-valid?-fn        common-input/vldfn-not-empty-date
                                              :onChange-updated-valid-fn #(put! chan-update 1)}}})

               (button/render {:text     "обновить"
                               :type     :primary
                               :on-click #(put! chan-update 1)})

               ))))
