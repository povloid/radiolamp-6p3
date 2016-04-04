(ns ix.omut.component.file-edit-form
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.component.input :as input]
            [ix.omut.component.textarea :as textarea]
            [ix.omut.component.toggle-button :as toggle-button]
            [ix.omut.component.edit-form-for-id :as edit-form-for-id]))




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
                              (assoc-in [:description :value] description) ))))
                     :app-to-row-fn
                     (fn []
                       {:id              (get @app :id)
                        :top_description (get-in @app [:top_description :value])
                        :description     (get-in @app [:description :value])})
                     :form-body
                     (dom/fieldset
                      nil
                      (dom/legend nil "Основные данные")

                      (om/build input/component-form-group (get-in app [:top_description])
                                {:opts {:label "Наименование"}})

                      (om/build textarea/component-form-group (get-in app [:description])
                                {:opts {:label "Описание"}})
                      )
                     })
        }))))
