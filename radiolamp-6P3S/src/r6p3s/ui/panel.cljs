(ns r6p3s.ui.panel
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.ui.glyphicon :as gicon]))




(defn render [{:keys [heading
                      heading-style
                      heading-glyphicon
                      badge
                      body
                      body-style
                      after-body
                      type
                      style
                      class+]}]
  (dom/div #js {:className (str "panel panel-"
                                (get {:default "default"
                                      :primary "primary"
                                      :success "success"
                                      :info    "info"
                                      :warning "warning"
                                      :danger  "danger"
                                      } type "default")
                                (or (str " " class+) ""))
                :style     style}
           (when heading
             (dom/div #js {:className "panel-heading" :style heading-style}
                      (when heading-glyphicon
                        (gicon/render heading-glyphicon))
                      (when heading-glyphicon " ")
                      heading
                      (when badge (dom/span #js {:className "badge"
                                                 :style     #js {:float "right"}} badge))))

           (when body
             (apply
              dom/div #js {:className "panel-body" :style body-style}
              (if (coll? body) body [body])))

           after-body))
