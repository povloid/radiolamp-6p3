(ns r6p3s.ui.label
  (:require [om.dom :as dom :include-macros true]))



(defn render [type text & [{:keys [style]}]]
  (dom/span #js {:className (str "label label-"
                                 (get {:default "default"
                                       :primary "primary"
                                       :success "success"
                                       :info    "info"
                                       :warning "warning"
                                       :danger  "danger"
                                       } type "default"))
                 :style     (or style #js {:whiteSpace "normal"})} text))

