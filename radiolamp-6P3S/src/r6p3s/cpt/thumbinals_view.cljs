(ns r6p3s.cpt.thumbinals-view
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [goog.dom.classes :as gdc]

            [r6p3s.io :as ix-io]
            [r6p3s.net :as rnet]
            [r6p3s.core :as c]

            [r6p3s.cpt.modal :as modal]
            [r6p3s.cpt.thumbinal :as thumbinal]
            [r6p3s.cpt.file-uploder :as file-uploder]
            [r6p3s.cpt.modal-yes-no :as modal-yes-no]
            [r6p3s.cpt.actions-modal :as actions-modal]
            [r6p3s.cpt.thumbinal-modal-edit-form :as thumbinal-modal-edit-form]
            [r6p3s.cpt.image-full-screen-viewer :as image-full-screen-viewer]

            [clojure.set :as clojset]
            [clojure.string :as clojstr]

            [goog.i18n.DateTimeFormat :as dtf]
            [goog.i18n.DateTimeParse :as dtp]

            [goog.string :as gstring]
            [goog.string.format]
            )

  (:import [goog.dom query]))





;;**************************************************************************************************
;;* BEGIN Thumbs
;;* tag: <thumbs>
;;*
;;* description: Функционал работы с тумбами и картинками
;;*
;;**************************************************************************************************




(def app-init
  {:list         []
   :last-params  {}
   :modal-act    actions-modal/app-init
   :modal-yes-no (assoc modal-yes-no/app-init :row {})
   :modal        thumbinal-modal-edit-form/app-init })





(defn component [app owner {:keys [uri params
                                   uri-upload
                                   uri-delete
                                   chan-update
                                   max-height]
                            :or   {params     {}
                                   max-height 500}}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-modal-act                              (chan)
       :chan-thumbnails-modal-edit-form-open-for-id (chan)})
    om/IWillMount
    (will-mount [this]
      (when chan-update
        (go
          (while true
            (let [cparams (<! chan-update)
                  p       (if (map? cparams) cparams params)]
              (rnet/get-data uri p
                             (fn [list]
                               (om/transact!
                                app #(assoc % :list list :last-params p) ))))))))
    om/IRenderState
    (render-state [_ {:keys [chan-modal-act
                             chan-thumbnails-modal-edit-form-open-for-id]}]
      (dom/div nil
               (om/build file-uploder/component app
                         {:opts {:accept "image/gif, image/jpeg, image/png, image/*"
                                 :get-uri-fn
                                 #(str
                                   uri-upload
                                   (get-in @app [:last-params :id])
                                   "/image")
                                 :update-fn
                                 #(put! chan-update (:last-params @app))
                                 }})
               (apply
                dom/div #js {:className "row"
                             :style     #js {:margin    5
                                             :maxHeight max-height
                                             :overflow  "auto"}}
                (map
                 (fn [{:as row}]
                   (om/build thumbinal/component row
                             {:opts {:onClick-fn
                                     (fn [{:keys [id] :as r}]
                                       (put! chan-modal-act
                                             {:label (str "Выбор действий над записью №" id)
                                              :acts
                                              [{:text "Просмотр" :btn-type :info
                                                :act-fn
                                                (fn []
                                                  (put! image-full-screen-viewer/chan-show r))
                                                }
                                               {:text "Редактировать" :btn-type :primary
                                                :act-fn
                                                (fn []
                                                  (put! chan-thumbnails-modal-edit-form-open-for-id id)
                                                  (modal/show (:modal app)))}
                                               {:text "Удалить" :btn-type :danger
                                                :act-fn
                                                #(do
                                                   (om/update! app [:modal-yes-no :row] r)
                                                   (modal/show (:modal-yes-no app)))}]
                                              }))}}))
                 (:list app)))

               (om/build actions-modal/component (:modal-act app) {:opts {:chan-open chan-modal-act}})

               (om/build modal-yes-no/component (:modal-yes-no app)
                         {:opts {:modal-size :sm
                                 :label      "Желаете удалить изображение?"
                                 :body
                                 (dom/div
                                  #js{:className "row"}
                                  (dom/img #js{:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                                               :src       (get-in @app [:modal-yes-no :row :path])}))
                                 :act-yes-fn
                                 (fn []
                                   (rnet/get-data
                                    uri-delete ;;"/tc/rb/product/files_rel/delete"
                                    {:id      (get-in @app [:last-params :id])
                                     :file-id (get-in @app [:modal-yes-no :row :id])}
                                    (fn [_]
                                      (when chan-update
                                        (put! chan-update (:last-params @app))))))
                                 }})

               (om/build thumbinal-modal-edit-form/component (:modal app)
                         {:opts {:chan-load-for-id chan-thumbnails-modal-edit-form-open-for-id
                                 :uri              "/files/find/transit"
                                 :uri-save         "/files/edit/transit"
                                 :post-save-fn     #(do
                                                      (when chan-update
                                                        (put! chan-update (:last-params @app)))
                                                      1)}})

               ))))

;; END Thumbs
;;..................................................................................................
