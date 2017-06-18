(ns ixinfestor.change-password
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [hipo.core :as hipo]
            [ixinfestor.core :as ix]
            [dommy.core :as dommy :refer-macros [sel sel1]]

            [cljs.core.async :as async :refer [>! <! put! chan alts!]]

            [goog.events :as events]
            [goog.dom.classes :as classes]))

;; Включаем печать в консоль из функции println
(enable-console-print!)


(defn change-password-new [page-main--chan-show-page
                           chan-do-after-repaint
                           {:keys []}]
  (let [chan-repaint (chan)
        chan-save (chan)]

    (go
      (while true
        (let [_ (<! chan-repaint)]

          (ix/clear-and-set-on-tag-by-id :page-caption "Смена пароля пользователя")

          (ix/clear-and-set-on-tag-by-id
           :toolbars-container
           [:div {:class "row" :style "padding:4px"}
            [:botton {:class "btn btn-primary col-sm-2 col-md-2 col-lg-2" :style "margin-left:4px;"
                      :on-click #(put! chan-save 1)} "Сохранить пароль"]
            ])

          (ix/clear-and-set-on-tag-by-id
           :main
           [:div {:class "container-fluid"}
            [:div {:class "row"}
             [:div {:class "col-sm-12 col-md-12 col-lg-12"}
              [:form {:id "main-form" :class "form-horizontal col-sm-12 col-md-10 col-lg-9"}
               [:div {:id "modal-1"}]

               [:fieldset
                [:legend "Форма смены пароля"]
                [:div {:class "form-group"}
                 [:label {:class "col-sm-3 control-label" :for "input-password-1"} "пароль"]
                 [:div {:class "col-sm-4"}

                  [:input {:id "input-password-1" :class "form-control" :type "password" }]
                  [:input {:id "input-password-2" :class "form-control" :type "password" }]
                  ]]

                ]]]]])

          (put! chan-do-after-repaint 1))))

    (go
      (while true
        (let [_ (<! chan-save)]
          (when-let [row  (-> {}
                              (ix/input-validate-and-assoc :password :input-password-1
                                                           [(partial ix/validate-equal-vals
                                                                     "Введеные пароли не совпадают"
                                                                     :input-password-2)])
                              ix/input-all-valid-or-nil)]
            (ix/ajax-post-json
             "/tc/rb/webusers/change-password"
             row

             (fn [_]
               (put! chan-repaint 1)
               (ix/display-message-on-time 2000 "Запись сохранена успешно")))))))

    {:chan-repaint chan-repaint}))
