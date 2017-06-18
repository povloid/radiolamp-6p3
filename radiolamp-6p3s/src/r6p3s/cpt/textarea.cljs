(ns r6p3s.cpt.textarea
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.common-form :as common-form]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.helper-p :as helper-p]))




(def app-init
  {:value ""})

(defn value [app] (get app :value))

(defn set-value! [app value]
  (assoc app :value value))

(defn component [app owner {:keys [class+
                                   onChange-valid?-fn
                                   onChange-updated-valid-fn
                                   onChange-updated-fn
                                   onKeyPress-fn
                                   placeholder
                                   readonly
                                   required
                                   maxlength
                                   rows
                                   wrap
                                   cols]
                            :or   {class+             ""
                                   onChange-valid?-fn (fn [_ _] true)
                                   onKeyPress-fn      (fn [_] nil)
                                   placeholder        ""
                                   readonly           ""
                                   required           ""
                                   maxlength          1000
                                   rows               "5"
                                   wrap               ""
                                   cols               "40"}}]
  (reify
    om/IRender
    (render [this]
      (dom/textarea #js {:value       (or (:value @app) "")
                         :onChange    (fn [e]
                                        (let [new-value (.. e -target -value)]
                                          (when (onChange-valid?-fn app new-value)
                                            (when onChange-updated-valid-fn
                                              (onChange-updated-valid-fn)))

                                          (om/update! app :value new-value)

                                          (when onChange-updated-fn
                                            (onChange-updated-fn))))
                         :onKeyPress  onKeyPress-fn
                         :placeholder placeholder
                         :className   (str "form-control " class+)
                         :readOnly    readonly
                         :required    required
                         :maxLength   maxlength
                         :rows        rows
                         :wrap        wrap
                         :cols        cols
                         }))))

(defn component-form-group  [app owner {:keys [label
                                               type
                                               label-class+
                                               input-class+
                                               spec-textarea]
                                        :or   {label         "Метка"
                                               label-class+  common-form/label-class
                                               input-class+  common-form/input-class
                                               spec-textarea {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-textarea})
                        (om/build helper-p/component app {}))))))
