(ns r6p3s.cpt.select-datetime-interval
  #_(:require-macros [cljs.core.async.macros :refer [go]])
  (:require ;;[cljs.core.async :refer [put! chan <!]]
   [om.core :as om :include-macros true]
   [om.dom :as dom :include-macros true]
   [r6p3s.core :as c]
   [r6p3s.common-form :as common-form]
   [r6p3s.common-input :as common-input]

   [r6p3s.ui.button :as button]
   [r6p3s.ui.glyphicon :as glyphicon]
   [r6p3s.ui.form-group :as form-group]
   [r6p3s.ui.datetime :as datetime]

   [r6p3s.cpt.modal :as modal]
   [r6p3s.cpt.input-datetime :as input-datetime]
   [r6p3s.cpt.helper-p :as helper-p]
   [r6p3s.cpt.modal-yes-no :as modal-yes-no]))




(defn app-init-fn [from-date to-date]
  {:interval {:from-date from-date
              :to-date   to-date}
   :modal    (merge modal-yes-no/app-init
                    {:from-date input-datetime/app-init
                     :to-date   input-datetime/app-init})})



(defn get-selected [app]
  (:interval app))


(defn- add-time [date time]
  (-> date .getTime (+ time ) (js/Date.)))


(defn move-interval [time app]
  (-> app
      (update-in [:interval :from-date] add-time time)
      (update-in [:interval :to-date  ] add-time time)))


(defn move-to-now [app]
  (let [now (js/Date.)
        int (or (- (.getTime (get-in app [:interval :to-date  ] now))
                   (.getTime (get-in app [:interval :from-date] now)))
                (* 1000 60 60 30))]
    (-> app
        (assoc-in [:interval :from-date]
                  (-> now .getTime (- int) (js/Date.)))
        (assoc-in [:interval :to-date] now))))



(defn component
  [app own {:keys [on-selected-fn] :as opts}]
  (letfn [(on-selected-fn-2 []
            (when on-selected-fn (on-selected-fn)))]
    (reify
      om/IRender
      (render [_]
        (let [app-v                       @app
              {:keys [from-date to-date]} (get-selected app-v)
              app-modal                   (app :modal)]
          (dom/div #js {:className "well well-sm"
                        :style     #js {:backgroundColor "inherit"}}


                   (dom/div #js {:className "text-primary"}
                            (dom/div #js{:style #js {:width 20 :float "left"}} "от")
                            (datetime/render from-date)
                            (dom/br nil)
                            (dom/div #js{:style #js {:width 20 :float "left"}} "до")
                            (datetime/render to-date))
                   (dom/b nil "выбрано: ")
                   (c/the-time-has-passed-from-the-date-to-date from-date to-date)


                   (dom/div
                    #js {:className "btn-toolbar"}

                    (dom/div
                     #js {:className "btn-group"}
                     (button/render {:type     :default
                                     :text     (glyphicon/render "calendar")
                                     :title    "задать интервал времени"
                                     :on-click (fn []
                                                 (om/transact!
                                                  app-modal
                                                  (fn [app-modal]
                                                    (-> app-modal
                                                        (update-in [:from-date] input-datetime/set-date! from-date)
                                                        (update-in [:to-date]   input-datetime/set-date! to-date))))
                                                 (modal/show app-modal))})
                     (button/render {:type     :default
                                     :text     (glyphicon/render "time")
                                     :title    "на текущее время"
                                     :on-click (fn []
                                                 (om/transact! app move-to-now)
                                                 (on-selected-fn-2))}))

                    (dom/div
                     #js {:className "btn-group"}
                     (button/render {:type     :default
                                     :text     (glyphicon/render "fast-backward")
                                     :title    "сдвинуть интервал на неделю назад"
                                     :on-click (fn []
                                                 (om/transact! app (partial move-interval (* 1000 60 60 24 -7)))
                                                 (on-selected-fn-2))})
                     (button/render {:type     :default
                                     :text     (glyphicon/render "step-backward")
                                     :title    "сдвинуть интервал на день назад"
                                     :on-click (fn []
                                                 (om/transact! app (partial move-interval (* 1000 60 60 24 -1)))
                                                 (on-selected-fn-2))})
                     (button/render {:type     :default
                                     :text     (glyphicon/render "step-forward")
                                     :title    "сдвинуть интервал на день вперед"
                                     :on-click (fn []
                                                 (om/transact! app (partial move-interval (* 1000 60 60 24  1)))
                                                 (on-selected-fn-2))})
                     (button/render {:type     :default
                                     :text     (glyphicon/render "fast-forward")
                                     :title    "сдвинуть интервал на неделю вперед"
                                     :on-click (fn []
                                                 (om/transact! app (partial move-interval (* 1000 60 60 24  7)))
                                                 (on-selected-fn-2))})))


                   (om/build modal-yes-no/component app-modal
                             {:opts {:label (dom/span nil
                                                      (glyphicon/render "chevron-left")
                                                      (glyphicon/render "time")
                                                      (glyphicon/render "chevron-right")
                                                      " Указать интервал времени")

                                     
                                     :body
                                     (dom/div
                                      #js{:className "form-horizontal"}
                                      (println  (@app-modal :from-date))
                                      (om/build input-datetime/component-form-group (app-modal :from-date)
                                                {:opts {:label "От"}})
                                      (om/build input-datetime/component-form-group (app-modal :to-date)
                                                {:opts {:label "До "}})

                                      (form-group/render
                                       {:label "Длительность: "
                                        :body  (dom/h5 #js {:className "text-primary"}
                                                       (let [app-modal-v @app-modal]
                                                         (c/the-time-has-passed-from-the-date-to-date
                                                          (-> app-modal-v :from-date input-datetime/date)
                                                          (-> app-modal-v :to-date   input-datetime/date))))}))

                                     :act-yes-fn
                                     (fn []
                                       (let [app-modal-v @app-modal]
                                         (om/update!
                                          app :interval
                                          {:from-date (-> app-modal-v :from-date input-datetime/date)
                                           :to-date   (-> app-modal-v :to-date   input-datetime/date)})
                                         (modal/hide app-modal)
                                         (on-selected-fn-2)))}})))))))




(defn component-form-group
  [app _ {:keys [label
                 type
                 label-class+
                 input-class+
                 spec-input]
          :or   {label        "Метка"
                 label-class+ common-form/label-class
                 input-class+ common-form/input-class
                 spec-select  {}}}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-input})
                        (om/build helper-p/component app {}))))))
