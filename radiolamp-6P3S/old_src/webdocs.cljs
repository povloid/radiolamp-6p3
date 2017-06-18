(ns ixinfestor.webdocs
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [hipo.core :as hipo]
            [ixinfestor.core :as ix]

            [ixinfestor.webdocs-edit :as webdocs-edit]

            [dommy.core :as dommy :refer-macros [sel sel1]]

            [cljs.core.async :as async :refer [>! <! put! chan alts!]]

            [goog.events :as events]
            [goog.dom.classes :as classes]))

(enable-console-print!)

;; ----------------------------------------------------------------------------------------------

(defn- tree-node-cr [{:keys [id tagname description href active? on-click-fn open? const]}]
  (let [const-css (if const "text-warning" "")]
    [:li (if active?
           {:id id :role "presentation" :class "active"}
           {:id id :role "presentation"})
     [:a {:href (or href "#") :style "width: 190px"
          :on-click (or on-click-fn #(.log js/console "click-" id))}
      (if open?
        [:span {:class (str "glyphicon glyphicon-folder-open " const-css)  :aria-hidden "true"}]
        [:span {:class (str "glyphicon glyphicon-folder-close " const-css) :aria-hidden "true"}])
      " "
      [:span {:class const-css} (ix/drop-long-string tagname 50)]
      ]]))

(defn- tree-node-add-childs [tree-node childs]
  (conj tree-node [:ul {:class "nav nav-pills nav-stacked" :style "margin-left: 5px"}  childs]))

;; ----------------------------------------------------------------------------------------------

(def doc-buttons-plus-fn :doc-buttons-plus-fn)

(def webdocs-edit-imap-key :webdocs-edit-imap)

(defn webdocs-new [page-main--chan-show-page
                   chan-do-after-repaint
                   {:keys [doc-buttons-plus-fn]
                    :or {doc-buttons-plus-fn (fn [page-state chan-repaint-tree chan-repaint-table] (list))}
                    :as specific}]
  (let [
        webdocs-edit-rmap (webdocs-edit/webdocs-edit-new
                           page-main--chan-show-page
                           chan-do-after-repaint
                           (or (webdocs-edit-imap-key specific) {}))
        page-state (atom {:tag-id 0 :page 1 :page-size 5 :fts-query ""})
        chan-repaint (chan)
        chan-repaint-tree (chan)
        chan-repaint-table (chan)
        chan-pagination (chan)
        chan-switch-tag (chan)
        chan-select-row (chan)

        chan-dialog-delete-row (chan)
        chan-delete-row (chan)

        chan-add-tag (chan)
        chan-do-tag (chan)
        ]


    (go
      (while true
        (let [_ (<! chan-repaint)]
          (ix/clear-and-set-on-tag-by-id :page-caption "Справочник документов")

          (ix/clear-and-set-on-tag-by-id
           :toolbars-container
           [:div {:class "row"}

            [:div {:class "col-sm-2 col-md-2 col-lg-2" :style "padding: 2px"}
             [:button {:class "btn btn-primary"
                       :role "button"
                       :on-click #(put! chan-do-tag {:parent_id (@page-state :tag-id)})
                       }
              "новый тег"]
             ]

            [:div {:class "col-sm-10  col-md-10  col-lg-10"
                   :style "padding: 2px;text-align:right"}

             [:div {:style "float:left" :class "btn-group" :role "group"}
              [:button {:class "btn btn-primary"
                        :role "button"
                        :on-click #(put! (webdocs-edit-rmap :chan-show-dialog)
                                         {:on-close :webdocs
                                          :id nil
                                          :tag-id (@page-state :tag-id)})
                        }
               "новый документ"]


              (doc-buttons-plus-fn page-state chan-repaint-tree chan-repaint-table)
              ;; [:button {:class "btn btn-default"
              ;;           :role "button"
              ;;           ;;:on-click #(put! chan-do-tag {:parent_id (@page-state :tag-id)})
              ;;           }
              ;;  "Импорт"]
              ]

             [:div {:style "display:inline-block;width:220px;padding-right:4px"}
              (ix/input-fts {:id :fts-form-div
                             :value (@page-state :fts-query)
                             :on-change-fn #(swap! page-state assoc :fts-query %)
                             :find-fn #(do
                                         (swap! page-state assoc :page 1)
                                         (ix/t-table-paginator-update-value-from-map :pagenator-1 @page-state)
                                         (put! chan-repaint-table 1))
                             })
              ]

             (ix/t-table-paginator :pagenator-1 "" chan-pagination @page-state)

             ]])

          (ix/clear-and-set-on-tag-by-id
           :main
           (list
            [:div {:class "container-fluid"}
             [:div {:class "row"}
              [:div {:class "col-sm-3 col-md-3 col-lg-3 sidebar"}
               [:ul {:id "tree-bar" :class "nav nav-pills nav-stacked"}]]
              [:div {:id "main-content"
                     :class "col-sm-9 col-sm-offset-3 col-md-9 col-md-offset-3 col-lg-9 col-lg-offset-3"}]
              [:div {:id "modal-1"}]]]))

          ;;;;;;(page-main/main-set-padding-top-by-navbar)

          (put! chan-repaint-tree 1)
          (put! chan-repaint-table 1)

          (put! chan-do-after-repaint 1))))

    (go
      (while true
        (let [id (<! chan-switch-tag)]
          (swap! page-state assoc :tag-id id :page 1)
          (ix/t-table-paginator-update-value-from-map :pagenator-1 @page-state)
          (put! chan-repaint-tree 1)
          (put! chan-repaint-table 1))))

    (go
      (while true
        (let [[_ k v] (<! chan-pagination)]
          (swap! page-state assoc k v)
          (println @page-state)
          (put! chan-repaint-table 1))))


    ;; Рисуем иерархию тегов слева
    (go
      (while true
        (let [_ (<! chan-repaint-tree)
              tree-bar (ix/by-id :tree-bar)]
          (dommy/clear! tree-bar)
          (ix/ajax-post-json
           "/tag/path-and-chailds"
           {:id (@page-state :tag-id)}
           (fn [[tree-path childs :as response]]
             (let [root-e {:id 0 :tagname "Корень" :open? true}
                   [selected-node & path] (if (empty? tree-path) [root-e] (conj tree-path root-e))]
               ;;(.log js/console "go!")
               ;;(.log js/console (str response))
               (->> childs
                    (map (fn [n] (tree-node-cr (assoc n :on-click-fn #(put! chan-switch-tag (:id n))))))
                    (tree-node-add-childs
                     (tree-node-cr (assoc selected-node
                                          :active? true
                                          :open? true
                                          :on-click-fn #(put! chan-do-tag selected-node)
                                          )))
                    ((fn [o]
                       (reduce #(tree-node-add-childs %2 %1)
                               o (map (fn [n]
                                        (tree-node-cr
                                         (assoc n
                                                :open? true
                                                :on-click-fn #(put! chan-switch-tag (:id n)))))
                                      path))))
                    (hipo/create)
                    (dommy/append! tree-bar))))))))


    (go
      (while true
        (let [_ (<! chan-repaint-table)]
          ;; Рендерим таблицу товаров
          (ix/ajax-post-json
           "/tc/rb/webdocs/bytag"
           (select-keys @page-state [:tag-id :page :page-size :fts-query])
           (fn [rows]
             (ix/clear-and-set-on-tag-by-id
              :main-content
              [:table {:class "table table-striped"}
               [:thead
                [:tr
                 [:th "Список документов"]
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
                       (when-let [i (row :web_title_image)]
                         [:img {:class "media-object"
                                :style "width:64px"
                                :src i :alt "Аватарка"}])
                       ]
                      [:div {:class "media-body"}
                       [:h4 {:class "media-heading"} (row :keyname)]
                       (row :web_top_description)
                       ]]]])
                 rows)]]))))))


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
                             :on-click #(put! (webdocs-edit-rmap :chan-show-dialog)
                                              {:on-close :webdocs :id id})
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
          (ix/ajax-post-json
           "/tc/rb/webdocs/delete"
           row
           (fn [response]
             (put! chan-repaint-table 1)
             (println "OK"))))))


    ;; TAGS OPERATIONS -------------------------------------------------------------------------------------------

    (go
      (while true
        (let [{id :id :as row} (<! chan-do-tag)]

          (println "EDIT TAG ")

          (ix/modal-cr-and-show-on-id
           :modal-1
           {:title (if id (str "Редактрование записи #" id) "Новый тег")
            :body [:form {:id "main-form" :class "form-horizontal"}

                   [:div {:class "form-group"}
                    [:label {:class "col-sm-2 control-label" :for "input-tagname"}
                     "Название"]
                    [:div {:class "col-sm-10"}
                     [:input {:id "input-tagname" :class "form-control"
                              :placeholder "Название...", :type "text"
                              :value (row :tagname)}]]]

                   [:div {:class "form-group"}
                    [:label {:class "col-sm-2 control-label" :for "input-tag-description"}
                     "Описание"]
                    [:div {:class "col-sm-10"}
                     [:textarea {:id "input-tag-description" :class "form-control"
                                 :placeholder "описание...", :rows 4}
                      (row :description) ]]]

                   ]


            :footer (list
                     (when id
                       [:button {:class "btn btn-danger navbar-left" :data-dismiss "modal", :type "button"
                                 :on-click (fn []
                                             (println "DELETE TAG:")
                                             (ix/ajax-post-json
                                              "/tag/delete"
                                              row
                                              (fn [response]
                                                (println "old row - " row)
                                                (put! chan-switch-tag (or (:parent_id row) 0))
                                                (println "OK"))
                                              ))}
                        "Удалить"])

                     [:button {:class "btn btn-primary" ;;:data-dismiss "modal",
                               :type "button"
                               :on-click (fn []
                                           (println "SAVE TAG:")
                                           (when-let [row (-> row ;;(if id {:id id} {})
                                                              (update-in [:parent_id] #(if (= % 0) nil %))
                                                              (ix/input-validate-and-assoc :tagname :input-tagname [ix/validate-for-not-empty])
                                                              (assoc :description (-> :input-tag-description ix/by-id dommy/value))
                                                              ix/input-all-valid-or-nil)]
                                             (ix/ajax-post-json
                                              "/tag/save"
                                              row
                                              (fn [response]
                                                (put! chan-repaint-tree 1)
                                                (ix/modal-hide-on-id :modal-1)
                                                (println "OK"))                                              
                                              (fn [response]
                                                (ix/modal-hide-on-id :modal-1)
                                                (ix/error-handler response)))))
                               }
                      "Принять"]

                     [:button {:class "btn btn-default" :data-dismiss "modal", :type "button"}
                      "Закрыть"]
                     )})
          )))


    {:chan-repaint chan-repaint
     :webdocs-edit-rmap webdocs-edit-rmap}
    ))
