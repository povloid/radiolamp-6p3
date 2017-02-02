(ns r6p3s.cpt.input-datetime
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.common-form :as common-form]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.helper-p :as helper-p]
            [goog.i18n.DateTimeFormat :as dtf]
            [goog.i18n.DateTimeParse :as dtp]))


(def date-str-format "yyyy-MM-ddTHH:mmZ")

(defn set-date! [app d]
  (assoc app :value (c/format-date date-str-format (or d (new js/Date)))))


(defn parse-str-to-date [s]
  (let [parser (new goog.i18n.DateTimeParse date-str-format)
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
                        (dom/b nil "введено: "
                               (c/date-com-format-datetime-to-min (parse-str-to-date (@app :value))))
                        (om/build
                         input/component app
                         {:opts (assoc spec-input
                                       :type                "datetime"
                                       :placeholder         date-str-format
                                       :onChange-valid?-fn  vldfn-not-empty-datetime)})
                        (om/build helper-p/component app {}))))))
