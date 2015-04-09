(ns ixinfestor.core-web

  (:use hiccup.core)
  (:use hiccup.page)
  (:use hiccup.form)
  (:use hiccup.element)
  (:use hiccup.util)

  (:require

   ;; !!!!!!!
   [hiccup-bridge.core :as hicv]

   [postal.core :as postal]

   [cemerick.friend :as friend]
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])

   [clj-time.core :as tco]
   [clj-time.format :as tf]
   [clj-time.coerce :as tc]
   [clj-time.local :as tl]

   ))

;;**************************************************************************************************
;;* BEGIN FORMATTERS
;;* tag: <formatters>
;;*
;;* description: Форматировщики данных
;;*
;;**************************************************************************************************

(def formatter-yyyy-MM-dd-HH:mm:ss (tf/formatter "yyyy-MM-dd HH:mm:ss"))

(def formatter-yyyy-MM-dd (tf/formatter "yyyy-MM-dd"))

(def formatter-HH:mm:ss (tf/formatter "HH:mm:ss"))


(def formatter-local-yyyy-MM-dd-HH:mm:ss (tf/formatter-local "yyyy-MM-dd HH:mm:ss"))

(def formatter-local-yyyy-MM-dd (tf/formatter-local "yyyy-MM-dd"))

(def formatter-local-HH:mm:ss (tf/formatter-local "HH:mm:ss"))

;; END FORMATTERS
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN Tools
;;* tag: <tools>
;;*
;;* description: полезные инструменты
;;*
;;**************************************************************************************************

(defn html->hiccup [& html]
  (->> html
       (reduce str)
       str
       rest
       reverse
       rest
       reverse
       (reduce str)
       (#(clojure.string/replace % #" >" ">"))
       (#(clojure.string/replace % #"> <" "><"))
       hiccup-bridge.core/html->hiccup
       first
       ((fn [x]
          (println)
          (clojure.pprint/pprint x)
          (println)
          x))
       doall))

;; END Tools
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN ID functions
;;* tag: <id>
;;*
;;* description: формирование идентификаторов
;;*
;;**************************************************************************************************

(defn- keyword-replace-char [kw c as-c]
  (->> kw name vec
       (map #(if (= % c) as-c %))
       (reduce str)
       keyword))

(defn xml-id [kw & parent]
  (if (empty? parent) (keyword-replace-char kw \- \_)
      (-> parent
          first
          name
          (str "--" (name kw))
          (keyword-replace-char \- \_))))


;; END ID functions
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN page regeneration
;;* tag: <page regeneration>
;;*
;;* description: механизм регенерации страниц
;;*
;;**************************************************************************************************

(def key-ix-request-parametr :ix)
(def key-ix-seq-id :ixseqid)
(def key-ix-store :ixstore)

(defn ix [request response-fn]
  (let [sc-id (get-in request [:params key-ix-request-parametr])]
    (if (or (nil? sc-id)
            (nil? (get-in request [:session key-ix-store sc-id])))
      ;; TRUE? - если скоп еще не создан
      (let [sc-id (-> request :session (key-ix-seq-id 0) inc)
            uri (request :uri)]
        (-> (hiccup.util/url uri {key-ix-request-parametr sc-id})
            str
            (ring.util.response/redirect)
            (update-in [:session] merge (request :session))
            (assoc-in [:session key-ix-seq-id] sc-id)
            (assoc-in [:session key-ix-store (str sc-id) uri] {})))
      ;; FALSE? - если скоп есть
      (let [params (request :params)
            restored-params (-> request
                                (get-in [:session key-ix-store sc-id (request :uri)])
                                (merge params))]
        (if (< 1 (count params))
          ;; если имеются дополнительные параметры
          ;; то надо их сохранить в хранилище и заново запросить в чистую
          (-> (hiccup.util/url (request :uri) {key-ix-request-parametr sc-id})
              str
              (ring.util.response/redirect)
              (assoc :session (request :session))
              (assoc-in [:session key-ix-store sc-id (request :uri)] restored-params))
          ;; иначе запрос чистый и выполняем функцию по запросу
          (-> request
              (assoc :params restored-params) ;; make restored request
              response-fn
              (assoc :session (request :session))
              (assoc-in [:session key-ix-store sc-id (request :uri)] restored-params)
              ))))))


(def key-ix-page-key-parametr :ix-page)

(defn ix-accoc-parametr [{{ix-id key-ix-request-parametr
                           ix-page key-ix-page-key-parametr
                           :as params} :params
                           session :session
                           :as request}]
  ;;(println ">>>1> " request)
  ;;(println ">>>2> " session)
  (-> "ok"
      (ring.util.response/response)
      (assoc :session (update-in session [key-ix-store ix-id ix-page]
                                 merge (dissoc params
                                               key-ix-request-parametr
                                               key-ix-page-key-parametr)))
      (ring.util.response/header "Content-Type" "text/html; charset=utf-8")))



;; END page regeneration
;;..................................................................................................

;;**************************************************************************************************
;;* BEGIN ACTUS FORM
;;* tag: <ix form>
;;*
;;* description: определение формы
;;*
;;**************************************************************************************************

(def key-act :act)
(def key-act-s (name key-act))

(def key-act-default :act-default)


(def key-acts :acts)
(def key-render :render)


(defn act-result      [key body] [key body])
(defn act-result-key  [[key  _]] key)
(defn act-result-body [[_ body]] body)

(defn do-act
  " Главная функция для отработки событий
  Параметры:

  1. request - запрос,колекция от ring

  2. данные
  :acts - карта событий и функция от запроса (fn [request] .....)
  Примеры acts:
  :action1 #(vec [nil (str %)])
  :action2 #(vec [:form %])
  :action3 #(vec [%])
  :action4 #(vec [:some-tag %])
  :action7 #(vector :redirect \"http://www.linux.org.ru\" %)
  :action8 #(vector :response \"some body\" %)

  :render - Функция рендер формы вида (fn [request] .....)"

  [{{act key-act} :params :as request} {acts key-acts render key-render}]
  (let [act-fn (if acts (-> act keyword acts) nil)]
    (if (nil? act-fn) (if (fn? render) ;; если обработка не задана то вызываем сразу отрисовку
                        (render request) ;; если есть функция отрисовки то выполняем ее
                        (html5 [:h1 "[1] Событие act: " (keyword act)] ;; иначе просто выводим страницу
                               [:br]
                               [:h2 "запрос:"]
                               [:br]
                               (str request)))
        (let [result (act-fn request)]
          (if (and (coll? result) (keyword? (first result)))
            ;; TRUE: Если условие проходит то считаем что это action
            (let [tag (act-result-key result)]
              (cond (= tag :form) (render result)  ; отрисовываем представление, код и тело передаем в
                                        ; в функцию рендеринга целиком

                                        ;ответ сразу без отрисовки
                    (= tag :response) (-> (ring.util.response/response (act-result-body result))
                                          (ring.util.response/charset "UTF-8"))

                                        ;перенаправление на другой ресурс
                    (= tag :redirect) (ring.util.response/redirect (act-result-body result))

                                        ;если ничего не найдено то выводим страницу с сообщением
                    :else (html5 [:h1 "[2] Событие act: " act]
                                 [:h1 "Не найден tag: "   tag]
                                 [:br]
                                 [:h2 "Результат:"]
                                 [:br]
                                 (str result))))

            ;; FALSE: Иначе выводим то что есть как строку
            (html5 [:h1 "[3] Событие act: " act]
                   [:h1 "Результат не имеет структуры вида " (str (act-result :tag :body)) ]
                   [:br]
                   [:h2 "Получено только:"]
                   [:br]
                   (str result)) )))))


(def key-a-form-state :astate)

(defn a-form-to [key-id method-action body]
  (form-to {:id (xml-id key-id)} method-action
           (hidden-field {} key-act key-act-default)
           body))

;; END ACTUS FORM
;;..................................................................................................


;;**************************************************************************************************
;;* BEGIN ajax
;;* tag: <ajax>
;;*
;;* description: AJAX
;;*
;;**************************************************************************************************


;; END ajax
;;..................................................................................................






;;**************************************************************************************************
;;* BEGIN handler tools
;;* tag: <handler tools>
;;*
;;* description: Инструменты для хэндлера
;;*
;;**************************************************************************************************

;;***********************************************************************
;;* BEGIN json errors
;;* tag: <json errors>
;;*
;;* description: Обертка для ошибок для JSON
;;*
;;***********************************************************************

(defmacro error-response-json [& body]
  `(try
     (do ~@body)
     (catch Exception ex#
       (do
         (clojure.stacktrace/print-stack-trace ex#)          
         (-> ex#
             .getMessage
             (ix/print-debug->>> "error-response-json")
             ring.util.response/response
             (ring.util.response/status 500)
             (ring.util.response/content-type "text/javascript ; charset=utf-8"))))))

;; END json errors
;;........................................................................



;; END handler tools
;;..................................................................................................



