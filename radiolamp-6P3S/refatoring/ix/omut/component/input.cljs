(ns ix.omut.component.input
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.component.common-input :as common-input]
            [ix.omut.component.helper-p :as helper-p]))



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

(defn component [app owner {:keys [class+
                                   type
                                   onChange-valid?-fn
                                   onChange-updated-fn
                                   onKeyPress-fn
                                   placeholder
                                   readonly?
                                   min max step]
                            :or   {class+             ""
                                   type               "text"
                                   onChange-valid?-fn (fn [_ _] true)
                                   onKeyPress-fn      (fn [_] nil)
                                   placeholder        ""
                                   }}]
  (reify
    om/IRender
    (render [this]
      (let [value (or (:value @app) "")]
        (dom/input #js {:value       value
                        :onChange    (fn [e]
                                       (let [new-value (.. e -target -value)]
                                         (if (onChange-valid?-fn app new-value)
                                           (om/update! app :value new-value)
                                           (om/update! app :value value))
                                         (when onChange-updated-fn
                                           (onChange-updated-fn))
                                         ))
                        :onKeyPress  onKeyPress-fn
                        :type        type
                        :min         min :max max :step step
                        :placeholder placeholder
                        :disabled    (@app :disabled?)
                        :className   (str "form-control " class+)})))))

(defn component-form-group  [app owner {:keys [label
                                               type
                                               label-class+
                                               input-class+
                                               spec-input]
                                        :or   {label        "Метка"
                                               label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                               input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                               spec-input   {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-input})
                        (om/build helper-p/component app {}))))))