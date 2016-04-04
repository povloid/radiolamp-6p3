(ns ix.omut.component.input-datetime
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.component.common-input :as common-input]
            [ix.omut.component.input :as input]
            [ix.omut.component.helper-p :as helper-p]
            [goog.i18n.DateTimeFormat :as dtf]
            [goog.i18n.DateTimeParse :as dtp]))


(def date-str-format "yyyy-MM-ddTHH:mmZ")

(defn set-date! [app d]
  (assoc app :value (c/format-date date-str-format d)))

(defn parse-str-to-date [s]
  (let [parser (new goog.i18n.DateTimeParse date-str-format)
        d (new js/Date)]
    (.parse parser s d)
    d))

(defn date [app]
  (parse-str-to-date (app :value)))


(defn component-form-group  [app owner {:keys [label
                                               type
                                               label-class+
                                               input-class+
                                               spec-input]
                                        :or   {label        "Метка"
                                               label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                               input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                               spec-input   {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (dom/b nil "введено: "
                               (c/date-com-format-datetime-to-min (parse-str-to-date (@app :value))))
                        (om/build
                         input/component app
                         {:opts (assoc spec-input
                                       :type                "datetime"
                                       :placeholder         date-str-format
                                       :onChange-valid?-fn  common-input/vldfn-not-empty-datetime)})
                        (om/build helper-p/component app {}))))))
