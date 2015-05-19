(ns ixinfestor.stext
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [hipo.core :as hipo]
            [ixinfestor.core :as ix]

            [ixinfestor.stext-edit :as stext-edit]

            [ajax.core :refer [GET POST]]
            [dommy.core :as dommy :refer-macros [sel sel1]]

            [cljs.core.async :as async :refer [>! <! put! chan alts!]]

            [goog.events :as events]
            [goog.dom.classes :as classes]))

(enable-console-print!)


(defn stext-new [page-main--chan-show-page
                 chan-do-after-repaint
                 {:keys [stext-edit-imap]}]
  (let [stext-edit-rmap (stext-edit/stext-edit-new
                         page-main--chan-show-page
                         chan-do-after-repaint
                         (or stext-edit-imap {}))
        page-state (atom {:page 1 :page-size 5 :fts-query ""})
        chan-repaint (chan)
        chan-repaint-table (chan)
        chan-pagination (chan)
        chan-select-row (chan)
        ]

    (go
      (while true
        (let [chan-do-after-repaint (<! chan-repaint)]
          (ix/clear-and-set-on-tag-by-id :page-caption "Справочник текстовых полей")

          (ix/clear-and-set-on-tag-by-id
           :toolbars-container
           [:div {:class "row" :style "padding: 4px"}

            [:div {:class "col-sm-1 col-md-1 col-lg-1"}]

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
          ;;(println @page-state)
          (put! chan-repaint-table 1))))

    (go
      (while true
        (let [_ (<! chan-repaint-table)]
          (POST "/tc/rb/stext/list"
                {:params (select-keys @page-state [:page :page-size :fts-query])
                 :format :json
                 :response-format :json
                 :error-handler ix/error-handler
                 :keywords? true
                 :handler (fn [rows]
                            (ix/clear-and-set-on-tag-by-id
                             :main
                             [:div {:class "container-fluid"}
                              [:div {:class "row"}
                               [:div {:class "col-sm-12 col-md-12 col-lg-12"}
                                [:table {:class "table table-striped"}
                                 [:thead
                                  [:tr
                                   [:th "Текстовые переменные"]
                                   ]
                                  ]
                                 [:tbody
                                  (map
                                   (fn [row]
                                     [:tr {:on-click #(this-as this (put! chan-select-row [this (row :id)]))
                                           :style "cursor: pointer"}
                                      [:td
                                       [:div {:class "media"}
                                        [:div {:class "media-left"}
                                         [:span {:class "glyphicon glyphicon-edit"
                                                 :style "font-size:3em" :aria-hidden "true"}]
                                         ]
                                        [:div {:class "media-body"}
                                         [:h4 {:class "media-heading"} (row :keyname)]
                                         (row :description)
                                         ]]
                                       ]])
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
                             :on-click #(put! (stext-edit-rmap :chan-show-dialog)
                                              {:on-close :stext :id id})
                             }
                    "Редактировать"]
                   ]
            :footer [:button {:class "btn btn-default"
                              :data-dismiss "modal", :type "button"
                              :on-click #(dommy/remove-class! this :active)
                              }
                     "Закрыть"]}))))

    {:chan-repaint chan-repaint
     :stext-edit-rmap stext-edit-rmap}
    ))
