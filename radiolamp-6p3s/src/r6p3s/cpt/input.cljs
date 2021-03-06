(ns r6p3s.cpt.input
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.common-form :as common-form]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.helper-p :as helper-p]))



;; Пример от Девида Нолена, полезен для ввода чиселj...
;; (defn input-0 [app owner]
;;   (reify
;;     om/IInitState
;;     (init-state [_]
;;       {:value "" :count 0})
;;     om/IRenderState
;;     (render-state [_ {:keys [value]}]
;;       (dom/div nil
;;                (dom/label nil "Only numeric : ")
;;                (dom/input #js
;;                           {:value value
;;                            :onChange
;;                            #(let [new-value (-> % .-target .-value)]
;;                               (if (js/isNaN new-value)
;;                                 (om/set-state! owner :value value)
;;                                 (om/set-state! owner :value new-value)))})))))


(def app-init
  {:value ""})

(defn value [app] (get app :value))

(defn set-value! [app value]
  (assoc app :value value))

(defn component
  [app owner {:keys [class+
                     type
                     onChange-valid?-fn
                     onChange-updated-valid-fn
                     onChange-updated-fn
                     onKeyPress-fn
                     onKeyDown-fn
                     onKeyUp-fn
                     placeholder
                     autocomplete
                     readonly?
                     min max step
                     style]
              :or   {class+             ""
                     type               "text"
                     onChange-valid?-fn (fn [_ _] true)
                     ;;onKeyPress-fn      (fn [_] nil)
                     ;;onKeyUp-fn         (fn [_] nil)
                     placeholder        ""
                     }}]
  (reify
    om/IRender
    (render [this]
      (let [value (or (:value @app) "")]
        (dom/input #js {:value    value
                        :onChange (fn [e]
                                    (let [new-value (.. e -target -value)]
                                      (when (onChange-valid?-fn app new-value)
                                        (when onChange-updated-valid-fn
                                          (onChange-updated-valid-fn)))
                                      
                                      (om/update! app :value new-value)
                                      
                                      (when onChange-updated-fn
                                        (onChange-updated-fn))))
                        :onKeyPress   onKeyPress-fn
                        :onKeyDown    onKeyDown-fn
                        :onKeyUp      onKeyUp-fn
                        :type         type
                        :min          min :max max :step step
                        :placeholder  placeholder
                        :disabled     (@app :disabled?)
                        :className    (str "form-control " class+)
                        :autocomplete autocomplete
                        :style        style})))))

(defn component-form-group  [app owner {:keys [label
                                               type
                                               label-class+
                                               label-style
                                               input-class+
                                               spec-input]
                                        :or   {label        "Метка"
                                               label-class+ common-form/label-class
                                               input-class+ common-form/input-class
                                               spec-input   {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+)
                               :style label-style} label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-input})
                        (om/build helper-p/component app {}))))))
