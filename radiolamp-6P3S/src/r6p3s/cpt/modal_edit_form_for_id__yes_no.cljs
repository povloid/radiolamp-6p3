(ns r6p3s.cpt.modal-edit-form-for-id--yes-no
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.ui.button :as button]
            [r6p3s.cpt.modal :as modal]
            [r6p3s.cpt.edit-form-for-id :as edit-form-for-id]))




(def app-init
  (merge modal/app-init edit-form-for-id/app-init))

(defn component [app _ {:keys [new-or-edit-fn?
                               edit-form-for-id]
                        :or   {edit-form-for-id
                               (fn [_ _]
                                 (reify
                                   om/IRender
                                   (render [_]
                                     (dom/h1 nil "Форма диалога еще не указана"))))
                               }
                        :as   opts}]
  (reify
    om/IInitState
    (init-state [_]
      {:chan-save (chan)})
    om/IRenderState
    (render-state [_ {:keys [chan-save]}]
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
                        (dom/div #js {:className "row"}
                                 (om/build edit-form-for-id app
                                           {:opts (assoc opts
                                                         :chan-save chan-save
                                                         :post-save-fn
                                                         (fn [r]
                                                           (modal/hide app)
                                                           (when-let [post-save-fn-2 (:post-save-fn opts)]
                                                             (post-save-fn-2 r))))}))
                        :footer
                        (dom/div nil
                                 (button/render {:type     :primary
                                               :on-click (fn [_]
                                                           (put! chan-save 1)
                                                           1)
                                               :text     "Принять"})
                                 (button/render {:type     :default
                                               :on-click (fn [_] (modal/hide app) 1)
                                               :text     "Отмена"})
                                 )
                        }}))))
