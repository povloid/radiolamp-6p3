(ns r6p3s.cpt.input-date
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as c]
            [r6p3s.common-form :as common-form]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.helper-p :as helper-p]))




(defn- date-to-date [d1]
  (let  [ds (c/date-com-format-date d1)
         d  (c/str-to-date ds)]
    [ds d]))



(def app-init
  (let [[ds d] (date-to-date (new js/Date))]
    {:value       d
     :input-value ds}))


(defn date [app]
  (app :value))

(defn set-date! [app nd]
  (let [[ds d] (date-to-date nd)]
    (assoc app
           :value       d
           :input-value ds)))





(defn component
  [app _ {:keys [class+
                 type
                 onChange-updated-valid-fn
                 onChange-valid?-fn
                 onChange-updated-fn
                 onKeyPress-fn
                 onKeyDown-fn
                 onKeyUp-fn
                 readonly?
                 min max step
                 style]
          :or   {class+             ""
                 onChange-valid?-fn common-input/vldfn-not-empty-date}}]
  (reify
    om/IRender
    (render [this]
      (let [app-v @app]
        (dom/input #js {:value    (app-v :input-value)
                        :onChange (fn [e]
                                    (let [new-value (.. e -target -value)]
                                      (when (onChange-valid?-fn app new-value)
                                        (do
                                          (om/update! app :value (c/str-to-date new-value))
                                          (when onChange-updated-valid-fn
                                            (onChange-updated-valid-fn))))
                                      
                                      (om/update! app :input-value new-value)

                                      (when onChange-updated-fn
                                        (onChange-updated-fn))))
                        :onKeyPress  onKeyPress-fn
                        :onKeyDown   onKeyDown-fn
                        :onKeyUp     onKeyUp-fn
                        :style       style
                        :type        "date"
                        :placeholder c/date-com-format-date
                        :disabled    (app-v :disabled?)
                        :className   (str "form-control " class+)})))))





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
