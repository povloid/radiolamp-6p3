(ns ixinfestor.net
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [cognitect.transit :as t]
            [cljs.reader :as reader]
            [goog.net.XhrIo]
            [goog.net.EventType]
            [goog.events :as events]
            )

  )


(enable-console-print!)

(def progress-element-id "net-in-progress")

(def r (t/reader :json
                 {:handlers
                  {"f" (fn [v] (new js/Number v))
                   }}))

(def w (t/writer :json))


(def transit-header (clj->js {:content-type "application/transit+json"}))
(def json-header    (clj->js {:content-type "application/json"}))





(defn w-transit [m]
  (t/write w m))

(defn r-transit [ee]
  (t/read r (.getResponseText ee)))




(defn w-json [m]
  (.stringify js/JSON (clj->js m)))

(defn r-json [ee]
  (js->clj (.getResponseJson ee) :keywordize-keys true))






(defn get-data

  ([uri params success-fn]
   (get-data :transit uri params success-fn nil nil nil nil))

  ([uri params success-fn error-fn]
   (get-data :transit uri params success-fn error-fn nil nil nil))

  ([uri params success-fn error-fn complete-fn]
   (get-data :transit uri params success-fn error-fn complete-fn nil nil))

  ([uri params success-fn error-fn complete-fn disable-progress-element?]
   (get-data :transit uri params success-fn error-fn complete-fn disable-progress-element? nil))

  ([uri params success-fn error-fn complete-fn disable-progress-element? my-error-alert-fn]
   (get-data :transit uri params success-fn error-fn complete-fn disable-progress-element? my-error-alert-fn))

  ([protocol uri params success-fn error-fn complete-fn disable-progress-element? my-error-alert-fn]
   (let [[hh rr ww] (condp = protocol
                      :transit [transit-header r-transit w-transit]
                      :json    [json-header    r-json    w-json])]
     (letfn [(get-response-as-transit-or-text [ee]
               (try
                 (rr ee)
                 (catch js/Error e
                   (.getResponseText ee))))

             (print-error [s ee show-alert]
               (println "ERROR IN " s "\n"
                        "LastErrorCode: " (.getLastErrorCode ee) "\n"
                        "Status: " (.getStatus ee) " - " (.getStatusText ee) "\n"
                        "Content-Type: " (.getResponseHeader ee "Content-Type") "\n"
                        "---------------------------------------------------------\n"
                        (get-response-as-transit-or-text ee)
                        "\n---------------------------------------------------------\n\n")
               (when (and show-alert  (not= 0 (.getStatus ee)))
                 (if my-error-alert-fn
                   (my-error-alert-fn (.getStatus ee) (.getStatusText ee))
                   (js/alert (str
                              "ERROR IN [" s "]\n"
                              "Status: " (.getStatus ee) " - " (.getStatusText ee) "\n"
                              (get-response-as-transit-or-text ee))))))

             ;; (redirect-to-root-when-not-transit [ee]
             ;;   (let [ct (.getResponseHeader ee "Content-Type")]
             ;;     (when-not (= ct "application/transit+json;charset=utf-8")
             ;;       (do (js/alert "ОТВЕТА СЕРВЕРА В ПЛОХОМ ФОРМАТЕ.")
             ;;           (set! (.-location js/window) "/")))))

             (make-reload-if-reload-client [r]
               (when (get r :reload-client)
                 (do (js/alert "СЕРВЕР ПРИСЛАЛ СИГНАЛ НА ОБНОВЛЕНИЕ")
                     (set! (.-location js/window) "/"))))]

       (let [req              (goog.net.XhrIo.)
             progress-element (if disable-progress-element?
                                false
                                (.getElementById js/document progress-element-id))]

         ;; Отобразить элемент прогресса выполнения
         (when progress-element
           (set! (-> progress-element .-style .-display) ""))


         (events/listen req goog.net.EventType.ERROR
                        (fn [e]
                          (print "REQUEST ERROR")
                          (let [ee (.-target e)]

                            (try
                              (make-reload-if-reload-client (rr ee))
                              (catch js/Error e
                                (println "ERR -1 " e)))

                            (print-error "ERROR" ee false)

                            (when error-fn
                              (try
                                (error-fn (rr ee))
                                (catch js/Error e
                                  (error-fn (.getResponseText ee))))))))

         (events/listen req goog.net.EventType.SUCCESS
                        (fn [e]
                          (println "REQUEST SUCCESS")
                          (let [ee (.-target e)]
                            ;;(redirect-to-root-when-not-transit ee)
                            (try
                              (let [r (rr ee)]
                                (make-reload-if-reload-client r)
                                (when success-fn (success-fn r)))
                              (catch js/Error e
                                (println (str "ERR 1 " e))
                                (print-error "ERRON IN REQUEST SUCCESS" ee false))))))

         (events/listen req goog.net.EventType.COMPLETE
                        (fn [e]
                          (let [ee (.-target e)]
                         ;;;(redirect-to-root-when-not-transit ee)
                            (when-not (.isSuccess ee)
                              (print-error "COMPLETE" ee true))

                            ;; Скрыть элемент прогресса выполнения
                            (when progress-element
                              (set! (-> progress-element .-style .-display) "none"))

                            (println "REQUEST COMPLETE")
                            (when complete-fn (complete-fn e)))))




         (.send req
                uri
                "POST"
                (ww (or params {}))
                hh))))))




(defn get-data-json

  ([uri params success-fn]
   (get-data :json uri params success-fn nil nil nil nil))

  ([uri params success-fn error-fn]
   (get-data :json uri params success-fn error-fn nil nil nil))

  ([uri params success-fn error-fn complete-fn]
   (get-data :json uri params success-fn error-fn complete-fn nil nil))

  ([uri params success-fn error-fn complete-fn disable-progress-element?]
   (get-data :json uri params success-fn error-fn complete-fn disable-progress-element? nil))

  ([uri params success-fn error-fn complete-fn disable-progress-element? my-error-alert-fn]
   (get-data :json uri params success-fn error-fn complete-fn disable-progress-element? my-error-alert-fn)))
