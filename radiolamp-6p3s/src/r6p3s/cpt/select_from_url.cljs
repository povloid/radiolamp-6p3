(ns r6p3s.cpt.select-from-url
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.net :as rnet]
            [r6p3s.core :as c]
            [r6p3s.common-form :as common-form]
            [r6p3s.common-input :as common-input]
            [r6p3s.cpt.select :as select]
            [r6p3s.cpt.helper-p :as helper-p]))



(def app-init select/app-init)


(defn component [app owner {:keys [url params] :as select-opts}]
  (reify
    om/IWillMount
    (will-mount [_]
      (rnet/get-data
       url params
       (fn [result]
         ;;(println result " to " @app)
         (om/update! app :list result))))
    om/IRender
    (render [_]
      (om/build select/component app {:opts select-opts}))))



(defn component-form-group  [app _ {:keys [label
                                           type
                                           label-class+
                                           input-class+
                                           spec-select]
                                    :or   {label        "Метка"
                                           label-class+ common-form/label-class
                                           input-class+ common-form/input-class
                                           spec-select  {}}}]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-select})
                        (om/build helper-p/component app {}))))))
