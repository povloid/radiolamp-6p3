(ns r6p3s.omut-webusers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.net :as rnet]
            [r6p3s.core :as rc]

            [r6p3s.ui.table :as table]
            [r6p3s.ui.thead-tr :as thead-tr]
            [r6p3s.ui.nav-tab :as nav-tab]

            [r6p3s.cpt.edit-form-for-id :as edit-form-for-id]
            [r6p3s.cpt.modal-edit-form-for-id--yes-no :as modal-edit-form-for-id--yes-no]


            ))

;;**************************************************************************************************
;;* BEGIN Webusers
;;* tag: <webusers webusers>
;;*
;;* description: Работа с пользователями системы
;;*
;;**************************************************************************************************


;;------------------------------------------------------------------------------
;; BEGIN: Webusers edit form
;; tag: <webusers edit form>
;; description: Форма редактирования пользователя системы
;;------------------------------------------------------------------------------

(def webusers-edit-form-app-init
  (merge edit-form-for-id/app-init
         {:username    (assoc rc/input-app-init :has-warning? true)
          :password    rc/input-change-password-app-init
          :description rc/textarea-app-init
          :troles      []
          :tabs        rc/nav-tabs-app-state
          }))



(defn webusers-edit-form [app _ {{:keys [app->row-fn row->app-fn
                                         nav-tabs-items-map
                                         abody-fn]
                                  :or   {nav-tabs-items-map {0 {:text "Логин"}
                                                             1   {:text "Роли"}}}} :specific
                                 :as                                           opts}]

  (reify
    om/IWillMount
    (will-mount [_]
      (om/update! app :tabs
                  (rc/nav-tabs-app-state-init nav-tabs-items-map)))
    om/IRender
    (render [_]
      (om/build
       edit-form-for-id/component app
       {:opts
        (merge opts
               {:uri      "/tc/rb/webusers/find/transit"
                :fill-app-fn
                (fn [row]
                  (om/transact!
                   app
                   (fn [app]
                     (-> app
                         ;; Заполнение формы
                         ;;(assoc :id id) ;; Id идет сразу, будет работатьт автоматом
                         (assoc-in [:username :value]    (get-in row [:username] ""))
                         (assoc-in [:description :value] (get-in row [:description] ""))
                         (update-in [:password] rc/input-change-password-clean)
                         (assoc :troles (let [[roles-list user-roles-set] (row :troles-set)
                                              roles-list                  (seq (group-by :id_2 roles-list))]
                                          {:groups (reduce
                                                    (fn [a [id_2 r]]
                                                      (assoc a id_2 (-> r first :title_2)))
                                                    {} roles-list)
                                           :roles  (->> roles-list
                                                        (reduce
                                                         (fn [a [g-id groups]]
                                                           (assoc a g-id
                                                                  (vec
                                                                   (map
                                                                    #(assoc-in % [:user-role? :value] (contains? user-roles-set (% :keyname)))
                                                                    groups))))
                                                         {}))}
                                          ))

                         ;; SPECIFIC
                         (as-> app
                             (if row->app-fn
                               (row->app-fn row app) app))

                         ))))


                :uri-save "/tc/rb/webusers/save/transit"
                :app-to-row-fn
                (fn []

                  (rc/input-change-password-check (@app :password))

                  {:row                 (-> (if-let [id (@app :id)] {:id id} {})
                                            (assoc
                                             :username      (get-in @app [:username :value])
                                             :description   (get-in @app [:description :value])
                                             :password (rc/input-change-password-value (@app :password)) )
                                            ;; SPECIFIC
                                            (as-> row
                                                (if app->row-fn
                                                  (app->row-fn @app row) row)))
                   :user-roles-keys-set (->> @app
                                             :troles
                                             :roles
                                             vals
                                             (mapcat conj)
                                             (filter #(get-in % [:user-role? :value]))
                                             (map :keyname)
                                             (reduce conj #{}))
                   })

                :form-body
                (apply
                 dom/div nil

                 (reduce
                  conj
                  [
                   (om/build rc/nav-tabs (app :tabs)
                             {:opts {}})

                   (nav-tab/render
                    (app :tabs) 0
                    (dom/fieldset
                     nil
                     (dom/legend nil "Основные данные")

                     (om/build rc/input-form-group (get-in app [:username])
                               {:opts {:label      "Наименование"
                                       :spec-input {:onChange-valid?-fn
                                                    rc/input-vldfn-not-empty}}})


                     (om/build rc/input-change-password-group (get-in app [:password]))

                     (om/build rc/textarea-form-group (get-in app [:description])
                               {:opts {:label "Описание"}})

                     ))

                   (nav-tab/render
                    (app :tabs) 1
                    (dom/fieldset
                     nil
                     (dom/legend nil "Роли пользователя")
                     (apply dom/div #js {:className "col-sm-12 col-md-12 col-lg-12"}

                            (let [roles  (get-in @app [:troles :roles])
                                  groups (get-in @app [:troles :groups])]
                              (map
                               (fn [[g-id title]]
                                 (dom/div
                                  nil (dom/hr nil) (dom/h5 nil title)
                                  (->> g-id
                                       roles
                                       (map
                                        (fn [i {:keys [title]}]
                                          (dom/div nil
                                                   (om/build rc/toggle-button (get-in app [:troles :roles g-id i :user-role?]))
                                                   title))
                                        (range))
                                       (apply dom/div nil)) ))
                               (seq groups)) ))))
                   ]

                  (if abody-fn (abody-fn app) []))


                 )


                })
        }))))


(def webusers-modal-edit-form-app-init
  (merge modal-edit-form-for-id--yes-no/app-init webusers-edit-form-app-init))

(defn webusers-modal-edit-form [app _ opts]
  (reify
    om/IRender
    (render [_]
      (om/build modal-edit-form-for-id--yes-no/component app
                {:opts (assoc opts :edit-form-for-id webusers-edit-form)}))))





;; END Webusers edit form
;;..............................................................................

;;------------------------------------------------------------------------------
;; BEGIN: Webusers search form
;; tag: <webusers search view form>
;; description: Форма поиска владельцев счетов
;;------------------------------------------------------------------------------

(def webusers-search-view-app-init
  (merge
   rc/search-view-app-init
   {:modal-add    webusers-modal-edit-form-app-init
    :modal-act    rc/actions-modal-app-init
    :modal-yes-no (assoc rc/modal-yes-no-app-init :row {})
    }))

(defn webusers-search-view [app own {:keys [selection-type
                                            webusers-edit-form-specific
                                            header app-to-tds-seq-fn
                                            editable?
                                            show-add-button-fn?
                                            get-spec-query-params-fn]
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
               (om/build rc/search-view app
                         {:opts
                          {:chan-update chan-update
                           :data-update-fn
                           (fn [app]
                             (rnet/get-data "/tc/rb/webusers/list/transit"
                                            (let [q {:fts-query (get-in @app [:fts-query :value])
                                                     :page      (get-in @app [:page])}]
                                              (if get-spec-query-params-fn
                                                (merge q (get-spec-query-params-fn app))
                                                q))
                                            (fn [response]
                                              (om/update! app :data (vec response)))))
                           :data-rendering-fn
                           (fn [app-2]
                             (table/render {:hover?      true
                                            :bordered?   true
                                            :striped?    true
                                            :responsive? true
                                            :thead       (thead-tr/render
                                                          (or header
                                                              [(dom/th nil "№")
                                                               (dom/th nil "Наименование")
                                                               (dom/th nil "Описание")]))
                                            :tbody
                                            (om/build rc/tbody-trs-sel (:data app-2)
                                                      {:opts {:selection-type selection-type
                                                              :app-to-tds-seq-fn
                                                              (or app-to-tds-seq-fn
                                                                  (fn [{:keys [id username description]}]
                                                                    [(dom/td nil id)
                                                                     (dom/td nil username)
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
                                                                                     (rc/modal-show (:modal-add app)))}
                                                                          {:text   "Удалить" :btn-type :danger
                                                                           :act-fn #(do
                                                                                      (om/update! app [:modal-yes-no :row] row)
                                                                                      (rc/modal-show (:modal-yes-no app)))}]
                                                                         }))
                                                                )
                                                              }})
                                            }))
                           :add-button-fn
                           (when show-add-button-fn?
                             #(do (rc/modal-show (:modal-add app))
                                  (put! chan-modal-add-id 0)))
                           }})

               (when editable?
                 (om/build rc/actions-modal (:modal-act app) {:opts {:chan-open chan-modal-act}}))

               (om/build webusers-modal-edit-form (:modal-add app)
                         {:opts {:specific         webusers-edit-form-specific
                                 :chan-load-for-id chan-modal-add-id
                                 :post-save-fn     #(do
                                                      (when chan-update
                                                        (put! chan-update 1))
                                                      1)
                                 }})

               (when editable?
                 (om/build rc/modal-yes-no (:modal-yes-no app)
                           {:opts {:modal-size :sm
                                   :label      "Желаете удалить запись?"
                                   :body
                                   (dom/div
                                    #js{:className "row"}
                                    (dom/h3 #js{:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"}
                                            (get-in @app [:modal-yes-no :row :username])))
                                   :act-yes-fn
                                   (fn []
                                     (rnet/get-data
                                      "/tc/rb/webusers/delete/transit"
                                      {:id (get-in @app [:modal-yes-no :row :id])}
                                      (fn [_]
                                        (when chan-update
                                          (put! chan-update 1)))))
                                   }}))

               ))))


;; END Webusers search form
;;..............................................................................

;;**************************************************************************************************
;;* BEGIN webuser change password form
;;* tag: <webuser change password form>
;;*
;;* description: Форма для изменения собственного пароля
;;*
;;**************************************************************************************************


(def webusers-change-password-form-app-init
  (merge edit-form-for-id/app-init
         {:password rc/input-change-password-app-init}))


(defn webusers-change-password-form [app _ _]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-save (chan)})
    om/IRenderState
    (render-state [_ {:keys [chan-save]}]
      (om/build
       edit-form-for-id/component app
       {:opts
        {:uri-save  "/tc/rb/webusers/change-password/transit"
         :chan-save chan-save
         :app-to-row-fn
         (fn []
           (rc/input-change-password-check (@app :password))
           {:password (rc/input-change-password-value (@app :password))})
         :form-body
         (dom/fieldset
          nil
          (dom/legend nil "Сменить пароль пользователя")
          (om/build rc/input-change-password-group (get-in app [:password]))

          (dom/button #js {:className "btn btn-primary"
                           :type      "button"
                           :onClick   (fn [_]
                                        (put! chan-save 1)
                                        1)
                           }
                      "Сохранить пароль")
          )
         }}))))

;; END webuser change password form
;;..................................................................................................
;; END Webusers
;;..................................................................................................
