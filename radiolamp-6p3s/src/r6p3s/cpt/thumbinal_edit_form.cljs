(ns r6p3s.cpt.thumbinal-edit-form
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]

            [r6p3s.net :as rnet]
            [r6p3s.core :as c]
            [r6p3s.cpt.input :as input]
            [r6p3s.cpt.textarea :as textarea]
            [r6p3s.cpt.toggle-button :as toggle-button]
            [r6p3s.cpt.edit-form-for-id :as edit-form-for-id]))


(def app-init
  (merge edit-form-for-id/app-init
         {:top_description input/app-init
          :description     textarea/app-init
          :galleria        toggle-button/app-init
          }))


(defn component [app owner opts]
  (reify
    om/IRender
    (render [_]
      (om/build
       edit-form-for-id/component app
       {:opts
        (merge opts {:fill-app-fn
                     (fn [{:keys [top_description
                                  description
                                  galleria]}]
                       (om/transact!
                        app
                        (fn [app]
                          (-> app
                              ;; Заполнение формы
                              (assoc-in [:top_description :value] top_description)
                              (assoc-in [:description :value] description)
                              (assoc-in [:galleria :value] galleria)))))
                     :app-to-row-fn
                     (fn []
                       {:id              (get @app :id)
                        :top_description (get-in @app [:top_description :value])
                        :description     (get-in @app [:description :value])
                        :galleria        (get-in @app [:galleria :value])})
                     :form-body
                     (dom/fieldset
                      nil
                      (dom/legend nil "Основные данные")

                      (om/build input/component-form-group (get-in app [:top_description])
                                {:opts {:label "Наименование"}})

                      (om/build toggle-button/component-form-group (get-in app [:galleria])
                                {:opts {:label "Отображать в галерее"}})

                      (om/build textarea/component-form-group (get-in app [:description])
                                {:opts {:label "Описание"}})
                      )
                     })
        }))))
