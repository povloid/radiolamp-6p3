(ns r6p3s.cpt.input-datetime
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.common-form :as common-form]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.input-date :as input-date]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.helper-p :as helper-p]
            [goog.string :as gstring]
            [goog.string.format]
            [goog.i18n.DateTimeFormat :as dtf]
            [goog.i18n.DateTimeParse :as dtp]))



(def datetime-str-format "yyyy-MM-ddTHH:mmZ")

(defn- parse-str-to-date [s]
  (let [parser (new goog.i18n.DateTimeParse datetime-str-format)
        d (new js/Date)]
    (.parse parser s d)
    d))



(def app-init
  (let [d (new js/Date)]
    {:date input-date/app-init
     :hh   0
     :mm   0}))

(defn date [{:keys [date hh mm]}]
  (let [d (input-date/date date)]
    (.setHours d hh)
    (.setMinutes d mm)
    d))

(defn set-date! [app d]
  (-> app
      (assoc :hh (.getHours d)
             :mm (.getMinutes d))
      (update-in [:date] input-date/set-date! d)))


(defn component
  [app owner {:keys [class+
                     onChange-updated-valid-fn
                     onChange-updated-fn
                     onKeyPress-fn
                     onKeyDown-fn
                     onKeyUp-fn]
              :or   {class+ ""}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                    :style     #js {:padding 0}}

               (dom/div #js {:className "col-xs-12 col-sm-6 col-md-6 col-lg-6"
                             :style   #js {:paddingRight 6
                                           :paddingLeft  0}}
                          (om/build input-date/component (app :date)))



               (dom/div #js {:className "col-xs-6 col-sm-3 col-md-3 col-lg-3"
                             :style   #js {:paddingRight 2
                                           :paddingLeft  0}}
                          (->> (range 24)
                               (map (fn [i]
                                      (let [is (gstring/format "%02d" i)]
                                        (dom/option #js {:value i} (str is " чac.")))))
                               (apply dom/select
                                      #js {:value     (@app :hh)
                                           :className "form-control"
                                           :disabled  (@app :disabled?)
                                           :style     #js {:marginBottom 4}
                                           :onChange
                                           (fn [e]
                                             (let [v (-> e .-target .-value)]
                                               (om/update! app :hh (js/parseInt v))
                                               (when onChange-updated-fn
                                                 (onChange-updated-fn))))})))


               (dom/div #js {:className "col-xs-6 col-sm-3 col-md-3 col-lg-3"
                             :style   #js {:paddingRight 6
                                           :paddingLeft  2}}
                          (->> (range 60)
                               (map (fn [i]
                                      (let [is (gstring/format "%02d" i)]
                                        (dom/option #js {:value i} (str is " мин.")))))
                               (apply dom/select
                                      #js {:value     (@app :mm)
                                           :className "form-control"
                                           :disabled  (@app :disabled?)
                                           :style     #js {:marginBottom 4}
                                           :onChange
                                           (fn [e]
                                             (let [v (-> e .-target .-value)]
                                               (om/update! app :mm (js/parseInt v))
                                               (when onChange-updated-fn
                                                 (onChange-updated-fn))))})))

               ))))







(defn component-form-group  [app owner {:keys [label
                                               type
                                               label-class+

                                               label-style
                                               input-class+
                                               spec-input]
                                        :or   {label        "Метка"
                                               label-class+ common-form/label-class
                                               input-class+ common-form/input-class
                                               spec-input   {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app)
                                    " " (common-input/input-css-string-has? (app :date)))}
               (dom/label #js {:className (str "control-label " label-class+)
                               :style     label-style} label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-input})
                        (dom/b nil "значение: "
                               (c/date-com-format-datetime-to-min (date @app)))
                        (om/build helper-p/component (app :date) {})
                        (om/build helper-p/component app {}))))))
