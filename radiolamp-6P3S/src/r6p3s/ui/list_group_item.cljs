(ns r6p3s.ui.list-group-item
  (:require [om.dom :as dom :include-macros true]))


(defn render [{:keys [text type badge active? class+]}]
  (dom/a #js {:className (str  "list-group-item "
                               (get {:success " list-group-item-success"
                                     :info    " list-group-item-info"
                                     :warning " list-group-item-warning"
                                     :danger  " list-group-item-danger"
                                     } type "")
                               (if active? "active" "")
                               " " (or class+ "")
                               )}
         (when badge (dom/span #js {:className "badge"} badge))
         text))
