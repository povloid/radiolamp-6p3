(ns ix.omut.ui.label
  (:require [om.dom :as dom :include-macros true]))



(defn render [type text]
  (dom/span #js {:className (str "label label-"
                                 (get {:default "default"
                                       :primary "primary"
                                       :success "success"
                                       :info    "info"
                                       :warning "warning"
                                       :danger  "danger"
                                       } type "default"))
                 :style     #js {:whiteSpace "normal"}} text))
