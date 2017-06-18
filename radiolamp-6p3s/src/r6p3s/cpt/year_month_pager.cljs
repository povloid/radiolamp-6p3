(ns r6p3s.cpt.year-month-pager
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.ui.button :as button]
            [r6p3s.ui.glyphicon :as glyphicon]))

(defn app-init-fn [d]
  {:yyyy (.getFullYear d)
   :mm   (inc (.getMonth d))})

(def app-init
  (app-init-fn (new js/Date)))



(defn component [app _ {:keys [chan-update
                               update-fn]}]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [yyyy mm]} @app]
        (dom/div #js {:className "input-group"
                      :style     #js {:textAlign "center"
                                      :maxWidth  500
                                      :float     "none"
                                      :margin    "0 auto"}}
                 (dom/span #js {:className "input-group-btn"}
                           (button/render
                            {:type     :default
                             :title    "месяц назад"
                             :on-click (fn [_]
                                         (let [[yyyy mm] (if (= mm 1)
                                                            [(dec yyyy) 12]
                                                            [yyyy (dec mm)])
                                               ]
                                           (om/transact! app #(assoc % :yyyy yyyy :mm mm))
                                           (when chan-update
                                             (put! chan-update 1))
                                           (when update-fn
                                             (update-fn))
                                           1))
                             :text     (dom/span nil
                                                 (glyphicon/render "step-backward")
                                                 " Назад")
                             }))

                 (dom/h4 nil "Год " yyyy " месяц " mm)

                 (dom/span #js {:className "input-group-btn"}
                           (button/render
                            {:type     :default
                             :title    "месяц вперед"
                             :on-click (fn [_]
                                         (let [[yyyy mm] (if (= mm 12)
                                                           [(inc yyyy) 1]
                                                           [yyyy (inc mm)])
                                               ]
                                           (om/transact! app #(assoc % :yyyy yyyy :mm mm))
                                           (when chan-update
                                             (put! chan-update 1))
                                           (when update-fn
                                             (update-fn))
                                           1))
                             :text     (dom/span nil "Вперед "
                                                 (glyphicon/render "step-forward"))
                             })

                           (button/render
                            {:type     :default
                             :title    "перейти на текущий месяц"
                             :on-click (fn [_]
                                         (om/update! app (app-init-fn (new js/Date)))
                                         (when chan-update
                                           (put! chan-update 1))
                                         (when update-fn
                                             (update-fn))
                                         1)
                             :text     (glyphicon/render "time")
                             })))))))
