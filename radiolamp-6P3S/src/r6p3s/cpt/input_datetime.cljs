(ns r6p3s.cpt.input-datetime
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.common-form :as common-form]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.helper-p :as helper-p]
            [goog.string :as gstring]
            [goog.string.format]
            [goog.i18n.DateTimeFormat :as dtf]
            [goog.i18n.DateTimeParse :as dtp]))


(def date-str-format     "yyyy-MM-dd")
(def datetime-str-format "yyyy-MM-ddTHH:mmZ")


(defn parse-str-to-date [s]
  (let [parser (new goog.i18n.DateTimeParse datetime-str-format)
        d (new js/Date)]
    (.parse parser s d)
    d))


(defn vldfn-not-empty-datetime [app v]
  (helper-p/clean app)
  (common-input/input-css-string-has?-clean app)
  (when-not (parse-str-to-date v)
    (om/transact! app #(assoc % :has-warning? true :text-warning "Неправильная дата")))
  true)

(defn date [app]
  (parse-str-to-date (app :value)))






(def app-init
  {:value          ""
   :calendar-value ""})

(defn set-value! [app value]
  (assoc app :value value :calendar-value (subs value 0 10)))

(defn set-date! [app d]
  (let [d (or d (new js/Date))]
    (assoc app
           :value          (c/format-date datetime-str-format d)
           :calendar-value (c/format-date date-str-format     d))))

(defn value [app] (get app :value))

(defn component
  [app owner {:keys [class+
                     type
                     onChange-valid?-fn
                     onChange-updated-valid-fn
                     onChange-updated-fn
                     onKeyPress-fn
                     onKeyDown-fn
                     onKeyUp-fn
                     placeholder
                     readonly?
                     min max step]
              :or   {class+             ""
                     type               "text"
                     onChange-valid?-fn (fn [_ _] true)
                     ;;onKeyPress-fn      (fn [_] nil)
                     ;;onKeyUp-fn         (fn [_] nil)
                     placeholder        ""
                     }}]
  (reify
    om/IRender
    (render [this]
      (let [value (or (:value @app) "")]
        (dom/div #js {:className ""}
                 (println @app)

                 (dom/div #js {:className "col-xs-12 col-sm-6 col-md-6 col-lg-6"
                               :style     #js {:paddingRight 6
                                               :paddingLeft  0}}
                          (dom/input #js {:value    (@app :calendar-value)
                                          :onChange (fn [e]
                                                      (let [new-value (.. e -target -value)]
                                                        (when (onChange-valid?-fn app new-value)
                                                          (when onChange-updated-valid-fn
                                                            (onChange-updated-valid-fn)))

                                                        (when (= (count new-value)
                                                                 (count date-str-format))
                                                          (om/transact!
                                                           app :value
                                                           (fn [old-value]
                                                             (str new-value (subs old-value 10)))))

                                                        (om/update! app :calendar-value new-value)

                                                        (when onChange-updated-fn
                                                          (onChange-updated-fn))))
                                          :onKeyPress  onKeyPress-fn
                                          :onKeyDown   onKeyDown-fn
                                          :onKeyUp     onKeyUp-fn
                                          :style       #js {:marginBottom 4}
                                          :type        "date"                                          
                                          :placeholder date-str-format
                                          :disabled    (@app :disabled?)
                                          :className   "form-control"}))


                 
                 (dom/div #js {:className "col-xs-6 col-sm-3 col-md-3 col-lg-3"
                               :style     #js {:paddingRight 2
                                               :paddingLeft  0}}
                          (->> (range 24)
                               (map (fn [i]
                                      (let [is (gstring/format "%02d" i)]
                                        (dom/option #js {:value is} is))))
                               (apply dom/select
                                      #js {:value     (subs value 11 13)
                                           :className "form-control"
                                           :disabled  (@app :disabled?)
                                           :style     #js {:marginBottom 4}
                                           :onChange
                                           (fn [e]
                                             (let [new-value (-> e .-target .-value)]
                                               (om/transact!
                                                app :value
                                                (fn [old-value]
                                                  (str (subs old-value 0 11) new-value (subs old-value 13))))
                                               (when onChange-updated-fn
                                                 (onChange-updated-fn))))})))


                 (dom/div #js {:className "col-xs-6 col-sm-3 col-md-3 col-lg-3"
                               :style     #js {:paddingRight 6
                                               :paddingLeft  2}}
                          (->> (range 60)
                               (map (fn [i]
                                      (let [is (gstring/format "%02d" i)]
                                        (dom/option #js {:value is} is))))
                               (apply dom/select
                                      #js {:value     (subs value 14 16)
                                           :className "form-control"
                                           :disabled  (@app :disabled?)
                                           :style     #js {:marginBottom 4}
                                           :onChange
                                           (fn [e]
                                             (let [new-value (-> e .-target .-value)]
                                               (om/transact!
                                                app :value
                                                (fn [old-value]
                                                  (str (subs old-value 0 14) new-value (subs old-value 16))))
                                               (when onChange-updated-fn
                                                 (onChange-updated-fn))))})))





                 )))))







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
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+)
                               :style     label-style} label)
               (dom/div #js {:className input-class+ :style #js {}}                        
                        (om/build component app {:opts spec-input})
                        (dom/b nil "значение: "
                               (c/date-com-format-datetime-to-min (parse-str-to-date (@app :value))))
                        (om/build helper-p/component app {}))))))
