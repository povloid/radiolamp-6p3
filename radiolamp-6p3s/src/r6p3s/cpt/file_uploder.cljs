(ns r6p3s.cpt.file-uploder
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.io :as ix-io]
            [r6p3s.core :as c]
            [r6p3s.ui.glyphicon :as glyphicon]))


;;**************************************************************************************************
;;* BEGIN Uploader elements
;;* tag: <uploader>
;;*
;;* description: Элементы для выгрузки файлов
;;*
;;**************************************************************************************************

(defn component [_ own {:keys [uri
                               get-uri-fn
                               update-fn
                               success-fn
                               accept]
                        :or   {uri    "/file-uploder/uri"
                               accept "*.*"}}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-upload (chan)
       :form-id     (c/uniq-id "file-uploder-form")
       :uploader-id (c/uniq-id "uploder")
       :in-progress false})
    om/IWillMount
    (will-mount [this]
      (let [{:keys [chan-upload form-id uploader-id]} (om/get-state own)]
        (go
          (while true
            (let [_ (<! chan-upload)]

              (om/set-state! own :in-progress true)

              (ix-io/file-upload
               (.getElementById js/document form-id)
               (.getElementById js/document uploader-id)
               (if get-uri-fn (get-uri-fn) uri)
               {:success  success-fn
                :complete #(do
                             (when update-fn (update-fn))
                             (om/set-state! own :in-progress false))
                }))))))

    om/IRenderState
    (render-state [_ {:keys [chan-upload form-id uploader-id in-progress]}]
      (dom/form #js {:id      form-id
                     :encType "multipart/form-data"
                     :method  "POST"}
                (dom/span #js {:className "btn btn-default btn-file btn-primary"}
                          "Загрузить"
                          (dom/input #js {:id       uploader-id
                                          :name     "uploader"
                                          :type     "file"
                                          :multiple true
                                          :accept   accept
                                          :onChange #(put! chan-upload 1)
                                          }))
                (when in-progress
                  (dom/span #js {:className "text-warning"}
                            " " (glyphicon/render "flag") " Подождите, идет выгрузка файлов на сервер..."
                            (dom/img #js {:src "/images/uploading.gif"})))

                ))))

;; END Uploader elements
;;..................................................................................................
