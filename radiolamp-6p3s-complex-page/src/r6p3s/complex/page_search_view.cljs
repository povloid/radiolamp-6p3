(ns r6p3s.complex.page-search-view
  ;;(:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.net :as rnet]
            [r6p3s.core :as rc]
            [r6p3s.cpt.modal :as modal]
            [r6p3s.cpt.modal-yes-no :as modal-yes-no]
            [r6p3s.cpt.actions-modal :as actions-modal]
            [r6p3s.cpt.search-view :as search-view]
            [r6p3s.ui.table :as table]
            [r6p3s.ui.thead-tr :as thead-tr]
            [r6p3s.cpt.tbody-trs-sel :as tbody-trs-sel]

            [r6p3s.complex.page-modal-edit-form :as modal-edit-form]))



(def app-init
  (merge
   search-view/app-init
   {:modal-add modal-edit-form/app-init
    :modal-act actions-modal/app-init
    :modal-yes-no (assoc modal-yes-no/app-init :row {})
    }))

(defn component [app _ {:keys [meta-page uri selection-type editable?]
                        :or   {selection-type :one}}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-update       (chan)
       :chan-modal-act    (chan)
       :chan-modal-add-id (chan)})
    om/IRenderState
    (render-state [_ {:keys [chan-modal-act
                             chan-modal-add-id
                             chan-update]}]
      (dom/div nil
               (om/build search-view/component app
                         {:opts
                          {:chan-update chan-update
                           :data-update-fn
                           (fn [app]
                             (rnet/get-data (str uri "/page/list")
                                            {:fts-query (get-in @app [:fts-query :value])
                                             :page      (get-in @app [:page])
                                             :arls      (get-in @app [:arls] #{})}
                                            (fn [response]
                                              (om/update! app :data (vec response)))))
                           :data-rendering-fn
                           (fn [app-2]
                             (table/render {:hover?      true
                                            :bordered?   true
                                            :striped?    true
                                            :responsive? true
                                            :thead       (thead-tr/render [(dom/th nil "№")
                                                                           (dom/th nil "Наименование")
                                                                           (dom/th nil "Описание")])
                                            :tbody
                                            (om/build tbody-trs-sel/component  (:data app-2)
                                                      {:opts {:selection-type selection-type
                                                              :app-to-tds-seq-fn
                                                              (fn [row]
                                                                (let [{:keys [id keyname description]} @row]
                                                                  [(dom/td nil id)
                                                                   (dom/td nil keyname)
                                                                   (dom/td #js{:style #js {:whiteSpace "normal"}}
                                                                           description)]))
                                                              :on-select-fn
                                                              (fn [{:keys [id] :as row}]
                                                                (when editable?
                                                                  (put! chan-modal-act
                                                                        {:label (str "Выбор действий над записью №" id)
                                                                         :acts
                                                                         [{:text   "Редактировать" :btn-type :primary
                                                                           :act-fn (fn []
                                                                                     (put! chan-modal-add-id id)
                                                                                     (modal/show (:modal-add app)))}
                                                                          {:text   "Удалить" :btn-type :danger
                                                                           :act-fn #(do
                                                                                      (om/update! app [:modal-yes-no :row] row)
                                                                                      (modal/show (:modal-yes-no app)))}]
                                                                         }))
                                                                )
                                                              }})
                                            }))
                           :add-button-fn
                           #(do (modal/show (:modal-add app))
                                (put! chan-modal-add-id 0)
                                )
                           }})

               (when editable?
                 (om/build actions-modal/component (:modal-act app) {:opts {:chan-open chan-modal-act}}))

               (om/build modal-edit-form/component (:modal-add app)
                         {:opts {:label            "Работа с записью"
                                 :uri              uri
                                 :meta-page        meta-page
                                 :chan-load-for-id chan-modal-add-id
                                 :post-save-fn     #(do
                                                      (when chan-update
                                                        (put! chan-update 1))
                                                      1)
                                 }})

               (when editable?
                 (om/build modal-yes-no/component (:modal-yes-no app)
                           {:opts {:modal-size :sm
                                   :label      "Желаете удалить запись?"
                                   :body
                                   (dom/div
                                    #js{:className "row"}
                                    (dom/h3 #js{:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"}
                                            (get-in @app [:modal-yes-no :row :keyname])))
                                   :act-yes-fn
                                   (fn []
                                     (rnet/get-data
                                      (str uri "/page/delete")
                                      {:id (get-in @app [:modal-yes-no :row :id])}
                                      (fn [_]
                                        (when chan-update
                                          (put! chan-update 1)))))
                                   }}))

               ))))
