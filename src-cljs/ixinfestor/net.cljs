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

(def r (t/reader :json
                 {:handlers
                  {"f" (fn [v] (new js/Number v))
                   }}
                 ))

(def w (t/writer :json))
(def transit-header (clj->js {:content-type "application/transit+json"}))

(defn get-data [uri params success-fn & [error-fn complete-fn]]
  (let [req (goog.net.XhrIo.)]

    (events/listen req goog.net.EventType.ERROR
                   (fn [e]
                     (let [response-text (.getResponseText (.-target e))
                           response (t/read r response-text)]
                       (println "ERROR: " response)
                       (js/alert (str "ОШИБКА ОБРАЩЕНИЯ К СЕРВЕРУ:\n" response)) 
                       (if error-fn (error-fn response)))))

    (events/listen req goog.net.EventType.COMPLETE
                   (fn [e]
                     (let [response (t/read r (.getResponseText (.-target e)))]
                       (println "REQUEST COMPLETE")
                       (if complete-fn (complete-fn response)))))


    (events/listen req goog.net.EventType.SUCCESS
                   (fn [e]
                     (success-fn (t/read r (.getResponseText (.-target e))))))
    (.send req
           uri
           "POST"
           (t/write w (or params {}))
           transit-header)))
