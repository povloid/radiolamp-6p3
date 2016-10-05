(ns r6p3s.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [clojure.set :as clojset]
            [clojure.string :as clojstr]

            [goog.i18n.DateTimeFormat :as dtf]
            [goog.i18n.DateTimeParse :as dtp]

            [goog.string :as gstring]
            [goog.string.format])

  #_(:import [goog.dom query]))


(enable-console-print!)

;;**************************************************************************************************
;;* BEGIN Common functions and tools
;;* tag: <com>
;;*
;;* description: Общие функции и инструменты
;;*
;;**************************************************************************************************

(defn by-id
  "Short-hand for document.getElementById(id)"
  [id]
  (.getElementById js/document (name id)))

(defn uniq-id [s]
  (str (gensym (str s "-"))))


(defn is-numeric? [n]
  (and (not (js/isNaN (js/parseFloat n))) (js/isFinite n)))




(defn parse-int-or [v dv]
  (if (is-numeric? v)
    (js/parseInt v)
    dv))

(defn parse-int-or-nil [v]
  (parse-int-or v nil))

(defn parse-int-or-zero [v]
  (parse-int-or v 0))




(defn parse-float-or [v dv]
  (if (is-numeric? v)
    (js/parseFloat v)
    dv))

(defn parse-float-or-nil [v]
  (parse-float-or v nil))

(defn parse-float-or-zero [v]
  (parse-float-or v 0))




(defn parse-number-or [v dv]
  (if (is-numeric? v)
    (new js/Number v)
    dv))

(defn parse-number-or-nil [v]
  (parse-number-or v nil))

(defn parse-number-or-zero [v]
  (parse-number-or v 0))



(defn parse-boolean [v]
  (new js/Boolean v))







(defn on-click-com-fn [f]
  (fn [e]
    (.preventDefault e)
    (.stopPropagation e)
    (f)
    1))



;; Работа с фалами и расширениями

(defn file-ext [filename]
  (let [m (js->clj (.split filename "."))]
    (when (< 1 (count m)) (last m))))

(defn file-type [filename]
  (let [ext (file-ext filename)]
    (assoc (condp = ext
             nil   {:type :dir  :title "Папка"}
             "jpg" {:type :file :title "Фаил изображения"}
             {:type :file :title "Фаил"})
           :ext ext)))



(defn path-up [path]
  (let [p (clojure.string/split path "/")
        c (count p)]
    ;;(println p c)
    (if (< c 3) "/"
        (->> p
             reverse
             rest
             reverse
             (reduce (fn [a s]
                       (if (or (empty? s) (= s "/")) a
                           (str a "/" s)))
                     "")))))

;; END Common functions and tools
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN omut row spec
;;* tag: <omut row spec>
;;*
;;* description: Данные для отображения строк
;;*
;;**************************************************************************************************


(def omut-row-key :omut-row)

(def omut-row-app-state {:selected false :collapsed {}})


(defn omut-row-init [row]
  (assoc row omut-row-key omut-row-app-state))

(defn omut-row-if-not-init-init!! [app]
  (om/transact!
   app
   (fn [app]
     (if (omut-row-key app)
       app
       (omut-row-init app)))))

(defn omut-row-row [row]
  (dissoc row omut-row-key))



(defn omut-row-set-selected! [app-row v]
  (assoc-in app-row [omut-row-key :selected] v))

(defn omut-row-set-selected!! [app-row v]
  (om/update! app-row [omut-row-key :selected] v))

(defn omut-row-set-selected-not!! [app-row]
  (om/transact! app-row [omut-row-key :selected] not))

(defn omut-row-selected? [app-row]
  (get-in app-row [omut-row-key :selected] false))


(defn omut-row-set-collapsed!! [app-row k v]
  (om/transact!
   app-row
   (fn [app-row]
     (assoc-in app-row [omut-row-key :collapsed k] v))))

(defn omut-row-set-collapsed-not!! [app-row k]
  (om/transact!
   app-row
   (fn [app-row]
     ;;(println (app-row omut-row-key))
     (let [v (get-in app-row [omut-row-key :collapsed k])]
       (assoc-in app-row [omut-row-key :collapsed k] (if (nil? v) false (not v)))))))

(defn omut-row-collapsed? [app-row k]
  (get-in app-row [omut-row-key :collapsed k] true))



;; END omut-row
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN Dates and Times
;;* tag: <date time timestamp>
;;*
;;* description: функции работы со временем
;;*
;;**************************************************************************************************

;; (defn convert-to-array [js-col]
;;   (-> (clj->js [])
;;       (.-slice)
;;       (.call js-col)
;;       (js->clj)))

;;**************************************************************************************************
;;* BEGIN date and time functions
;;* tag: <date and time functions>
;;*
;;* description: Работа с датами и временем
;;* примеры
;; 1 форматирование
;;(.format (goog.i18n.DateTimeFormat. (.-LONG_DATE goog.i18n.DateTimeFormat.Format)) (js/Date.))
;; 2 получение из стороки и форматирование
;;(.format (goog.i18n.DateTimeFormat. (.-LONG_DATE goog.i18n.DateTimeFormat.Format))
;;  (new js/Date (.parse js/Date (get-in row [:webdoc-row :udate]))))
;;**************************************************************************************************

(def date-formats
  (let [f goog.i18n.DateTimeFormat.Format]
    {:FULL_DATE       (.-FULL_DATE f)
     :FULL_DATETIME   (.-FULL_DATETIME f)
     :FULL_TIME       (.-FULL_TIME f)
     :LONG_DATE       (.-LONG_DATE f)
     :LONG_DATETIME   (.-LONG_DATETIME f)
     :LONG_TIME       (.-LONG_TIME f)
     :MEDIUM_DATE     (.-MEDIUM_DATE f)
     :MEDIUM_DATETIME (.-MEDIUM_DATETIME f)
     :MEDIUM_TIME     (.-MEDIUM_TIME f)
     :SHORT_DATE      (.-SHORT_DATE f)
     :SHORT_DATETIME  (.-SHORT_DATETIME f)
     :SHORT_TIME      (.-SHORT_TIME f)
     }))


(defn str-to-date [date-string]
  (let [d (new js/Date (.parse js/Date date-string))]
    (if (js/isNaN d) nil d)))


(defn format-date
  "Format a date using either the built-in goog.i18n.DateTimeFormat.Format enum
  or a formatting string like \"dd MMMM yyyy\"
  examples:
  > (format-date-generic :LONG_DATE (js/Date.))
  > \"July 14, 2012\"
  > (format-date-generic \"dd MMMM yyyy\" (js/Date.))
  > \"14 July 2012\"
  > (format-date-generic \"MMMM\" (js/Date.))
  > \"July\"
  "
  [date-format date]
  (if (nil? date) nil
      (.format (goog.i18n.DateTimeFormat. (or (date-formats date-format) date-format))  date)))

(defn str-to-date-and-format [date-format alt-string s]
  (if (nil? s) alt-string
      (format-date date-format (str-to-date s))))


(defn date-com-format-date [d]
  (format-date "yyyy-MM-dd" d))

(defn date-com-format-datetime [d]
  (format-date "yyyy-MM-dd HH:mm:ss z" d))

(defn date-com-format-datetime-to-min [d]
  (format-date "yyyy-MM-dd HH:mm z" d))

(defn the-time-has-passed-from [date-1 date-2]
  (let [time-diff (.abs js/Math
                        (- (.getTime date-2)
                           (.getTime date-1)))

        diff-ds   (.floor js/Math
                          (/ time-diff
                             (* 1000 60 60 24)))

        diff-hs   (.floor js/Math
                          (/ time-diff
                             (* 1000 60 60)))

        diff-ms   (.floor js/Math
                          (/ time-diff
                             (* 1000 60)))]
    [
     ;; Дни
     diff-ds
     ;; Часы
     (- diff-hs (* diff-ds 24))
     ;; Минуты
     (- diff-ms (* diff-hs 60))
     ]))

(defn the-time-has-passed-from-the-date [date]
  (the-time-has-passed-from date (new js/Date)))

(defn the-time-has-passed-from-the-date-as-str [date]
  (let [[d h m] (the-time-has-passed-from-the-date date)]
    (gstring/format "прошло: %d дн. %02d час. %02d мин." d h m)))

(defn the-time-has-passed-from-the-date-to-date [from-date to-date]
  (when (and from-date to-date)
    (let [[d h m] (the-time-has-passed-from from-date to-date)]
      (str d " сут. " h " час. " m " мин. "))))


;; END date and time functions
;;..................................................................................................
;; END Dates and Times
;;..................................................................................................


(defn calc-percent [objects-all objects & [precision]]
  (.toFixed (/ objects (/ objects-all  100)) (or  precision 2)))

(defn calc-percent-as-str [objects-all objects & [precision]]
  (str (calc-percent objects-all objects precision) '%))
