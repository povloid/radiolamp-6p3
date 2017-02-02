(ns r6p3s.cpt.toggle-button
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.cpt.helper-p :as helper-p]
            [r6p3s.common-form :as common-form]
            [r6p3s.common-input :as common-input]
            [r6p3s.ui.button :as button]))



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
                                       :or   {label        "Метка"
                                              label-class+ common-form/label-class
                                              input-class+ common-form/input-class
                                              spec-        {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-})
                        (om/build helper-p/component app {}) )))))
