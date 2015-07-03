(ns ixinfestor.files
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [hipo.core :as hipo]
            [ixinfestor.core :as ix]
            [ixinfestor.io :as ix-io]

            [dommy.core :as dommy :refer-macros [sel sel1]]

            [cljs.core.async :as async :refer [>! <! put! chan alts!]]

            [goog.events :as events]
            [goog.dom.classes :as classes]))

(enable-console-print!)


(defn files-new [page-main--chan-show-page
                 chan-do-after-repaint {}]
  (let [page-state (atom {:page 1 :page-size 5 :fts-query ""})
        chan-repaint (chan)
        chan-repaint-table (chan)
        chan-pagination (chan)
        chan-select-row (chan)
        chan-edit-row (chan)
        chan-delete-row (chan)
        chan-add (chan)
        ]

    (go
      (while true
        (let [chan-do-after-repaint (<! chan-repaint)]
          (ix/clear-and-set-on-tag-by-id :page-caption "Файлы и картинки")

          (ix/clear-and-set-on-tag-by-id
           :toolbars-container
           [:div {:class "row" :style "padding: 4px"}

            [:div {:class "col-sm-1 col-md-1 col-lg-1"}

             [:form {:id "image-uploader-form" :enctype "multipart/form-data" :method "POST"}
              [:span {:class "btn btn-default btn-file btn-primary"}
               "Добавить фаил"
               [:input {:id "image-uploader"
                        :name "image-uploader"
                        :type "file" :multiple true :accept "image/gif, image/jpeg, image/png"
                        :on-change #(put! chan-add 1)}]]]
             ]

            [:div {:class "col-sm-3 col-sm-offset-2 col-md-3 col-md-offset-2 col-lg-3 col-lg-offset-2"
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

            [:div {:class "col-sm-4 col-sm-offset-2 col-md-4 col-md-offset-2 col-lg-4 col-lg-offset-2"}
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
          (ix/ajax-post-json
           "/files/list"
           (select-keys @page-state [:page :page-size :fts-query])
           (fn [rows]
             (ix/clear-and-set-on-tag-by-id
              :main
              [:div {:class "container-fluid"}
               [:div {:class "row"}
                [:div {:class "col-sm-12 col-md-12 col-lg-12"}
                 [:table {:class "table table-striped"}
                  [:thead
                   [:tr
                    [:th "Таблица ресурсов"]
                    ]
                   ]
                  [:tbody
                   (map
                    (fn [{:keys [id path content_type top_description description] :as row}]
                      (let [image? (#{"image/gif"
                                      "image/jpeg"
                                      "image/pjpeg"
                                      "image/png"
                                      "image/svg+xml"
                                      "image/tiff"} content_type)]

                        [:tr {:on-click #(this-as this (put! chan-select-row row))
                              :style "cursor: pointer"}
                         [:td
                          [:div {:class "media"}
                           [:div {:class "media-left"}
                            (if image?
                              [:img {:class "media-object"
                                     :style "width:64px"
                                     :src path :alt "Аватарка"}]
                              [:span {:class "glyphicon glyphicon-file"
                                      :style "font-size:5em;float:left" :aria-hidden "true"}]
                              )
                            ]
                           [:div {:class "media-body"}
                            [:h4 {:class "media-heading"} top_description]
                            description
                            [:br]
                            [:div {:style ""}
                             [:span {:class "label label-default" } "URL"]" "
                             [:input {:type "text" :style "width:50%;font-size:0.7em"
                                      :value path
                                      :on-mousedown #(this-as this (.select this))}]]
                            ]]]]
                        ))
                    rows)
                   ]]]]
               [:div {:id "modal-1"}]]
              ))))))

    ;; Канал работы с картинками
    (go
      (while true
        (let [_ (<! chan-add)
              form-e (ix/by-id :image-uploader-form)]
          (ix-io/file-upload
           form-e "/files/upload"
           {:success #(do (put! chan-repaint-table 1)
                          (println "IMAGE UPLOAD SUCCES!!!"))}
           ))))


    (go
      (while true
        (let [row (<! chan-select-row)]
          (println "[*]" row)
          (ix/modal-cr-and-show-on-id
           :modal-1
           {:title (str "Операции с запипсью #" (:id row))
            :body [:div {:class "well center-block" :style "max-width:400px;"}
                   [:a {:class "btn btn-info btn-lg btn-block"
                        :target "_blank" :href (:path row)
                        :type "button" }
                    "открыть в другой вкладке"]
                   [:button {:class "btn btn-primary btn-lg btn-block"
                             :role "button" :data-dismiss "modal"
                             :on-click #(put! chan-edit-row row)}
                    "Редактировать"]
                   [:button {:class "btn btn-danger btn-lg btn-block"
                             :type "button" :data-dismiss "modal"
                             :on-click #(put! chan-delete-row row)}
                    "Удалить"]]
            :footer [:button {:class "btn btn-default" :data-dismiss "modal", :type "button"}
                     "Закрыть"]}))))

    (go
      (while true
        (let [row (<! chan-edit-row)]
          (println "Edit image row! " row)

          (ix/modal-cr-and-show-on-id
           :modal-1
           {:title (str "Редактирование записи #" (:id row))
            :body [:form {:id "main-form" :class "form-horizontal"}

                   [:div {:class "form-group"}
                    [:label {:class "col-sm-2 control-label" :for "input-image-top-description"}
                     "Название"]
                    [:div {:class "col-sm-10"}
                     [:input {:id "input-image-top-description" :class "form-control"
                              :placeholder "Название...", :type "text"
                              :value (row :top_description)}]]]

                   [:div {:class "form-group"}
                    [:label {:class "col-sm-2 control-label" :for "input-image-galleria"}
                     "Галерея"]
                    [:div {:class "col-sm-10"}
                     [:div {:class "checkbox"}
                      [:label
                       [:input {:id "input-image-galleria" :type "checkbox"
                                :checked (row :galleria)}]
                       "отображать"]]
                     ]]

                   [:div {:class "form-group"}
                    [:label {:class "col-sm-2 control-label" :for "input-image-description"}
                     "Описание"]
                    [:div {:class "col-sm-10"}
                     [:textarea {:id "input-image-description" :class "form-control"
                                 :placeholder "описание...", :rows 4}
                      (row :description) ]]]
                   ]

            :footer
            (list
             [:button
              {:class "btn btn-primary btn-lg"
               :type "button" :data-dismiss "modal"
               :on-click (fn []
                           (ix/ajax-post-json
                            "/files/edit"
                            {:id (row :id)
                             :top_description (dommy/value (sel1 :#input-image-top-description))
                             :galleria (.-checked (sel1 :#input-image-galleria))
                             :description (dommy/value (sel1 :#input-image-description))}
                            (fn [response]
                              (put! chan-repaint-table 1)
                              (println "IMAGE EDITED!"))))}
              "Сохранить"]

             [:button {:class "btn btn-default" :data-dismiss "modal", :type "button"} "Закрыть"])
            }
           ))))

    (go
      (while true
        (let [row (<! chan-delete-row)]
          (println "Delete image row! " row )

          (ix/modal-cr-and-show-on-id
           :modal-1
           {:title (str "Удаление записи #" (:id row))
            :body  [:div {:class "row"}
                    [:img {:class "thumbnail col-sm-4 col-sm-offset-4"
                           :src (row :path) :alt "фото"}]]
            :footer (list
                     [:button {:class "btn btn-danger btn-lg"
                               :type "button" :data-dismiss "modal"
                               :on-click (fn []
                                           (ix/ajax-post-json
                                            "/files/delete"
                                            {:id (:id row)}
                                            (fn [response]
                                              (put! chan-repaint-table 1)
                                              (println "IMAGE DELETED!")))) }
                      "Удалить!"]
                     [:button {:class "btn btn-default" :data-dismiss "modal", :type "button"} "Закрыть"])
            }))))




    {:chan-repaint chan-repaint}))
