(ns r6p3s.ui.label
  (:require [om.dom :as dom :include-macros true]))



(defn render [type text & [{:keys [style]
                             :or   {style #js {:whiteSpace "normal"}}}]]
  (dom/span #js {:className (str "label label-"
                                 (get {:default "default"
                                       :primary "primary"
                                       :success "success"
                                       :info    "info"
                                       :warning "warning"
                                       :danger  "danger"
                                       } type "default"))
                 :style     style} text))
