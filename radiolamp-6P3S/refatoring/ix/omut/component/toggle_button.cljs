(ns ix.omut.component.toggle-button
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.component.helper-p :as helper-p]
            [ix.omut.common-input :as common-input]
            [ix.omut.ui.button :as button]))



(def app-init
  {:value false})

(defn component [app _ {:keys [bs-type
                               class+
                               onClick-fn
                               text-on
                               text-off
                               disabled?]
                        :or   {bs-type :default
                               class+  ""
                               text-on "вкл." text-off "выкл."}}]
  (reify
    om/IRender
    (render [_]
      (button/render {:type      bs-type
                      :active?   (@app :value)
                      :disabled? disabled?
                      :on-click  (fn [_]
                                   (om/transact! app :value not)
                                   (when onClick-fn (onClick-fn)))
                      :text      (if (@app :value) text-on text-off)}))))


(defn component-form-group [app owner {:keys [label
                                              type
                                              label-class+
                                              input-class+
                                              spec-]
                                       :or   {label              "Метка"
                                              label-class+       "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                              input-class+       "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                              spec- {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-})
                        (om/build helper-p/component app {}) )))))
