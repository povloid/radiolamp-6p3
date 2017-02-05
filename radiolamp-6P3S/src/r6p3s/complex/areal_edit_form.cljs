(ns r6p3s.complex.areal-edit-form
    (:require [cljs.core.async :refer [put! chan <!]]
              [om.core :as om :include-macros true]
              [om.dom :as dom :include-macros true]
              [r6p3s.net :as rnet]
              [r6p3s.core :as rc]
              [r6p3s.common-input :as common-input]
              [r6p3s.cpt.input :as input]
              [r6p3s.cpt.textarea :as textarea]
              [r6p3s.cpt.edit-form-for-id :as edit-form-for-id]
              [r6p3s.cpt.one-image-uploder :as one-image-uploader]))


(def app-init
  (merge edit-form-for-id/app-init
         {:parent_id   nil
          :keyname     (assoc input/app-init :has-warning? true)
          :description textarea/app-init
          :logotype    one-image-uploader/app-init
          }))


(defn component [app _ opts]
  (reify
    om/IRender
    (render [_]
      (om/build
       edit-form-for-id/component app
       {:opts
        (merge opts
               {:uri "/camerton/rb/areal/find"
                :fill-app-fn
                (fn [row]
                  (om/transact!
                   app
                   (fn [app]
                     (-> app
                         ;; Заполнение формы
                         ;;(assoc :id id) ;; Id идет сразу, будет работатьт автоматом
                         ;;(assoc :parent_id               (get-in row [:parent_id]))
                         (assoc-in [:keyname :value]     (get-in row [:keyname] ""))
                         (assoc-in [:description :value] (get-in row [:description] ""))
                         (assoc-in [:logotype :image]    (get-in row [:logotype] ""))))))

                :uri-save "/camerton/rb/areal/save"
                :app-to-row-fn
                (fn []
                  (let [app-v @app]
                    {:row (-> (if-let [id (app-v :id)] {:id id} {})
                              (assoc
                               :parent_id     (app-v :parent_id)
                               :keyname       (get-in app-v [:keyname :value])
                               :description   (get-in app-v [:description :value])
                               :logotype      (get-in app-v [:logotype :image])))
                     }))

                :form-body
                (dom/fieldset
                 nil
                 (dom/legend nil "Основные данные " (str (@app :parent_id)))

                 (om/build input/component-form-group (get-in app [:keyname])
                           {:opts {:label      "Наименование"
                                   :spec-input {:onChange-valid?-fn
                                                common-input/vldfn-not-empty}}})

                 (om/build one-image-uploader/component-form-group (get-in app [:logotype])
                           {:opts {:label "Логотип"
                                   :spec-one-image-uploader
                                   {:uri "/camerton/upload/image"}}})

                 (om/build textarea/component-form-group (get-in app [:description])
                           {:opts {:label "Описание"}})


                 )
                })}))))
