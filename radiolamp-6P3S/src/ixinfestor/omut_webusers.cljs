(ns ixinfestor.omut-webusers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ixinfestor.net :as ixnet]
            [ixinfestor.omut :as omut]

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
  (merge omut/edit-form-for-id-app-init
         {:username (assoc omut/input-app-init :has-warning? true)
          :password omut/input-change-password-app-init
          :description omut/textarea-app-init
          :troles []
          :tabs omut/nav-tabs-app-state
          }))



(defn webusers-edit-form [app _ {{:keys [app->row-fn row->app-fn
                                         nav-tabs-items-map
                                         abody]
                                  :or {nav-tabs-items-map {0 {:text "Логин"}
                                                           1 {:text "Роли"}}}} :specific
                                 :as opts}]
  (reify
      om/IWillMount
    (will-mount [_]
      (om/update! app :tabs
                  (omut/nav-tabs-app-state-init nav-tabs-items-map)))
    om/IRender
    (render [_]
      (om/build
       omut/edit-form-for-id app
       {:opts
        (merge opts
               {:uri "/tc/rb/webusers/find/transit"
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
                         (update-in [:password] omut/input-change-password-clean)
                         (assoc :troles (let [[roles-list user-roles-set] (row :troles-set)
                                              roles-list (seq (group-by :id_2 roles-list))]
                                          {:groups (reduce
                                                    (fn [a [id_2 r]]
                                                      (assoc a id_2 (-> r first :title_2)))
                                                    {} roles-list)
                                           :roles (->> roles-list
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

                  (omut/input-change-password-check (@app :password))

                  {:row (-> (if-let [id (@app :id)] {:id id} {})
                            (assoc
                             :username      (get-in @app [:username :value])
                             :description   (get-in @app [:description :value])
                             :password (omut/input-change-password-value (@app :password)) )
                            ;; SPECIFIC
                            (as-> row
                                (if app->row-fn
                                  (app->row-fn app row) row)))
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

                 (into
                  [(om/build omut/nav-tabs (app :tabs)
                             {:opts {}})

                   (omut/ui-nav-tab
                    (app :tabs) 0
                    (dom/fieldset
                     nil
                     (dom/legend nil "Основные данные")

                     (om/build omut/input-form-group (get-in app [:username])
                               {:opts {:label "Наименование"
                                       :spec-input {:onChange-valid?-fn
                                                    omut/input-vldfn-not-empty}}})


                     (om/build omut/input-change-password-group (get-in app [:password]))

                     (om/build omut/textarea-form-group (get-in app [:description])
                               {:opts {:label "Описание"}})

                     ))

                   (omut/ui-nav-tab
                    (app :tabs) 1
                    (dom/fieldset
                     nil
                     (dom/legend nil "Роли пользователя")
                     (apply dom/div #js {:className "col-sm-12 col-md-12 col-lg-12"}

                            (let [roles (get-in @app [:troles :roles])
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
                                                   (om/build omut/toggle-button (get-in app [:troles :roles g-id i :user-role?]))
                                                   title))
                                        (range))
                                       (apply dom/div nil)) ))
                               (seq groups)) ))))]

                  (or  abody []))


                 )


                })
        }))))


(def webusers-modal-edit-form-app-init
  (merge omut/modal-edit-form-for-id--YN--app-init webusers-edit-form-app-init))

(defn webusers-modal-edit-form [app _ opts]
  (reify
      om/IRender
    (render [_]
      (om/build omut/modal-edit-form-for-id--YN- app
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
   omut/search-view-app-init
   {:modal-add webusers-modal-edit-form-app-init
    :modal-act omut/actions-modal-app-init
    :modal-yes-no (assoc omut/modal-yes-no-app-init :row {})
    }))

(defn webusers-search-view [app webuserser {:keys [selection-type
                                                   editable?]
                                            :or {selection-type :one}}]
  (reify
      om/IInitState
    (init-state [_]
      {:chan-update (chan)
       :chan-modal-act (chan)
       :chan-modal-add-id (chan)})
    om/IRenderState
    (render-state [_ {:keys[chan-modal-act
                            chan-modal-add-id
                            chan-update]}]
      (dom/div nil
               (om/build omut/search-view app
                         {:opts
                          {:chan-update chan-update
                           :data-update-fn
                           (fn [app]
                             (ixnet/get-data "/tc/rb/webusers/list/transit"
                                             {:fts-query (get-in @app [:fts-query :value])
                                              :page (get-in @app [:page])
                                              :arls (get-in @app [:arls] #{})}
                                             (fn [response]
                                               (om/update! app :data (vec response)))))
                           :data-rendering-fn
                           (fn [app-2]
                             (omut/ui-table {:hover? true
                                             :bordered? true
                                             :striped? true
                                             :responsive? true
                                             :thead (omut/ui-thead-tr [(dom/th nil "№")
                                                                       (dom/th nil "Наименование")
                                                                       (dom/th nil "Описание")])
                                             :tbody
                                             (om/build omut/tbody-trs-sel (:data app-2)
                                                       {:opts {:selection-type selection-type
                                                               :app-to-tds-seq-fn
                                                               (fn [{:keys [id username description]}]
                                                                 [(dom/td nil id)
                                                                  (dom/td nil username)
                                                                  (dom/td #js{:style #js {:whiteSpace "normal"}}
                                                                          description)])
                                                               :on-select-fn
                                                               (fn [{:keys [id] :as row}]
                                                                 (when editable?
                                                                   (put! chan-modal-act
                                                                         {:label (str "Выбор действий над записью №" id)
                                                                          :acts
                                                                          [{:text "Редактировать" :btn-type :primary
                                                                            :act-fn (fn []
                                                                                      (put! chan-modal-add-id id)
                                                                                      (omut/modal-show (:modal-add app)))}
                                                                           {:text "Удалить" :btn-type :danger
                                                                            :act-fn #(do
                                                                                       (om/update! app [:modal-yes-no :row] row)
                                                                                       (omut/modal-show (:modal-yes-no app)))}]
                                                                          }))
                                                                 )
                                                               }})
                                             }))
                           :add-button-fn
                           #(do (omut/modal-show (:modal-add app))
                                (put! chan-modal-add-id 0)
                                )
                           }})

               (when editable?
                 (om/build omut/actions-modal (:modal-act app) {:opts {:chan-open chan-modal-act}}))

               (om/build webusers-modal-edit-form (:modal-add app)
                         {:opts {
                                 :chan-load-for-id chan-modal-add-id
                                 :post-save-fn #(do
                                                  (when chan-update
                                                    (put! chan-update 1))
                                                  1)
                                 }})

               (when editable?
                 (om/build omut/modal-yes-no (:modal-yes-no app)
                           {:opts {:modal-size :sm
                                   :label "Желаете удалить запись?"
                                   :body
                                   (dom/div
                                    #js{:className "row"}
                                    (dom/h3 #js{:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"}
                                            (get-in @app [:modal-yes-no :row :username])))
                                   :act-yes-fn
                                   (fn []
                                     (ixnet/get-data
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
  (merge omut/edit-form-for-id-app-init
         {:password omut/input-change-password-app-init}))


(defn webusers-change-password-form [app _ _]
  (reify
      om/IInitState
    (init-state [_]
      {:chan-save (chan)})
    om/IRenderState
    (render-state [_ {:keys [chan-save]}]
      (om/build
       omut/edit-form-for-id app
       {:opts
        {:uri-save "/tc/rb/webusers/change-password/transit"
         :chan-save chan-save
         :app-to-row-fn
         (fn []
           (omut/input-change-password-check (@app :password))
           {:password (omut/input-change-password-value (@app :password))})
         :form-body
         (dom/fieldset
          nil
          (dom/legend nil "Сменить пароль пользователя")
          (om/build omut/input-change-password-group (get-in app [:password]))

          (dom/button #js {:className "btn btn-primary"
                           :type "button"
                           :onClick (fn [_]
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
