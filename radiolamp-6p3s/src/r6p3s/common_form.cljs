(ns r6p3s.common-form
  (:require [r6p3s.common-input :as common-input]
            [r6p3s.cpt.alert :as alert]
            [r6p3s.cpt.helper-p :as helper-p]))





(def label-class "col-xs-12 col-sm-4 col-md-3 col-lg-3")
(def input-class "col-xs-12 col-sm-8 col-md-9 col-lg-9")


(defn form-show-invalid-messages!! [app message & [input-app input-message]]
  (alert/clean-and-set!! app :alert-danger (or message "Ошибка ввода данных"))
  (when input-app
    (common-input/input-css-string-has?-clean-and-set!! input-app :has-error?)
    (helper-p/clean-and-set!!
     input-app :text-danger (or input-message "ошибка ввода"))))
