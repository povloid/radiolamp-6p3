(ns r6p3s.complex.scontent-edit-form
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
            [r6p3s.ui.media :as media]
            [r6p3s.cpt.markdown-editor :as markdown-editor]
            [r6p3s.cpt.thumbinals-view :as thumbinals-view]
            [r6p3s.ui.nav-tab :as nav-tab]))



(def page-common-idx 0)
(def page-images-idx 1)
(def page-files-idx  2)


(def app-init
  (merge edit-form-for-id/app-init
         {:keyname     nil
          :ctype       nil
          :title       nil
          :description nil
          :tabs        (-> nav-tabs/app-state
                           (assoc :tabs
                                  (nav-tabs/app-state-i-maker
                                   {page-common-idx {:text "Общ."}
                                    page-images-idx {:text "Фото"}
                                    page-files-idx  {:text "Файлы"}})))

          :common {:keyname (assoc input/app-init :has-warning? true)
                   :v_text  markdown-editor/app-init}
          :images thumbinals-view/app-init
          :files  files-view/app-init}))


(defn component [app owner {:keys [chan-load-for-id
                                   uri]
                            :or   {uri ""}
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

       (let [{:keys [keyname title description]} @app]
         (media/render
          {:heading (dom/h4 #js {:className "text-primary"}
                            (dom/b nil (or (str keyname) "БЕЗ ТИПА")) ": " title)
           :body    (dom/p nil description)}))


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
                  :uri       (str uri "/scontent/find")
                  :fill-app-fn
                  (fn [row]
                    (om/transact!
                     app
                     (fn [app]
                       (-> app
                           ;; Заполнение формы
                           (assoc :keyname     (row :keyname ""))
                           (assoc :ctype       (row :ctype ""))
                           (assoc :title       (row :title ""))
                           (assoc :description (row :description ""))
                           (assoc-in [:common :v_text :value] (row :v_text "")))))
                    (put! chan-update (nav-tabs/active-tab (@app :tabs))))

                  :uri-save (str uri "/scontent/save")
                  :app-to-row-fn
                  (fn []
                    {:row (-> (if-let [id (@app :id)] {:id id} {})
                              (assoc
                               :v_text (get-in @app [:common :v_text :value])))
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

                   (om/build markdown-editor/component-form-group (get-in app [:common :v_text])
                             {:opts {:label "Значение"}})

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
                           {:opts {:uri         (str uri "/scontent/images-list")
                                   :uri-upload  (str uri "/scontent/upload/")
                                   :uri-delete  (str uri "/scontent/files_rel/delete")
                                   :chan-update chan-images-update
                                   }})))

       ;; Файлы
       (nav-tab/render
        (@app :tabs) page-files-idx
        (dom/div #js {:className "col-xs-12 col-sm-12 col-md-12 col-lg-12"
                      :style     #js {:marginTop 6}}
                 (om/build files-view/component (:files app)
                           {:opts {:uri         (str uri "/scontent/files-list")
                                   :uri-upload  (str uri "/scontent/upload/")
                                   :uri-delete  (str uri "/scontent/files_rel/delete")
                                   :chan-update chan-files-update
                                   }})))
       ))))
