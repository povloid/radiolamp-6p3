(ns r6p3s.io
  (:require [goog.net.IframeIo]
            [goog.net.EventType]
            [goog.events :as events]))


(enable-console-print!)


;; (defn file-upload [form-e uri & [{:keys [success error complete] :as map-e->fns}]]
;;     (let [io (goog.net.IframeIo.)]

;;       ;;(events/listen io (aget goog.net.EventType "SUCCESS") #(js/alert "SUCCESS!"))
;;       (events/listen io goog.net.EventType.SUCCESS  (or success  #(.log js/console "SUCCES!")))
;;       (events/listen io goog.net.EventType.ERROR    (or error    #(.log js/console "ERROR!")))
;;       (events/listen io goog.net.EventType.COMPLETE (or complete #(.log js/console "COMPLETE!")))
;;       (.setErrorChecker io #(not= "OK" (.getResponseText io)))

;;       (.sendFromForm io form-e uri true)

;;       ;;For simplicity, disable the input while uploading.
;;       ;; fileInput.disabled = true;
;;       (set! (.-disabled form-e) true)))







(defn file-upload [form-e form-e-uploader
                   uri & [{:keys [success error complete] :as map-e->fns}]]
  (let [io (goog.net.IframeIo.)]

    (set! (.-setTimeoutInterval io) 0)

    (events/listen io goog.net.EventType.SUCCESS
                   (fn [e]
                     (.log js/console "SUCCESS!")
                     (when success (success nil))))

    (events/listen io goog.net.EventType.ERROR
                   (fn [e]
                     (.log js/console "ERROR!")
                     (do (.log js/console "ERROR!!")
                         (js/alert (str  "Произошла ошибка. файлы небыли выгружены.  ERROR!: "
                                         (.getLastErrorCode io) " - "(.getLastError io) ))
                         (when error (error)))
                     (when error (error))))

    (events/listen io goog.net.EventType.COMPLETE
                   (fn [e]
                     (let [iframeIo (.-target e)
                           file     (.-value form-e-uploader)]

                       (if (.isSuccess io)
                         (do
                           (.log js/console "UPLOADING SUCCESS! --> " (.getResponseText io))
                           (when success (success (.getResponseText io))))

                         (do (.log js/console "ERROR!")
                             (js/alert (str  "Произошла ошибка. файлы небыли выгружены.  ERROR!!: "
                                             (.getLastErrorCode io) " - "(.getLastError io) ))
                             (when error (error))))

                       (.dispose iframeIo)

                       (.log js/console "UPLOADING COMPLETE FOR FILE: " file)
                       (when complete (complete)))))


    (.setErrorChecker io #(= nil (.getResponseText io)))

    (.sendFromForm io form-e uri true)

    ;;For simplicity, disable the input while uploading.
    ;; fileInput.disabled = true;
    (set! (.-disabled form-e) true)))
