(ns r6p3s.complex.page-edit-form
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.common-input :as common-input]
            [r6p3s.core :as rc]
            [r6p3s.net :as rnet]
            [r6p3s.cpt.edit-form-for-id :as edit-form-for-id]
            [r6p3s.cpt.file-view :as files-view]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.nav-tabs :as nav-tabs]
            [r6p3s.cpt.textarea :as textarea]
            [r6p3s.cpt.markdown-editor :as markdown-editor]
            [r6p3s.cpt.input-select-checkboxes :as input-select-checkboxes]
            [r6p3s.cpt.thumbinals-view :as thumbinals-view]
            [r6p3s.ui.nav-tab :as nav-tab]))




(def page-common-idx 0)
(def page-images-idx 1)
(def page-files-idx  2)


(def app-init
  (merge edit-form-for-id/app-init
         {:tabs (-> nav-tabs/app-state
                    (assoc :tabs
                           (nav-tabs/app-state-i-maker
                            {page-common-idx {:text "Общ."}
                             page-images-idx {:text "Фото"}
                             page-files-idx  {:text "Файлы"}})))

          :common {:keyname       (assoc input/app-init :has-warning? true)
                   :keywords      textarea/app-init
                   :web_content   markdown-editor/app-init
                   :description   textarea/app-init
                   :content-types input-select-checkboxes/app-init}
          :images thumbinals-view/app-init
          :files  files-view/app-init}))


(defn component [app owner {:keys [uri chan-load-for-id meta-page]
                            :or {uri ""}
                            :as opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-update        (chan)
       :chan-save          (chan)
       :chan-images-update (chan)
       :chan-files-update  (chan)})
    om/IWillMount
    (will-mount [this]
      (let [{:keys [chan-update
                    chan-images-update
                    chan-files-update]} (om/get-state owner)]

        (go ;; Обработка отрисовки табулятороров и переключения на страници
          (while true
            (let [i  (<! chan-update)
                  id (:id @app)]
              (println "Переключение на " i " :id = " id)

              (if (nil? id)
                (nav-tabs/enable-inly-one (:tabs app) page-common-idx)

                (do
                  (nav-tabs/enable-all (:tabs app))
                  ;; Обновление нужных страниц по запросу
                  (condp = i
                    page-images-idx (put! chan-images-update {:id id})
                    page-files-idx  (put! chan-files-update {:id id})
                    nil))) )))

        (put! chan-update page-common-idx)))

    om/IRenderState
    (render-state [_ {:keys [chan-update
                             chan-save
                             chan-images-update
                             chan-files-update]}]

      (dom/div
       nil
       (dom/div #js {:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"}
                (om/build nav-tabs/component (:tabs app)
                          {:opts {:chan-update chan-update
                                  :type        :tabs
                                  :justified?  true}}))

       (nav-tab/render
        (@app :tabs) page-common-idx
        (om/build
         edit-form-for-id/component app
         {:opts
          (merge opts
                 {:chan-save chan-save
                  :uri       (str uri "/page/find")
                  :fill-app-fn
                  (fn [{:keys [content_type direct_type] :as row}]
                    (let [content-type (reduce #(->> %2 :keyname (conj %1)) #{} content_type)
                          direct-type  (reduce #(->> %2 :keyname (conj %1)) #{} direct_type)]
                      (om/transact!
                       app
                       (fn [app]
                         (-> app
                             ;; Заполнение формы
                             (assoc-in [:common :keyname :value]     (get-in row [:keyname] ""))
                             (assoc-in [:common :description :value] (get-in row [:description] ""))
                             (assoc-in [:common :keywords :value]    (get-in row [:keywords] ""))
                             (assoc-in [:common :web_content :value] (get-in row [:web_content] ""))
                             (update-in [:common :content-types]
                                        input-select-checkboxes/make-data
                                        :title :selected
                                        (->> meta-page
                                             :content-type
                                             (map (fn [[k row]]
                                                    (assoc row
                                                           :keyname k
                                                           :selected (contains? content-type k))))))))))
                    (put! chan-update (nav-tabs/active-tab (@app :tabs))))

                  :uri-save (str uri "/page/save")
                  :app-to-row-fn
                  (fn []
                    {:row (-> (if-let [id (@app :id)] {:id id} {})
                              (assoc
                               :keyname         (get-in @app [:common :keyname :value])
                               :keywords        (get-in @app [:common :keywords :value])
                               :web_content     (get-in @app [:common :web_content :value])
                               :description     (get-in @app [:common :description :value])
                               :content-types   (->> @app :common :content-types
                                                     input-select-checkboxes/get-selected
                                                     (map :keyname))))
                     })

                  :post-save-fn
                  (fn [{id :id external_id :external_id :as result}]
                    (om/update! app :id id)
                    (om/update! app [:common :external_id :value] external_id)
                    (put! chan-update 0))

                  :form-body
                  (dom/fieldset
                   nil
                   (dom/legend nil "Основные данные")

                   (om/build input/component-form-group (get-in app [:common :keyname])
                             {:opts {:label      "Наименование"
                                     :spec-input {:onChange-valid?-fn
                                                  common-input/vldfn-not-empty}}})

                   (om/build textarea/component-form-group (get-in app [:common :description])
                             {:opts {:label "Описание"}})

                   (om/build textarea/component-form-group (get-in app [:common :keywords])
                             {:opts {:label "Ключевые слова"}})

                   (om/build input-select-checkboxes/component-form-group (get-in app [:common :content-types])
                             {:opts {:label "Тип контента"}})

                   (om/build markdown-editor/component-form-group (get-in app [:common :web_content])
                             {:opts {:label "Контент страници"}})

                   (dom/button #js {:className "btn btn-primary" :type "button"
                                    :onClick   (fn [_]
                                                 (put! chan-save 1)
                                                 1)}
                               "Сохранить"))
                  })
          }))


       ;; Картинки
       (nav-tab/render
        (@app :tabs) page-images-idx
        (dom/div #js {:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                      :style     #js {:marginTop 6}}
                 (om/build thumbinals-view/component (:images app)
                           {:opts {:uri         (str uri "/page/images-list")
                                   :uri-upload  (str uri "/page/upload/")
                                   :uri-delete  (str uri "/page/files_rel/delete")
                                   :chan-update chan-images-update
                                   }})))

       ;; Файлы
       (nav-tab/render
        (@app :tabs) page-files-idx
        (dom/div #js {:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                      :style     #js {:marginTop 6}}
                 (om/build files-view/component (:files app)
                           {:opts {:uri         (str uri "/page/files-list")
                                   :uri-upload  (str uri "/page/upload/")
                                   :uri-delete  (str uri "/page/files_rel/delete")
                                   :chan-update chan-files-update
                                   }})))
       ))))
