(ns ix.omut.component.thumbinals-view
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [goog.dom.classes :as gdc]

            [ix.io  :as ix-io]
            [ix.net :as ixnet]
            [ix.omut.core :as c]

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
   :modal-act    actions-modal-app-init
   :modal-yes-no (assoc modal-yes-no-app-init
                        :row {})
   :modal        thumbnails-modal-edit-form-app-init})

(defn view [app owner {:keys [uri params
                                         uri-upload
                                         uri-delete
                                         chan-update]
                                  :or   {params {}}}]
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
              (ixnet/get-data uri p
                              (fn [list]
                                (om/transact!
                                 app #(assoc % :list list :last-params p) ))))))))
    om/IRenderState
    (render-state [_ {:keys [chan-modal-act
                             chan-thumbnails-modal-edit-form-open-for-id]}]
      (dom/div nil
               (om/build file-uploder app
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
                             :style     #js {:margin 5}}
                (map
                 (fn [{:as row}]
                   (om/build thumbnail row
                             {:opts {:onClick-fn
                                     (fn [{:keys [id] :as r}]
                                       (put! chan-modal-act
                                             {:label (str "Выбор действий над записью №" id)
                                              :acts
                                              [{:text "Просмотр" :btn-type :info
                                                :act-fn
                                                (fn []
                                                  (put! chan-thumb-show-in-full-screen-app-init r))
                                                }
                                               {:text "Редактировать" :btn-type :primary
                                                :act-fn
                                                (fn []
                                                  (put! chan-thumbnails-modal-edit-form-open-for-id id)
                                                  (modal-show (:modal app)))}
                                               {:text "Удалить" :btn-type :danger
                                                :act-fn
                                                #(do
                                                   (om/update! app [:modal-yes-no :row] r)
                                                   (modal-show (:modal-yes-no app)))}]
                                              }))}}))
                 (:list app)))

               (om/build actions-modal (:modal-act app) {:opts {:chan-open chan-modal-act}})

               (om/build modal-yes-no (:modal-yes-no app)
                         {:opts {:modal-size :sm
                                 :label      "Желаете удалить изображение?"
                                 :body
                                 (dom/div
                                  #js{:className "row"}
                                  (dom/img #js{:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                                               :src       (get-in @app [:modal-yes-no :row :path])}))
                                 :act-yes-fn
                                 (fn []
                                   (ixnet/get-data
                                    uri-delete ;;"/tc/rb/product/files_rel/delete"
                                    {:id      (get-in @app [:last-params :id])
                                     :file-id (get-in @app [:modal-yes-no :row :id])}
                                    (fn [_]
                                      (when chan-update
                                        (put! chan-update (:last-params @app))))))
                                 }})

               (om/build thumbnails-modal-edit-form (:modal app)
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
