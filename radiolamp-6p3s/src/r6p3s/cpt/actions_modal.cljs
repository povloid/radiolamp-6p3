(ns r6p3s.cpt.actions-modal
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.cpt.modal :as modal]
            [r6p3s.cpt.actions-modal-button :as actions-modal-button]))


(def app-init modal/app-init)

(defn component [app owner {:keys [chan-open]}]
  (reify
    om/IInitState
    (init-state [_]
      {:actions
       {:label "Пусто"
        :acts  []}})
    om/IWillMount
    (will-mount [this]
      (when chan-open
        (go
          (while true
            (let [actions (<! chan-open)]
              (om/set-state! owner :actions actions)
              (modal/show app))))))
    om/IRenderState
    (render-state [_ {{:keys [label acts]} :actions}]
      (om/build modal/component app
                {:opts {:label      label
                        :modal-size :sm
                        :body       (let [buttons (->> acts
                                                       (filterv (comp not nil?))
                                                       (map
                                                        (fn [opts]
                                                          (om/build actions-modal-button/component
                                                                    app {:opts opts}))))]
                                      (if (empty? buttons)
                                        (dom/h2 nil "Действий нет")
                                        (apply dom/div nil buttons)))
                        }}
                ))))
