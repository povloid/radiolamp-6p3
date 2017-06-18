(ns r6p3s.ui.button
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]))



(defn render [{:keys [text
                      title
                      type
                      size
                      block?
                      disabled?
                      active?
                      on-click
                      style
                      class+]
               :or   {text "Кнопка"
                      type :default}}]
  (dom/button #js {:className  (str "btn "
                                    ({:default "btn-default"
                                      :primary "btn-primary"
                                      :success "btn-success"
                                      :info    "btn-info"
                                      :warning "btn-warning"
                                      :danger  "btn-danger"
                                      :link    "btn-link"} type)
                                    (if size ({:lg " btn-lg"
                                               :sm " btn-sm"
                                               :xs " btn-xs"} size)
                                        "")
                                    (if block? " btn-block" "")
                                    (if active? " active" "")
                                    (if class+ (str " " class+) ""))
                   :title      title
                   :type       "button"
                   :disabled   (if disabled? "disabled" "")
                   :onClick    (c/on-click-com-fn on-click)
                   :onTouchEnd (c/on-click-com-fn on-click)
                   :style      style}
              text))
