(ns ixinfestor.webusers
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [hipo.core :as hipo]
            [ixinfestor.core :as ix]
            [ixinfestor.webusers-edit :as webusers-edit]
            [ajax.core :refer [GET POST]]
            [dommy.core :as dommy :refer-macros [sel sel1]]

            [cljs.core.async :as async :refer [>! <! put! chan alts!]]

            [goog.events :as events]
            [goog.dom.classes :as classes]))

(enable-console-print!)

(defn webusers-new [page-main--chan-show-page
                    chan-do-after-repaint
                    {:keys [webusers-edit-imap]}]
  (let [webusers-edit-rmap (webusers-edit/webusers-edit-new
                            page-main--chan-show-page
                            chan-do-after-repaint
                            (or webusers-edit-imap {}))
        page-state (atom {:page 1 :page-size 5 :fts-query ""})
        chan-repaint (chan)
        chan-repaint-table (chan)
        chan-pagination (chan)
        chan-select-row (chan)

        chan-dialog-delete-row (chan)
        chan-delete-row (chan)
        ]

    (go
      (while true
        (let [chan-do-after-repaint (<! chan-repaint)]
          (ix/clear-and-set-on-tag-by-id :page-caption "Справочник пользователей")

          (ix/clear-and-set-on-tag-by-id
           :toolbars-container
           [:div {:class "row" :style "padding: 4px"}

            [:button {:id "btn-new-user" :class "btn btn-primary col-sm-1 col-md-1 col-lg-1"
                      :role "button" :on-click #(put! (webusers-edit-rmap :chan-show-dialog)
                                                      {:on-close :webusers :id nil})}
             "Создать"]

            [:div {:class "col-sm-3 col-sm-offset-1 col-md-3 col-md-offset-1 col-lg-3 col-lg-offset-1"
                   :style "padding: 2px"}
             (ix/input-fts {:id :fts-form-div
                            :value (@page-state :fts-query)
                            :on-change-fn #(swap! page-state assoc :fts-query %)
                            :find-fn #(do
                                        (swap! page-state assoc :page 1)
                                        (ix/t-table-paginator-update-value-from-map :pagenator-1 @page-state)
                                        (put! chan-repaint-table 1))
                            })
             ]


            [:div {:class "col-sm-4 col-sm-offset-3 col-md-4 col-md-offset-3 col-lg-4 col-lg-offset-3"}
             (ix/t-table-paginator :pagenator-1 "" chan-pagination @page-state)]

            ])

          (put! chan-repaint-table 1)
          (put! chan-do-after-repaint 1))))

    (go
      (while true
        (let [[_ k v] (<! chan-pagination)]
          (swap! page-state assoc k v)
          (println @page-state)
          (put! chan-repaint-table 1))))

    (go
      (while true
        (let [_ (<! chan-repaint-table)]
          (println @page-state)
          (POST "/tc/rb/webusers/list"
                {:params (select-keys @page-state [:page :page-size :fts-query])
                 :format :json
                 :response-format :json
                 :error-handler ix/error-handler
                 :keywords? true
                 :handler (fn [rows]
                            (println rows)
                            (ix/clear-and-set-on-tag-by-id
                             :main
                             [:div {:class "container-fluid"}
                              [:div {:class "row"}
                               [:div {:class "col-sm-12 col-md-12 col-lg-12"}
                                [:table {:class "table table-striped"}
                                 [:thead
                                  [:tr
                                   [:th "#"]
                                   [:th "пользователь"]
                                   [:th "Описание"]
                                   ]
                                  ]
                                 [:tbody
                                  (map
                                   (fn [row]
                                     [:tr {:on-click #(this-as this (put! chan-select-row [this (row :id)]))
                                           :style "cursor: pointer"}
                                      [:td (row :id)]
                                      [:td (row :username)]
                                      [:td (row :description)]
                                      ])
                                   rows)
                                  ]]]]
                              [:div {:id "modal-1"}]]
                             ))}))))

    (go
      (while true
        (let [[this id] (<! chan-select-row)]
          (dommy/add-class! this :active)
          (ix/modal-cr-and-show-on-id
           :modal-1
           {:title (str "Операции с запипсью #" id )
            :body [:div {:class "well center-block" :style "max-width:400px;"}
                   [:button {:class "btn btn-info btn-lg btn-block"
                             :data-dismiss "modal" :type "button"
                             }
                    "Информация"]
                   [:button {:class "btn btn-primary btn-lg btn-block"
                             :role "button"
                             :data-dismiss "modal" ;; В дилогах закрывание всегда надо с :data-dismiss "modal"
                                        ;TODO: написать диалоговые кнопки
                             :on-click #(put! (webusers-edit-rmap :chan-show-dialog)
                                              {:on-close :webusers :id id})
                             }
                    "Редактировать"]
                   [:button {:class "btn btn-danger btn-lg btn-block"
                             :type "button" :data-dismiss "modal"
                             :on-click #(put! chan-dialog-delete-row {:id id})}
                    "Удалить"]]
            :footer [:button {:class "btn btn-default"
                              :data-dismiss "modal", :type "button"
                              :on-click #(dommy/remove-class! this :active)
                              }
                     "Закрыть"]}))))


    (go
      (while true
        (let [row (<! chan-dialog-delete-row)]
          (println "Delete row?" row )
          (ix/dialog-delete-row-yes?-no? row :modal-1 #(put! chan-delete-row row)))))

    (go
      (while true
        (let [row (<! chan-delete-row)]
          (println "Delete row! " row )
          (POST "/tc/rb/webusers/delete"
                {:params row
                 :format :json
                 :response-format :json
                 :keywords? true
                 :error-handler ix/error-handler
                 :handler
                 (fn [response]
                   (put! chan-repaint-table 1)
                   (println "OK"))
                 })
          )))

    {:chan-repaint chan-repaint
     :webusers-edit-rmap webusers-edit-rmap}
    ))
