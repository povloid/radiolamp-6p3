(ns r6p3s.cpt.file-view
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.io :as ix-io]
            [r6p3s.net :as rnet]
            [r6p3s.core :as c]
            [r6p3s.cpt.file :as file]
            [r6p3s.cpt.file-modal-edit-form :as file-modal-edit-form]
            [r6p3s.cpt.modal :as modal]
            [r6p3s.cpt.actions-modal :as actions-modal]
            [r6p3s.cpt.modal-yes-no :as modal-yes-no]
            [r6p3s.cpt.file_uploder :as file-uploder]))



(def app-init
  {:list         []
   :last-params  {}
   :modal-act    actions-modal/app-init
   :modal-yes-no (assoc modal-yes-no/app-init
                        :row {})
   :modal        file-modal-edit-form/app-init})

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
      {:chan-modal-act                         (chan)
       :chan-files-modal-edit-form-open-for-id (chan)})
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
                             chan-files-modal-edit-form-open-for-id]}]
      (dom/div nil
               (om/build file-uploder/component app
                         {:opts {:get-uri-fn
                                 #(str
                                   uri-upload
                                   (get-in @app [:last-params :id])
                                   "/file")
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
                   (om/build file/component row
                             {:opts {:onClick-fn
                                     (fn [{:keys [id] :as r}]
                                       (put! chan-modal-act
                                             {:label (str "Выбор действий над записью №" id)
                                              :acts
                                              [{:text   "Редактировать" :btn-type :primary
                                                :act-fn (fn []
                                                          (put! chan-files-modal-edit-form-open-for-id id)
                                                          (modal/show (:modal app)))}
                                               {:text   "Удалить" :btn-type :danger
                                                :act-fn #(do
                                                           (om/update! app [:modal-yes-no :row] r)
                                                           (modal/show (:modal-yes-no app)))}]
                                              }))}}))
                 (:list app)))

               (om/build actions-modal/component (:modal-act app) {:opts {:chan-open chan-modal-act}})

               (om/build modal-yes-no/component (:modal-yes-no app)
                         {:opts {:modal-size :sm
                                 :label      "Желаете удалить Фаил?"
                                 :body
                                 (dom/h4 nil (get-in @app [:modal-yes-no :row :filename]))
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

               (om/build file-modal-edit-form/component (:modal app)
                         {:opts {:chan-load-for-id chan-files-modal-edit-form-open-for-id
                                 :uri              "/files/find/transit"
                                 :uri-save         "/files/edit/transit"
                                 :post-save-fn     #(do
                                                      (when chan-update
                                                        (put! chan-update (:last-params @app)))
                                                      1)}})

               ))))
