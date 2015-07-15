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

(def r (t/reader :json))
(def w (t/writer :json))
(def transit-header (clj->js {:content-type "application/transit+json"}))

(defn get-data [uri params cb]
  (let [req (goog.net.XhrIo.)]

    (events/listen req goog.net.EventType.ERROR (fn [e] (println "error !!!")))
    (events/listen req goog.net.EventType.SUCCESS (fn [e] (println "OK")))
    (events/listen req goog.net.EventType.COMPLETE
                   (fn [e]
                     (cb (t/read r (.getResponseText (.-target e))))))
    (.send req
           uri
           "POST"
           (t/write w (or params {}))
           transit-header)))

