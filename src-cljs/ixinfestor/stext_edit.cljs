(ns ixinfestor.stext-edit

  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [hipo.core :as hipo]
            [ixinfestor.core :as ix]
            [dommy.core :as dommy :refer-macros [sel sel1]]

            [cljs.core.async :as async :refer [>! <! put! chan alts!]]

            [goog.events :as events]
            [goog.dom.classes :as classes]))




;; Включаем печать в консоль из функции println
(enable-console-print!)

(defn stext-edit-new [page-main--chan-show-page
                      chan-do-after-repaint
                      {:keys []}]
  (let [
        page-state (atom {})
        _ (add-watch
           page-state :log
           (fn [_ _ old new]
             (when (not= old new)
               (println "New stext_edit/page-state is"  new))))

        chan-repaint (chan)
        chan-show-dialog (chan)
        chan-save (chan)
        ]

    (go
      (while true
        (let [new-page-data (<! chan-show-dialog)]
          (swap! page-state merge new-page-data)
          (put! page-main--chan-show-page :stext-edit))))

    (go
      (while true
        (let [_ (<! chan-repaint)]
          (ix/ajax-post-json
           "/tc/rb/stext/find"
           {:id (@page-state :id)}

           (fn [row]
             (swap! page-state merge (select-keys row [:plantext]))

             (ix/clear-and-set-on-tag-by-id :page-caption "Редактирование текстовой переменной")

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
                   [:label {:class "col-sm-3 control-label" :for "input-keyname"} "Переменная"]
                   [:div {:class "col-sm-9"}
                    [:div {:class "media"}
                     [:div {:class "media-left"}
                      [:span {:class "glyphicon glyphicon-edit"
                              :style "font-size:3em" :aria-hidden "true"}]
                      ]
                     [:div {:class "media-body"}
                      [:h4 {:class "media-heading"} (row :keyname)]
                      (row :description)
                      ]]]]


                  [:div {:class "form-group"}
                   [:label {:class "col-sm-3 control-label" :for "inputanytext1"} "Описание"]
                   [:div {:class "col-sm-9"}
                    [:textarea {:id "inputanytext1" :class "form-control"
                                :placeholder "Значение текстовой переменной...", :rows 14}
                     (or (:anytext row) "")]]]


                  ;; [:script
                  ;;  "var myCodeMirror = CodeMirror.fromTextArea(document.getElementById('input-anytext'),
                  ;;                    {lineNumbers: true,              // показывать номера строк
                  ;;                     matchBrackets: true,            // подсвечивать парные скобки
                  ;;                     ndentUnit: 4                    // размер табуляции
                  ;;                     });"]

                  ;; Вариант под ckeditor !
                  (when (not (:plantext row))
                    [:script {:type "text/javascript"} "CKEDITOR.replace('inputanytext1');"])
                  ]]]])

             ;; Вариант для codemirror !!!
             (when (:plantext row)
               (let [cm (.fromTextArea js/CodeMirror (ix/by-id "inputanytext1")
                                       (clj->js
                                        {:mode {:name "htmlmixed"
                                                :xml true :javascript true :css true}
                                         :lineNumbers true
                                         :matchBrackets true
                                         :selectionPointer true
                                         :ndentUnit 4
                                         }))]
                 (swap! page-state assoc :cm cm))) )))))

    (go
      (while true
        (let [_ (<! chan-save)]


          (when-let [row  (-> (if-let [id (@page-state :id)] {:id id} {})

                              (as-> new-row
                                  (if (not (@page-state :plantext))
                                    ;; ckeditor !
                                    (assoc new-row :anytext (-> js/CKEDITOR .-instances (aget "inputanytext1") .getData))
                                    ;; codemirror !
                                    (do (-> @page-state :cm .save)
                                        (assoc new-row :anytext (-> :inputanytext1 ix/by-id dommy/value)))))

                              ix/input-all-valid-or-nil)]
            (ix/ajax-post-json
             "/tc/rb/stext/save"
             {:row row}
             (fn [{id :id}]
               (println id)
               (swap! page-state assoc :id id)
               (put! chan-repaint 1)
               (ix/display-message-on-time 2000 "Запись сохранена успешно")))))))

    {:chan-show-dialog chan-show-dialog
     :chan-repaint chan-repaint}))
