(ns ixinfestor.webdocs-edit
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [hipo.core :as hipo]
            [ixinfestor.core :as ix]
            [ixinfestor.io :as ix-io]

            [dommy.core :as dommy :refer-macros [sel sel1]]

            [cljs.core.async :as async :refer [>! <! put! chan alts!]]

            [goog.events :as events]
            [goog.dom.classes :as classes]

            [goog.i18n.DateTimeFormat :as dtf]))

;; Включаем печать в консоль из функции println
(enable-console-print!)

(def specific-common-key :specific-common)
(def specific-common-inputs-fn-key :specific-common-inputs-fn)
(def specific-common-save-fn-key :specific-common-save-fn)

(def specific-tab1-key :specific-tab1)
(def specific-tab1-caption-key :specific-tab1-caption)
(def specific-tab1-tollbar-fn-key :specific-tab1-tollbar-fn)
(def specific-tab1-inputs-fn-key :specific-tab1-inputs-fn)
(def specific-tab1-save-fn-key :specific-tab1-save-fn)


;;**************************************************************************************************
;;* BEGIN tmp zone
;;* tag: <tmp zone>
;;*
;;* description: Зона для обкатки временного кода
;;*
;;**************************************************************************************************

(defn input-one-image [id & [image-url]]
  (let [immage-id (ix/xml-id "-image" id)
        input-id (ix/xml-id "-uploader" id)]
    [:form {:id (name id) :enctype "multipart/form-data" :method "POST"}
     [:div {:class "panel panel-default"}
      [:div {:class "panel-heading"}
       [:span {:class "btn btn-default btn-file btn-primary"}
        "Фаил..."
        [:input {:id (name input-id)
                 :name "file-uploader"
                 :type "file" :multiple true
                 :on-change #(this-as this
                                      (let [img (ix/by-id immage-id)
                                            reader (new js/FileReader)]
                                        (println (-> this .-files (aget 0) .-name))
                                        (println (.createObjectURL js/URL (-> this .-files (aget 0))))
                                        (set! (.-src img) (.createObjectURL js/URL (-> this .-files (aget 0)))) ))
                 }]]]
      [:div {:class "panel-body"}
       ;;[:div {:class "thumbnail"}
       [:img {:id (name immage-id) :src (if image-url (str "/image/" image-url) "") :class "img-thumbnail" :alt "нет картинки"}]
       ;; ]
       ]
      ]]))

;; END tmp zone
;;..................................................................................................

(defn webdocs-edit-new [page-main--chan-show-page
                        chan-do-after-repaint
                        specific]
  (let [page-state (atom {:tab 0 :id nil :tag-id nil})
        _ (add-watch page-state :log
                     (fn [_ _ old new]
                       (when (not= old new)
                         (println ">>>" new))))

        ;; COMMON
        chan-repaint (chan)
        chan-show-dialog (chan)
        chan-switch-tab (chan)
        chan-save-common (chan)
        chan-save-web (chan)

        ;; TAGS
        tags-groups-state (atom #{})
        chan-update-tags (chan)
        chan-toggle-tag (chan)
        chan-save-tags (chan)

        ;; IMAGES
        chan-load-images (chan)
        chan-add-image (chan)
        chan-select-image-action (chan)
        chan-edit-image-row (chan)
        chan-delete-image-row (chan)

        ;; FILES
        chan-load-files (chan)
        chan-add-file (chan)
        chan-select-file-action (chan)
        chan-edit-file-row (chan)
        chan-delete-file-row (chan)
        ]

    (go
      (while true
        (let [new-page-data (<! chan-show-dialog)]
          (swap! page-state merge (update-in new-page-data [:tag-id] #(if (= % "root") nil %)))
          (put! page-main--chan-show-page :webdocs-edit))))

    (go
      (while true
        (let [_ (<! chan-repaint)]
          (ix/ajax-post-json
           "/tc/rb/webdocs/edit"
           {:id (@page-state :id)}
           (fn [row]

             (ix/clear-and-set-on-tag-by-id :page-caption "Редактирование документа")

             (ix/clear-and-set-on-tag-by-id
              :toolbars-container
              (list
               [:div {:class "row" :style "padding: 4px; margin-right: 5px"}
                [:div {:class "col-sm-12 col-md-12 col-lg-12"}
                 [:button {:class "btn btn-default navbar-left"
                           :role "button" :style "margin-right: 10px"
                           :on-click #(put! page-main--chan-show-page (@page-state :on-close))}
                  "закрыть"]

                 ;; tablabels
                 [:ul {:id "tabs" :class "nav nav-tabs" :style "margin-bottom: 0px;"}
                  [:li {:id "tab-label-0" :role "presentation" :on-click #(put! chan-switch-tab 0)} [:a "Основные данные"]]

                  (when-let [s (get-in specific [specific-tab1-key])]
                    [:li {:id "tab-label-1" :role "presentation" :on-click #(put! chan-switch-tab 1)}
                     [:a (if-let [c (get-in s [specific-tab1-caption-key])]
                           c "Спец.")]])

                  [:li {:id "tab-label-2" :role "presentation" :on-click #(put! chan-switch-tab 2)} [:a "Теги"]]
                  [:li {:id "tab-label-3" :role "presentation" :on-click #(put! chan-switch-tab 3)} [:a "Web"]]
                  [:li {:id "tab-label-4" :role "presentation" :on-click #(put! chan-switch-tab 4)} [:a "Фото"]]
                  [:li {:id "tab-label-5" :role "presentation" :on-click #(put! chan-switch-tab 5)} [:a "Файлы"]]
                  ]
                 ]]

               ;; Toolbar - Main
               [:div{:id "tab-toolbar-0" :class "row" :style "padding: 5px; display: none"}
                [:a {:id "btn-save-common" :class "btn btn-primary navbar-left"
                     :role "button" :style "margin-right: 10px" :on-click #(put! chan-save-common 0)}
                 "Сохранить"]]

               ;; Toolbar - 2 cpecific
               (when-let [s (get-in specific [specific-tab1-key])]
                 [:div {:id "tab-toolbar-1" :class "row" :style "padding: 5px; display: none"}
                  (when-let [f (get-in s [specific-tab1-tollbar-fn-key])]
                    (f row))])

               ;; Toolbar - Tags and groups
               [:div {:id "tab-toolbar-2" :class "row" :style "padding: 5px; display: none"}
                [:a {:id "btn-save-tags" :class "btn btn-primary navbar-left"
                     :role "button" :style "margin-right: 10px" :on-click #(put! chan-save-tags 0)}
                 "Сохранить теги"]
                [:div {:id "tags-scroll-buttons"}]
                ]

               ;; Toolbar - Web
               [:div {:id "tab-toolbar-3"  :class "row" :style "padding: 5px; display: none"}
                [:a {:id "btn-save-web" :class "btn btn-primary navbar-left"
                     :role "button" :style "margin-right: 10px" :on-click #(put! chan-save-web 0)}
                 "Сохранить"]]

               ;; Toolbar - Fotos
               [:div {:id "tab-toolbar-4" :class "row" :style "padding: 5px; display: none"}
                [:form {:id "image-uploader-form" :enctype "multipart/form-data" :method "POST"}
                 [:span {:class "btn btn-default btn-file btn-primary"}
                  "Добавить изображение"
                  [:input {:id "image-uploader"
                           :name "image-uploader"
                           :type "file" :multiple true :accept "image/gif, image/jpeg, image/png, image/*"
                           :on-change #(put! chan-add-image 1)}]]]]

               ;; Toolbar - Files
               [:div {:id "tab-toolbar-5" :class "row" :style "padding: 5px; display: none"}
                [:form {:id "file-uploader-form" :enctype "multipart/form-data" :method "POST"}
                 [:span {:class "btn btn-default btn-file btn-primary"}
                  "Добавить фаил"
                  [:input {:id "file-uploader"
                           :name "file-uploader"
                           :type "file" :multiple true
                           :on-change #(put! chan-add-file 1)}]]]]
               ))


             (ix/clear-and-set-on-tag-by-id
              :main
              [:div {:class "container-fluid"}
               [:div {:class "row"}
                [:div {:id "tab-0" :class "col-sm-12 col-md-12 col-lg-12" :style "display: none"}

                 ;; Panel - Main
                 [:form {:id "main-form" :class "form-horizontal col-sm-12 col-md-10 col-lg-9"}
                  [:fieldset
                   [:legend "Основные данные документа"]
                   (when-let [id (get-in row [:webdoc-row :id])]
                     (list
                      [:div {:class "form-group"}
                       [:label {:class "col-sm-2 control-label" :for "input-web-meta-subject"} "Параметры документа"]
                       [:div {:class "col-sm-5"}
                        [:ul {:class "list-group"}
                         [:li {:class "list-group-item"}
                          [:span {:class "badge"}
                           (ix/str-to-date-and-format :SHORT_DATETIME "нет" (get-in row [:webdoc-row :cdate]))
                           ]
                          "Cоздан:"]
                         [:li {:class "list-group-item"}
                          [:span {:class "badge"}
                           (ix/str-to-date-and-format :SHORT_DATETIME "нет" (get-in row [:webdoc-row :udate]))
                           ]
                          "Обновлен:"]]]]

                      [:div {:class "form-group"}
                       [:label {:class "col-sm-2 control-label"} "URL+"]
                       [:div {:class "col-sm-10"}
                        [:div {:class "panel panel-default"}
                         [:div {:class "panel-body"}
                          [:a {:href (str "/doc/" id "/" (get-in row [:webdoc-row :ttitle])) :target "_blank"}
                           "Документ на сайте..."]
                          [:br]
                          [:var (str "/doc/" id "/" (get-in row [:webdoc-row :ttitle]))]]]]]))

                   [:div {:class "form-group"}
                    [:label {:class "col-sm-2 control-label" :for "input-url1"} "URL"]
                    [:div {:class "col-sm-10"}
                     [:div {:class "checkbox"}
                      [:label
                       [:input {:id "input-url1flag" :type "checkbox"
                                :checked (get-in row [:webdoc-row :url1flag] false)}]
                       "перенаправлять на"]]
                     [:input {:id "input-url1" :class "form-control"
                              :placeholder "Дополниетельные URL...", :type "text"
                              :value (get-in row [:webdoc-row :url1] "")}]]]

                   [:div {:class "form-group"}
                    [:label {:class "col-sm-2 control-label" :for "input-keyname"} "Заголовок"]
                    [:div {:class "col-sm-10"}
                     [:input {:id "input-keyname" :class "form-control"
                              :placeholder "наименование", :type "text"
                              :value (get-in row [:webdoc-row :keyname] "")}]]]

                   [:div {:class "form-group"}
                    [:label {:class "col-sm-2 control-label" :for "input-description"} "Описание"]
                    [:div {:class "col-sm-10"}
                     [:textarea {:id "input-description" :class "form-control"
                                 :placeholder "описание", :rows 2}
                      (get-in row [:webdoc-row :description] "")]]]



                   ]

                  ;; Fieldsets...
                  (when-let [f (get-in specific [specific-common-key specific-common-inputs-fn-key])]
                    (f row))

                  ]]

                (when-let [s (get-in specific [specific-tab1-key])]
                  [:div {:id "tab-1" :class "col-sm-12 col-md-12 col-lg-12" :style "display: none"}
                   (when-let [f (get-in s [specific-tab1-inputs-fn-key])]
                     (f row))])

                ;; Panel - Tags and groups
                [:div {:id "tab-2" :class "col-sm-12 col-md-12 col-lg-12" :style "display: none"}
                 [:div {:id "tags-list"}]]

                ;; Panel - Web content
                [:div {:id "tab-3" :class "col-sm-12 col-md-12 col-lg-12" :style "display: none"}
                 [:form {:id "main-form" :class "form-horizontal col-sm-12 col-md-10 col-lg-9"}

                  [:div {:class "form-group"}
                   [:label {:class "col-sm-2 control-label" :for "input-web-meta-subject"} "Метатег subject"]
                   [:div {:class "col-sm-10"}
                    [:input {:id "input-web-meta-subject" :class "form-control"
                             :placeholder "тема", :type "text"
                             :value (get-in row [:webdoc-row :web_meta_subject] "")}]]]

                  [:div {:class "form-group"}
                   [:label {:class "col-sm-2 control-label" :for "input-web-meta-keywords"} "Метатег keywords"]
                   [:div {:class "col-sm-10"}
                    [:textarea {:id "input-web-meta-keywords" :class "form-control"
                                :placeholder "ключевые слова", :rows 3}
                     (get-in row [:webdoc-row :web_meta_keywords] "")]]]

                  [:div {:class "form-group"}
                   [:label {:class "col-sm-2 control-label" :for "input-web-meta-description"} "Метатег description"]
                   [:div {:class "col-sm-10"}
                    [:textarea {:id "input-web-meta-description" :class "form-control"
                                :placeholder "описание", :rows 4}
                     (get-in row [:webdoc-row :web_meta_description] "")]]]

                  [:div {:class "form-group"}
                   [:label {:class "col-sm-2 control-label" :for "input-web-meta-subject"} "Аватар"]
                   [:div {:class "col-sm-10"}
                    (input-one-image :avatar (get-in row [:webdoc-row :web_title_image]))]]

                  [:div {:class "form-group"}
                   [:label {:class "col-sm-2 control-label" :for "input-web-top-description"} "Краткое описание"]
                   [:div {:class "col-sm-10"}
                    [:textarea {:id "input-web-top-description" :class "form-control"
                                :placeholder "описание в заголовке", :rows 4}
                     (get-in row [:webdoc-row :web_top_description] "")]]]

                  [:div {:class "form-group"}
                   [:label {:class "col-sm-2 control-label" :for "input-web-citems"} "Структура оглавния"]
                   [:div {:class "col-sm-10"}
                    [:textarea {:id "input-web-citems" :class "form-control"
                                :placeholder "Структура оглавления (#p1 > Якорь 1; #p1 > Якорь 2; #p3 > Якорь 3;.....)"
                                :rows 3}
                     (get-in row [:webdoc-row :web_citems] "")]]]


                  [:div {:class "form-group"}
                   [:label {:class "col-sm-2 control-label" :for "input-web-description"} "Полное описание"]
                   [:div {:class "col-sm-10" :style "color: black"}
                    [:textarea {:id "inputckedit1" :class "form-control ckeditor"
                                ;;.ckeditor
                                ;;:data-provide "markdown"
                                :placeholder "полное описание", :rows 10 }
                     (get-in row [:webdoc-row :web_description] "")]]]

                  ;; For mardown
                  [:script {:type "text/javascript"} "CKEDITOR.replace('inputckedit1');"]
                  ;;[:script {:type "text/javascript"} "$('#inputckedit1').markdown({autofocus:false,savable:false})"]
                  ]]

                ;; Panel - Fotos
                [:div {:id "tab-4" :class "col-sm-12 col-md-12 col-lg-12" :style "display: none"}]

                ;; Panel - Files
                [:div {:id "tab-5" :class "col-sm-12 col-md-12 col-lg-12" :style "display: none"}]

                ;; необходимо помещать во внутреннюю чать проматываемого тега
                [:div {:id "modal-1"}]
                ]]
              )


             ;; first update
             ;; !!!
             ;; Отработка справочных данных. обязательно только после полной отрисовки интерфейса, иначе обновлять будет нечего
             ;; !!!
             ;;(com/chan-rb-data-repaint (:rb-data row))

             (put! chan-switch-tab (@page-state :tab))
             (println "REPAINT END")
             (put! chan-do-after-repaint 1)
             )))))


    ;; tab
    (let [tabs-map-0 {0 [:tab-label-0 :tab-toolbar-0 :tab-0]
                      2 [:tab-label-2 :tab-toolbar-2 :tab-2]
                      3 [:tab-label-3 :tab-toolbar-3 :tab-3]
                      4 [:tab-label-4 :tab-toolbar-4 :tab-4]
                      5 [:tab-label-5 :tab-toolbar-5 :tab-5]
                      }

          tabs-map (if (get-in specific [specific-tab1-key])
                     (assoc tabs-map-0 1 [:tab-label-1 :tab-toolbar-1 :tab-1])
                     tabs-map-0)
          ]
      (go
        (while true
          (let [i (<! chan-switch-tab)
                [i dis] (if (-> @page-state :id nil?)
                          [0 #{1 2 3 4 5}]
                          [i #{}])]
            (println i)
            (doseq [[ti [label & ee]] (seq tabs-map)
                    :let [e-label (ix/by-id label)]]
              (do
                (if (= ti i)
                  (dommy/add-class! e-label :active)
                  (dommy/remove-class! e-label :active))

                (if (contains? dis ti)
                  (dommy/add-class! e-label :disabled)
                  (dommy/remove-class! e-label :disabled))

                (doseq [e ee :let [e-e (ix/by-id e)]]
                  (if (= ti i)
                    (dommy/show! e-e)
                    (dommy/hide! e-e)))

                (swap! page-state assoc :tab i)))

            ;;;;(page-main/main-set-padding-top-by-navbar)

            (condp = i
              ;; TAGS
              2 (put! chan-update-tags 1)
              ;; IMAGES LOAD...
              4 (put! chan-load-images 1)
              ;; FILES LOAD...
              5 (put! chan-load-files 1)
              (println "Выбрана обычная вкладка " i))

            ))))

    ;; Канал сохранения основных свойств
    (go
      (while true
        (let [_ (<! chan-save-common)]
          (when-let [row (-> (if-let [id (@page-state :id)] {:id id} {})
                             (ix/input-validate-and-assoc :keyname :input-keyname [ix/validate-for-not-empty])
                             (assoc :description (-> :input-description ix/by-id dommy/value))
                             (assoc :url1flag (-> :input-url1flag ix/by-id .-checked))
                             (assoc :url1 (-> :input-url1 ix/by-id dommy/value))

                             (as-> row
                                 (if-let [f (get-in specific [specific-common-key specific-common-save-fn-key])]
                                   (f row)
                                   row))

                             (#(do (println "ROW FOR SAVING:"  %) %))

                             ix/input-all-valid-or-nil)]
            (ix/ajax-post-json
             "/tc/rb/webdocs/save"
             {:webdoc-row row
              :webdoctag-ids-for-updating  (when (not (:id row))
                                             (when-let [tag-id (@page-state :tag-id)]
                                               [tag-id]))}
             (fn [response]
               (swap! page-state assoc :id (-> response :webdoc-row :id))
               (put! chan-repaint 1)
               (ix/display-message-on-time 2000 "Запись успешно сохранена")
               ))))))

    ;; Канал сохранения web свойств
    (go
      (while true
        (let [_ (<! chan-save-web)]
          (when-let [row (-> {:id (@page-state :id)
                              :web_meta_subject (dommy/value (ix/by-id :input-web-meta-subject))
                              :web_meta_keywords (dommy/value (ix/by-id :input-web-meta-keywords))
                              :web_meta_description (dommy/value (ix/by-id :input-web-meta-description))
                              :web_top_description (dommy/value (ix/by-id :input-web-top-description))
                              :web_description (-> js/CKEDITOR .-instances (aget "inputckedit1") .getData)
                              ;;:web_description (dommy/value (ix/by-id :inputckedit1))
                              :web_citems (dommy/value (ix/by-id :input-web-citems))
                              }
                             ;;Расскоментировать если будет валидиция
                             ;;ix/input-all-valid-or-nil
                             )]

            (ix/ajax-post-json
             "/tc/rb/webdocs/save"
             {:webdoc-row row }
             (fn [response]
               (println "SAVE WEB! " (-> (ix/xml-id "-uploader" :avatar) ix/by-id .-files .-length))
               (when (-> (ix/xml-id "-uploader" :avatar) ix/by-id .-files .-length (= 1))
                 (ix-io/file-upload
                  (ix/by-id :avatar)
                  (str "/tc/rb/webdocs/upload/" (@page-state :id) "/image/avatar")
                  {:success #(println "AVATAR UPLOAD SUCCES!!!")}))

               (ix/display-message-on-time 2000 "Запись сохранена успешно"))
             )))))


    ;; cgtag
    (go
      (while true
        (let [_ (<! chan-update-tags)]
          (ix/ajax-post-json
           "/tc/rb/webdocs/webdoctags-edit-table"
           {:id (@page-state :id)}
           (fn [webdoctag-edit-table]
             (let [keyname-loockup (->> webdoctag-edit-table
                                        (reduce into)
                                        (reduce #(assoc % (%2 :id) %2) {}))]

               (ix/clear-and-set-on-tag-by-id
                :tags-scroll-buttons
                [:ul {:class "nav nav-pills"}
                 (map
                  (fn [g]
                    (let [{:keys [id tagname]} (first g)]
                      [:li {:role "presentation"}
                       [:a {:href (str "#gr-" id)} tagname]]))
                  webdoctag-edit-table)
                 ])

               (ix/clear-and-set-on-tag-by-id
                :tags-list
                (->> webdoctag-edit-table
                     (map (fn [tags-group]
                            (list
                             [:a {:name (str "gr-" (-> tags-group first :id)) :style "display:block;height:170px;margin-top:-170px;visibility:hidden;"}]
                             [:table {:class "table table-striped table-condensed"}
                              [:thead [:tr [:th]]]
                              [:tbody
                               (map (fn [{tag-id :id contain? :contain? path :path :as tag}]
                                      (let [[this-tag-id & path-to] (reverse path)
                                            {:keys [tagname const]} (keyname-loockup this-tag-id)
                                            css-const (if const "text-warning" "text-primary")]

                                        (when contain? (swap! tags-groups-state conj tag-id))

                                        [:tr {:on-click #(this-as this (put! chan-toggle-tag [this tag-id]))
                                              :class (if contain? "success" "")
                                              :style "cursor: pointer"}
                                         [:td (reduce
                                               (fn [a x]
                                                 (let [{:keys [tagname const]} (keyname-loockup x)
                                                       css-const (if const "text-warning" "text-success")]
                                                   (conj a [:span {:class css-const}
                                                            [:span {:class (str "glyphicon glyphicon-asterisk " css-const)
                                                                    :aria-hidden "true" :style "margin-left: 10px"}]
                                                            "    " tagname])))
                                               (list [:span {:class css-const}
                                                      [:span {:class (str "glyphicon glyphicon-asterisk " css-const)
                                                              :aria-hidden "true" :style "margin-left: 10px"}]
                                                      "    " tagname])
                                               path-to)
                                          ]
                                         ]))
                                    tags-group)
                               ]]
                             )))))))))))

    (go
      (while true
        (let [[this id] (<! chan-toggle-tag)]
          (if (contains? @tags-groups-state id)
            (do
              (swap! tags-groups-state disj id)
              (dommy/remove-class! this :success))
            (do
              (swap! tags-groups-state conj id)
              (dommy/add-class! this :success))))))

    ;; Канал работы с тегами
    (go
      (while true
        (let [_ (<! chan-save-tags)]
          (ix/ajax-post-json
           "/tc/rb/webdocs/save"
           {:webdoc-row {:id (@page-state :id)}
            :webdoctag-ids-for-updating (vec @tags-groups-state)}
           (fn [response]
             (println "SAVE CGTAGS!")
             (ix/display-message-on-time 2000 "Запись сохранена успешно"))))))



    ;; IMAGES *******************************************************************************************************
    (go
      (while true
        (let [_ (<! chan-load-images)
              id (@page-state :id)]
          (when (and id (= 4 (@page-state :tab)))
            (println "Start load images!")
            (ix/ajax-post-json
             "/tc/rb/webdocs/images-list"
             {:id (@page-state :id)}
             (fn [response]
               (let [images-list (sel1 :#tab-4)]
                 (dommy/clear! images-list)
                 (->> response
                      (map (fn [{:keys [id path top_description description galleria] :as row}]
                             [:div {:class "col-xs-3 col-md-3"}
                              [:div {:class "thumbnail"
                                     :on-click #(put! chan-select-image-action row) :style "cursor: pointer"}
                               (when galleria
                                 [:span {:class "glyphicon glyphicon-film" :style "position:absolute;top:10px;left:5px;font-size:2em" :aria-hidden "true"}])
                               [:a [:img {:src (str "/image/" path) :alt "фото"}]]

                               [:div {:class "caption"}
                                (when (not (clojure.string/blank? top_description))
                                  [:h3 top_description])
                                [:div
                                 (when (not (clojure.string/blank? description))
                                   (list description [:br]))
                                 [:span {:class "label label-default"} "URL"]" "
                                 [:input {:type "text" :style "width:70%;font-size:0.7em"
                                          :value (str "/image/" path)
                                          :on-mousedown #(this-as this (.select this))}]]
                                ]]]))
                      (partition-all 4)
                      (map (partial conj [:div {:class "row"}]))
                      hipo/create
                      (dommy/append! images-list))))
             )))))

    ;; Канал работы с картинками
    (go
      (while true
        (let [_ (<! chan-add-image)
              form-e (ix/by-id :image-uploader-form)]
          (ix-io/file-upload
           form-e (str "/tc/rb/webdocs/upload/" (@page-state :id) "/image")
           {:success #(do (put! chan-load-images 1)
                          (println "IMAGE UPLOAD SUCCES!!!"))}
           ))))

    (go
      (while true
        (let [row (<! chan-select-image-action)]
          (println "[*]" row)
          (ix/modal-cr-and-show-on-id
           :modal-1
           {:title (str "Операции с запипсью #" (:id row))
            :body [:div {:class "well center-block" :style "max-width:400px;"}
                   [:a {:class "btn btn-info btn-lg btn-block"
                        :target "_blank" :href (str "/file/" (:path row))
                        :type "button" }
                    "открыть в другой вкладке"]
                   [:button {:class "btn btn-primary btn-lg btn-block"
                             :role "button" :data-dismiss "modal"
                             :on-click #(put! chan-edit-image-row row)}
                    "Редактировать"]
                   [:button {:class "btn btn-danger btn-lg btn-block"
                             :type "button" :data-dismiss "modal"
                             :on-click #(put! chan-delete-image-row row)}
                    "Удалить"]]
            :footer [:button {:class "btn btn-default" :data-dismiss "modal", :type "button"}
                     "Закрыть"]}))))

    (go
      (while true
        (let [row (<! chan-edit-image-row)]
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
                              (put! chan-load-images 1)
                              (println "IMAGE EDITED!"))))}
              "Сохранить"]

             [:button {:class "btn btn-default" :data-dismiss "modal", :type "button"} "Закрыть"])
            }
           ))))



    (go
      (while true
        (let [row (<! chan-delete-image-row)]
          (println "Delete image row! " row )

          (ix/modal-cr-and-show-on-id
           :modal-1
           {:title (str "Удаление записи #" (:id row))
            :body  [:div {:class "row"}
                    [:img {:class "thumbnail col-sm-4 col-sm-offset-4"
                           :src (str "/image/" (row :path)) :alt "фото"}]]
            :footer (list
                     [:button {:class "btn btn-danger btn-lg"
                               :type "button" :data-dismiss "modal"
                               :on-click (fn []
                                           (ix/ajax-post-json
                                            "/tc/rb/webdocs/files_rel/delete"
                                            {:webdoc-id (@page-state :id)
                                             :file-id (row :id) :entity-key :webdoc}
                                            (fn [response]
                                              (put! chan-load-images 1)
                                              (println "IMAGE DELETED!")) ))}
                      "Удалить!"]
                     [:button {:class "btn btn-default" :data-dismiss "modal", :type "button"} "Закрыть"])
            }))))



    ;; FILES *******************************************************************************************************
    ;; Like images

    (go
      (while true
        (let [_ (<! chan-load-files)
              id (@page-state :id)]
          (when (and id (= 5 (@page-state :tab)))
            (println "Start load files!")
            (ix/ajax-post-json
             "/tc/rb/webdocs/files-list"
             {:id (@page-state :id)}
             (fn [response]
               (let [files-list (sel1 :#tab-5)]
                 (dommy/clear! files-list)
                 (->> response
                      (map (fn [{:keys [id path top_description description] :as row}]
                             [:div {:class "col-xs-3 col-md-3"}
                              [:div {:class "thumbnail"
                                     :on-click #(put! chan-select-file-action row)
                                     :style "cursor:pointer;min-height:75px;"}

                               [:span {:class "glyphicon glyphicon-file"
                                       :style "font-size:5em;float:left" :aria-hidden "true"}]

                               [:div {:class "caption"}
                                (when (not (clojure.string/blank? top_description))
                                  [:h3 top_description])
                                [:div
                                 (when (not (clojure.string/blank? description))
                                   (list description [:br]))
                                 [:div {:style ""}
                                  [:span {:class "label label-default" } "URL"]" "
                                  [:input {:type "text" :style "width:50%;font-size:0.7em"
                                           :value (str "/file/" path)
                                           :on-mousedown #(this-as this (.select this))}]]]
                                ]]]))
                      (partition-all 4)
                      (map (partial conj [:div {:class "row"}]))
                      hipo/create
                      (dommy/append! files-list))))
             )))))




    ;; Канал работы с картинками
    (go
      (while true
        (let [_ (<! chan-add-file)
              form-e (ix/by-id :file-uploader-form)]
          (ix-io/file-upload
           form-e (str "/tc/rb/webdocs/upload/" (@page-state :id) "/file")
           {:success #(do (put! chan-load-files 1)
                          (println "FILE UPLOAD SUCCES!!!"))}
           ))))

    (go
      (while true
        (let [row (<! chan-select-file-action)]
          (println "[*]" row)
          (ix/modal-cr-and-show-on-id
           :modal-1
           {:title (str "Операции с запипсью #" (:id row))
            :body [:div {:class "well center-block" :style "max-width:400px;"}
                   [:a {:class "btn btn-info btn-lg btn-block"
                        :target "_blank" :href (str "/file/" (:path row))
                        :type "button" }
                    "открыть в другой вкладке"]
                   [:button {:class "btn btn-primary btn-lg btn-block"
                             :role "button" :data-dismiss "modal"
                             :on-click #(put! chan-edit-file-row row)}
                    "Редактировать"]
                   [:button {:class "btn btn-danger btn-lg btn-block"
                             :type "button" :data-dismiss "modal"
                             :on-click #(put! chan-delete-file-row row)}
                    "Удалить"]]
            :footer [:button {:class "btn btn-default" :data-dismiss "modal", :type "button"}
                     "Закрыть"]}))))

    (go
      (while true
        (let [row (<! chan-edit-file-row)]
          (println "Edit file row! " row)

          (ix/modal-cr-and-show-on-id
           :modal-1
           {:title (str "Редактирование записи #" (:id row))
            :body [:form {:id "main-form" :class "form-horizontal"}

                   [:div {:class "form-group"}
                    [:label {:class "col-sm-2 control-label" :for "input-file-top-description"}
                     "Название"]
                    [:div {:class "col-sm-10"}
                     [:input {:id "input-file-top-description" :class "form-control"
                              :placeholder "Название...", :type "text"
                              :value (row :top_description)}]]]


                   [:div {:class "form-group"}
                    [:label {:class "col-sm-2 control-label" :for "input-file-description"}
                     "Описание"]
                    [:div {:class "col-sm-10"}
                     [:textarea {:id "input-file-description" :class "form-control"
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
                             :top_description (dommy/value (sel1 :#input-file-top-description))
                             :description (dommy/value (sel1 :#input-file-description))}
                            (fn [response]
                              (put! chan-load-files 1)
                              (println "IMAGE EDITED!")))) }
              "Сохранить"]

             [:button {:class "btn btn-default" :data-dismiss "modal", :type "button"} "Закрыть"])
            }
           ))))

    (go
      (while true
        (let [row (<! chan-delete-file-row)]
          (println "Delete file row! " row )

          (ix/modal-cr-and-show-on-id
           :modal-1
           {:title (str "Удаление записи #" (:id row))
            :body  [:div {:class "row"}
                    [:img {:class "thumbnail col-sm-4 col-sm-offset-4"
                           :src (str "/file" (row :path)) :alt "фото"}]]
            :footer (list
                     [:button {:class "btn btn-danger btn-lg"
                               :type "button" :data-dismiss "modal"
                               :on-click (fn []
                                           (ix/ajax-post-json
                                            "/tc/rb/webdocs/files_rel/delete"
                                            {:webdoc-id (@page-state :id)
                                             :file-id (row :id) :entity-key :webdoc}
                                            (fn [response]
                                              (put! chan-load-files 1)
                                              (println "IMAGE DELETED!")))) }
                      "Удалить!"]
                     [:button {:class "btn btn-default" :data-dismiss "modal", :type "button"} "Закрыть"])
            }))))

    {:chan-show-dialog chan-show-dialog
     :chan-repaint chan-repaint}
    ))
