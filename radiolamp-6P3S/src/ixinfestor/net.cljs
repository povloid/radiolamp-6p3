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






(defn get-data [uri params success-fn & [error-fn complete-fn disable-progress-element?]]
  (letfn [(print-error [s ee show-alert]            
            (println "ERROR IN " s "\n"
                     "LastErrorCode: " (.getLastErrorCode ee) "\n"
                     "Status: " (.getStatus ee) " - " (.getStatusText ee) "\n"
                     "Content-Type: " (.getResponseHeader ee "Content-Type") "\n"
                     "---------------------------------------------------------\n"
                     (.getResponseText ee)
                     "\n---------------------------------------------------------\n\n")
            (when show-alert
              (js/alert (str
                         "ERROR IN [" s "]\n"
                         "Status: " (.getStatus ee) " - " (.getStatusText ee)))))

          ;; (redirect-to-root-when-not-transit [ee]
          ;;   (let [ct (.getResponseHeader ee "Content-Type")]
          ;;     (when-not (= ct "application/transit+json;charset=utf-8")
          ;;       (do (js/alert "ОТВЕТА СЕРВЕРА В ПЛОХОМ ФОРМАТЕ.")
          ;;           (set! (.-location js/window) "/")))))

          (make-reload-if-reload-client [r]
            (when (get r :reload-client)
              (do (js/alert "СЕРВЕР ПРИСЛАЛ СИГНАЛ НА ОБНОВЛЕНИЕ")
                (set! (.-location js/window) "/"))))]
    
    (let [req (goog.net.XhrIo.)
          progress-element (if disable-progress-element?
                             false
                             (.getElementById js/document progress-element-id))]

      ;; Отобразить элемент прогресса выполнения
      (when progress-element
        (set! (-> progress-element .-style .-display) ""))

      (events/listen req goog.net.EventType.ERROR
                     (fn [e]
                       (print "REQUEST ERROR: ")
                       (let [ee (.-target e)
                             rt (.getResponseText ee)]
                         (try
                           (let [response (t/read r rt)]
                             (make-reload-if-reload-client r)
                             (print-error "ERROR: " ee false)
                             ;;(js/alert (str "ОШИБКА ОБРАЩЕНИЯ К СЕРВЕРУ:\n" response))
                             (if error-fn (error-fn response)))                           
                           (catch js/Error e
                             (let [m (str "ERROR: " e)]                               
                               (print-error "ERRON IN ERROR:" ee true)
                               (js/alert (str  "ERRON IN ERROR: " rt))))))))

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

                         (let [])
                         
                         (if complete-fn (complete-fn e)))))


      (events/listen req goog.net.EventType.SUCCESS
                     (fn [e]
                       (println "REQUEST SUCCESS")
                       (let [ee (.-target e)]
                         ;;(redirect-to-root-when-not-transit ee)
                         (try
                           (let [r (t/read r (.getResponseText ee))]
                             (make-reload-if-reload-client r)
                             (success-fn r))                           
                           (catch js/Error e
                             (let [m (str "Ошибка сохранения: " e)]
                               (println m)
                               (print-error "ERRON IN REQUEST SUCCESS" ee false)))))))
      (.send req
             uri
             "POST"
             (t/write w (or params {}))
             transit-header))))
