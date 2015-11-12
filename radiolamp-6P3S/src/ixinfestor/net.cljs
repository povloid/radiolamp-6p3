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
  (letfn [(print-error [s ee]
            (println "ERROR IN " s "\n"
                     "LastErrorCode: " (.getLastErrorCode ee) "\n"
                     "Status: " (.getStatus ee) " - " (.getStatusText ee) "\n"
                     "Content-Type: " (.getResponseHeader ee "Content-Type") "\n"
                     "---------------------------------------------------------\n"
                     (.getResponseText ee)
                     "\n---------------------------------------------------------\n\n"))

          (if-not-transit-redirect-to-root [ee]
            (let [ct (.getResponseHeader ee "Content-Type")]
              (when-not (= ct "application/transit+json;charset=utf-8")
                (do (js/alert "приложение было обновлено, надо зайти снова.")
                  (set! (.-location js/window) "/")))))]
    
    (let [req (goog.net.XhrIo.)
          progress-element (if disable-progress-element?
                             false
                             (.getElementById js/document progress-element-id))]

      ;; Отобразить элемент прогресса выполнения
      (when progress-element
        (set! (-> progress-element .-style .-display) ""))

      (events/listen req goog.net.EventType.ERROR
                     (fn [e]
                       (print "ERROR: ")
                       (let [response-text (.getResponseText (.-target e))
                             response (t/read r response-text)]
                         (println  response)
                         (js/alert (str "ОШИБКА ОБРАЩЕНИЯ К СЕРВЕРУ:\n" response))
                         (if error-fn (error-fn response)))))

      (events/listen req goog.net.EventType.COMPLETE
                     (fn [e]
                       (let [ee (.-target e)]
                         (if-not-transit-redirect-to-root ee)
                         (when-not (.isSuccess ee)
                           (print-error "COMPLETE" ee))

                         ;; Отобразить элемент прогресса выполнения
                         (when progress-element
                           (set! (-> progress-element .-style .-display) "none"))

                         (println "REQUEST COMPLETE")
                         (if complete-fn (complete-fn (t/read r (.getResponseText ee)))))))


      (events/listen req goog.net.EventType.SUCCESS
                     (fn [e]
                       (let [ee (.-target e)]
                         (if-not-transit-redirect-to-root ee)
                         (try
                           (success-fn (t/read r (.getResponseText ee)))
                           (catch js/Error e
                             (let [m (str "Ошибка сохранения: " e)]
                               (println m)
                               (print-error "SUCCESS" ee)))))))
      (.send req
             uri
             "POST"
             (t/write w (or params {}))
             transit-header))))
