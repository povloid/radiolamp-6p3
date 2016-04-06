(ns r6p3s.cpt.modal-edit-form-for-id--close
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.ui.button :as button]
            [r6p3s.cpt.edit-form-for-id :as edit-form-for-id]
            [r6p3s.cpt.modal :as modal]))



(def app-init
  (merge modal/app-init edit-form-for-id/app-init))



(defn component [app _ {:keys [new-or-edit-fn?
                               edit-form-for-id
                               post-save-fn]
                        :or   {edit-form-for-id
                               (fn [_ _]
                                 (reify
                                   om/IRender
                                   (render [_]
                                     (dom/h1 nil "Форма диалога еще не указана"))))
                               }
                        :as   opts}]
  (reify
    om/IRender
    (render [_]
      (om/build modal/component app
                {:opts {:modal-size :lg
                        :label      (if new-or-edit-fn?
                                      (condp = (new-or-edit-fn?)
                                        :new  "Создание новой записи"
                                        :edit "Редактирование записи"
                                        "???")
                                      "Редактирование записи")
                        ;;:modal-size :lg
                        :body
                        (dom/div
                         #js {:className "row"}
                         (om/build edit-form-for-id/component app {:opts opts}))
                        :footer
                        (button/render {:type     :default
                                        :on-click (fn [_]
                                                    (when post-save-fn
                                                      (post-save-fn {}))
                                                    (modal/hide app) 1)
                                        :text     "Закрыть"})}}))))
