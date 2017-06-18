(ns r6p3s.common-input
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [goog.dom.classes :as gdc]

            [r6p3s.io :as ix-io]
            [r6p3s.net :as rnet]
            [r6p3s.core :as c]

            [clojure.set :as clojset]
            [clojure.string :as clojstr]

            [r6p3s.cpt.helper-p :as helper-p]

            [goog.i18n.DateTimeFormat :as dtf]
            [goog.i18n.DateTimeParse :as dtp]

            [goog.string :as gstring]
            [goog.string.format])

  (:import [goog.dom query]))




;;**************************************************************************************************
;;* BEGIN has
;;* tag: <has>
;;*
;;* description: Подсветка элементов
;;*
;;**************************************************************************************************

(def input-css-string-has?-app-init
  {:has-success? nil
   :has-warning? nil
   :has-error?   nil})


(defn input-css-string-has? [{:keys [has-success?
                                     has-warning?
                                     has-error?]}]
  (condp = true
    has-error?   "has-error"
    has-warning? "has-warning"
    has-success? "has-success"
    ""))

(defn input-css-string-has?-clean [app]
  (om/transact! app
                (fn [app]
                  (dissoc app
                          :has-success?
                          :has-warning?
                          :has-error?))))

(defn input-css-string-has?-clean-and-set! [app k]
  (-> app
      (dissoc :has-success?
              :has-warning?
              :has-error?)
      (assoc k true)))

(defn input-css-string-has?-clean-and-set!! [app k]
  (om/transact! app
                (fn [app]
                  (input-css-string-has?-clean-and-set! app k))))

;; END has
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN Throw validators
;;* tag: <throw validators>
;;*
;;* description: Валидаторы с исключениями и подсветкой
;;*
;;**************************************************************************************************

(defn get-valid-value-or-throw
  [app path get-val-fn pred? throw-message]
  (let [v (get-val-fn (get-in @app path))]
    (if (pred? v)
      (do (om/transact! app path #(assoc % :has-danger? true :text-danger throw-message))
          (throw (js/Error. throw-message)))
      v)))

;; END
;;..................................................................................................




;;**************************************************************************************************
;;* BEGIN input
;;* tag: <input>
;;*
;;* description: поле ввода
;;*
;;**************************************************************************************************


(defn vldfn-not-empty [app v]
  (helper-p/clean app)
  (input-css-string-has?-clean app)
  
  (if (= (count (.trim v)) 0)
    (do
      (om/transact! app #(assoc % :has-error? true :text-danger "Пустое поле"))
      false)
    (do
      (om/transact! app #(assoc % :has-success? true))
      true)))


(defn vldfn-not-empty-or-0 [app v]
  (helper-p/clean app)
  (input-css-string-has?-clean app)
  
  (if (or (= (count (.trim v)) 0) (= (.valueOf (new js/Number v)) 0))
    (do
      (om/transact! app #(assoc % :has-error? true :text-danger "Показание пустое либо равно нулю"))
      false)    
    (do
      (om/transact! app #(assoc % :has-success? true))
      true)))


(defn vldfn-not-empty-date [app v]
  (helper-p/clean app)
  (input-css-string-has?-clean app)
  
  (if (c/str-to-date v)
    (do
      (om/transact! app #(assoc % :has-error? true))
      true)
    (do
      (om/transact! app #(assoc % :has-danger? true :text-danger "Неправильная дата"))
      false)))


;;------------------------------------------------------------------------------
;; BEGIN: for some-> validations
;; tag: <some validations>
;; description: Валидация для some->
;;------------------------------------------------------------------------------



(defn validate-field-and-value-to-row-fn!
  ([get-cursor-keyword get-value-fn vld-fn]
   (validate-field-and-value-to-row-fn! get-cursor-keyword get-value-fn vld-fn get-cursor-keyword))
  ([get-cursor-fn get-value-fn vld-fn as-keyword]
   (fn [app]
     (let [app (get-cursor-fn app)
           v   (get-value-fn @app)]
       {:valid? (vld-fn app v)
        :key    as-keyword
        :value  v}))))

(defn value-to-row-fn!
  ([get-cursor-keyword get-value-fn]
   (value-to-row-fn! get-cursor-keyword get-value-fn get-cursor-keyword))
  ([get-cursor-fn get-value-fn as-keyword]
   (validate-field-and-value-to-row-fn! get-cursor-fn get-value-fn (fn [_ _] true) as-keyword)))

(defn validate-fields!
  [app & validate-fields]
  (doall (map #(% app) validate-fields)))

(defn every-valid?-then-row [validate-form-result]
  (when (every? :valid? validate-form-result)
    (reduce
     (fn [a {:keys [key value]}]
       (assoc a key value))
     {} validate-form-result)))

;; END for some-> validations
;;..............................................................................






;; END input
;;..................................................................................................
