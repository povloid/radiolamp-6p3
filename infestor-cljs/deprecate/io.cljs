(ns ixinfestor.io
  (:require [ixinfestor.core :as ixs]
            [goog.net.IframeIo]
            [goog.net.EventType]
            [goog.events :as events]))



(defn file-upload [form-e uri & [{:keys [success error complete] :as map-e->fns}]]
  (let [io (goog.net.IframeIo.)]

    ;;(events/listen io (aget goog.net.EventType "SUCCESS") #(js/alert "SUCCESS!"))
    (events/listen io goog.net.EventType.SUCCESS  (or success  #(.log js/console "SUCCES!")))    
    (events/listen io goog.net.EventType.ERROR    (or error    #(.log js/console "ERROR!")))
    (events/listen io goog.net.EventType.COMPLETE (or complete #(.log js/console "COMPLETE!")))
    (.setErrorChecker io #(not= "OK" (.getResponseText io)))

    (.sendFromForm io form-e uri)))










