(ns r6p3s.cpt.input-select-checkboxes
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.io :as ix-io]
            [r6p3s.net :as rnet]
            [r6p3s.core :as c]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.helper-p :as helper-p]))


(def app-init
  {:data []})


(defn make-data [app key-text key-value rows]
  (assoc app :data (->> rows
                        (map (fn [row]
                               (assoc row
                                      :value (get row key-value false)
                                      :text  (get row key-text "..."))))
                        vec)))


(defn get-selected [key-value app]
  (->> app :data (filter key-value)))



(defn component [app own
                 {:keys [on-click-fn]
                  :or   {}
                  :as   opts}]
  (reify
    om/IRender
    (render [_]
      (->> app
           :data
           (map (fn [app-row]
                  (letfn [(on-click [_]
                            (om/transact! app-row :value not)
                            (when on-click-fn (on-click-fn)))]
                    (let [{:keys [value text]} @app-row]
                      #_(button/render
                         {:type      (if value :success :default)
                          :active?   value
                          :disabled? disabled?
                          :block?    true
                          :style     #js {:textAlign "left"}
                          :on-click  (fn [_]
                                       (om/transact! app-row :value not)
                                       (when onClick-fn (onClick-fn)))
                          :text      text})
                      (dom/div
                       #js {:className "checkbox"}
                       (dom/label
                        nil
                        (dom/input #js {:type       "checkbox"
                                        :checked    value
                                        :onClick    (c/on-click-com-fn on-click)
                                        :onTouchEnd (c/on-click-com-fn on-click)
                                        })
                        text))))))
           (apply dom/div #js {:className ""})))))


(defn component-form-group
  [app own {:keys [label
                   type
                   label-class+
                   input-class+
                   spec-input-select-checkboxes]
            :or   {label                        "Метка"
                   label-class+                 "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                   input-class+                 "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                   spec-input-select-checkboxes {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-input-select-checkboxes})
                        (om/build helper-p/component app {}))))))
