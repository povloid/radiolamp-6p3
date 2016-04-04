(ns ix.omut.component.alert
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))





#_(def app-init
    {:alert-muted   nil
     :alert-primary nil
     :alert-success nil
     :alert-info    nil
     :alert-warning nil
     :alert-danger  nil})


(defn component [app _]
  (reify
    om/IRender
    (render [_]
      (let [{:keys [alert-muted
                    alert-primary
                    alert-success
                    alert-info
                    alert-warning
                    alert-danger]} app]
        (dom/div nil
                 (when alert-muted
                   (dom/div #js {:className "alert alert-muted"} alert-muted))
                 (when alert-primary
                   (dom/div #js {:className "alert alert-primary"} alert-primary))
                 (when alert-success
                   (dom/div #js {:className "alert alert-success"} alert-success))
                 (when alert-info
                   (dom/div #js {:className "alert alert-info"} alert-info))
                 (when alert-warning
                   (dom/div #js {:className "alert alert-warning"} alert-warning))
                 (when alert-danger
                   (dom/div #js {:className "alert alert-danger"} alert-danger))  )))))


(defn clean [app]
  (om/transact! app
                (fn [app]
                  (dissoc app
                          :alert-muted
                          :alert-primary
                          :alert-success
                          :alert-info
                          :alert-warning
                          :alert-danger))))

(defn clean-and-set!! [app k message]
  (om/transact! app
                (fn [app]
                  (-> app
                      (dissoc :alert-muted
                              :alert-primary
                              :alert-success
                              :alert-info
                              :alert-warning
                              :alert-danger)
                      (assoc k message)))))
