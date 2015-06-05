(ns ixinfestor.webusers-edit
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [hipo.core :as hipo]
            [ixinfestor.core :as ix]
            [ajax.core :refer [GET POST]]
            [dommy.core :as dommy :refer-macros [sel sel1]]

            [cljs.core.async :as async :refer [>! <! put! chan alts!]]

            [goog.events :as events]
            [goog.dom.classes :as classes]))




;; Включаем печать в консоль из функции println
(enable-console-print!)

(defn webusers-edit-new [page-main--chan-show-page
                         chan-do-after-repaint
                         {:keys []}]
  (let [
        page-state (atom {:user-roles-keys-set #{}})
        _ (add-watch
           page-state :log
           (fn [_ _ old new]
             (when (not= old new)
               (println "New webusers_edit/page-state is"  new))))

        chan-repaint (chan)
        chan-show-dialog (chan)
        chan-save (chan)
        chan-set-role (chan)

        ]
    (go
      (while true
        (let [new-page-data (<! chan-show-dialog)]
          (swap! page-state merge new-page-data)
          (put! page-main--chan-show-page :webusers-edit))))

    (go
      (while true
        (let [[set? role] (<! chan-set-role)]
          (swap! page-state update-in [:user-roles-keys-set] (if set? conj disj) role))))

    (go
      (while true
        (let [_ (<! chan-repaint)]
          (POST "/tc/rb/webusers/find"
               {:params {:id (@page-state :id)}
                :error-handler ix/error-handler
                :format :json
                :response-format :json :keywords? true
                :handler
                (fn [row]
                  (ix/clear-and-set-on-tag-by-id :page-caption "Управление пользователем")

                  (ix/clear-and-set-on-tag-by-id
                   :toolbars-container
                   [:div {:class "row" :style "padding:4px"}
                    [:button {:class "btn btn-default col-sm-2 col-md-2 col-lg-2"
                              :on-click #(put! page-main--chan-show-page (@page-state :on-close))}
                     "закрыть"]
                    [:botton {:class "btn btn-primary col-sm-2 col-md-2 col-lg-2" :style "margin-left:4px;"
                              :on-click #(put! chan-save 1)} "coхранить"]
                    ])

                  (ix/clear-and-set-on-tag-by-id
                   :main
                   [:div {:class "container-fluid"}
                    [:div {:class "row"}
                     [:div {:class "col-sm-12 col-md-12 col-lg-12"}
                      [:form {:id "main-form" :class "form-horizontal col-sm-12 col-md-10 col-lg-9"}
                       [:div {:id "modal-1"}]

                       [:div {:class "form-group"}
                        [:label {:class "col-sm-3 control-label" :for "input-username"} "имя пользователя"]
                        [:div {:class "col-sm-9"}
                         [:input {:id "input-username"  :class "form-control"
                                  :placeholder "Имя пользователя"
                                  :value (or (:username row) "")
                                  :type "text" :required "required"}]]]

                       [:div {:class "form-group"}
                        [:label {:class "col-sm-3 control-label" :for "input-password-1"} "пароль"]
                        [:div {:class "col-sm-4"}

                         [:input {:id "input-password-1" :class "form-control" :type "password" }]
                         [:input {:id "input-password-2" :class "form-control" :type "password" }]
                         ]]

                       [:div {:class "form-group"}
                        [:label {:class "col-sm-3 control-label" :for "input-description"} "Описание"]
                        [:div {:class "col-sm-9"}
                         [:textarea {:id "input-description" :class "form-control"
                                     :placeholder "допольнительные сведения...", :rows 2}
                          (or (:description row) "")]]]

                       [:div {:class "form-group"}
                        [:label {:class "col-sm-3 control-label"} "Роли"]
                        [:div {:class "col-sm-9"}

                         (let [[roles user-roles-keys] (:troles-set row)
                               user-roles-keys-set (set user-roles-keys)]
                           (swap! page-state assoc :user-roles-keys-set user-roles-keys-set)
                           (map (fn [{:keys [id keyname title description]}]
                                  [:div {:class "checkbox"}
                                   [:label
                                    [:input {:type "checkbox"
                                             :checked (user-roles-keys-set keyname)
                                             :on-change #(this-as this (put! chan-set-role [(.-checked this) keyname]))}]
                                    [:b title] [:br]
                                    [:small description]]]) roles))
                         ]]]]]]))}))))

    (go
      (while true
        (let [_ (<! chan-save)]
          (when-let [row  (-> (if-let [id (@page-state :id)] {:id id} {})
                              (ix/input-validate-and-assoc :username :input-username [ix/validate-for-not-empty])
                              (assoc :description (-> :input-description ix/by-id dommy/value))
                              (ix/input-validate-and-assoc :password :input-password-1
                                                           [(partial ix/validate-equal-vals
                                                                     "Введеные пароли не совпадают"
                                                                     :input-password-2)])
                              ix/input-all-valid-or-nil)]
            (POST "/tc/rb/webusers/save"
                  {:params {:row row :user-roles-keys-set (@page-state :user-roles-keys-set)}
                   :error-handler ix/error-handler
                   :format :json
                   :response-format :json :keywords? true
                   :handler
                   (fn [{id :id}]
                     (println id)
                     (swap! page-state assoc :id id)
                     (put! chan-repaint 1)
                     (ix/display-message-on-time 2000 "Запись сохранена успешно"))})))))
    
    {:chan-show-dialog chan-show-dialog
     :chan-repaint chan-repaint}))
