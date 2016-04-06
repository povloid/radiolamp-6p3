(ns r6p3s.cpt.helper-p
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))



#_(def app-init
    {:text-muted   nil
     :text-primary nil
     :text-success nil
     :text-info    nil
     :text-warning nil
     :text-danger  nil})


(defn component [app _]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [text-muted
                    text-primary
                    text-success
                    text-info
                    text-warning
                    text-danger]} @app]
        (dom/div nil
                 (when text-muted
                   (dom/p #js {:className "text-muted"} text-muted))
                 (when text-primary
                   (dom/p #js {:className "text-primary"} text-primary))
                 (when text-success
                   (dom/p #js {:className "text-success"} text-success))
                 (when text-info
                   (dom/p #js {:className "text-info"} text-info))
                 (when text-warning
                   (dom/p #js {:className "text-warning"} text-warning))
                 (when text-danger
                   (dom/p #js {:className "text-danger"} text-danger))  )))))



(defn clean [app]
  (om/transact! app
                (fn [app]
                  (dissoc app
                          :text-muted
                          :text-primary
                          :text-success
                          :text-info
                          :text-warning
                          :text-danger))))

(defn clean-and-set!! [app k message]
  (om/transact! app (fn [app]
                      (-> app
                          (dissoc :text-muted
                                  :text-primary
                                  :text-success
                                  :text-info
                                  :text-warning
                                  :text-danger)
                          (assoc k message)))))
