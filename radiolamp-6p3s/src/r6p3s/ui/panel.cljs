(ns r6p3s.ui.panel
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.ui.glyphicon :as gicon]
            [r6p3s.ui.font-icon :as font-icon]))




(defn render [{:keys [heading
                      heading-style
                      heading-glyphicon
                      heading-font-icon
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
                      (when heading-font-icon
                        (font-icon/render heading-font-icon))
                      (when (or  heading-glyphicon
                                 heading-font-icon)
                        " ")
                      heading
                      (when badge (dom/span #js {:className "badge"
                                                 :style     #js {:float "right"}} badge))))

           (when body
             (apply
              dom/div #js {:className "panel-body" :style body-style}
              (if (coll? body) body [body])))

           after-body))
