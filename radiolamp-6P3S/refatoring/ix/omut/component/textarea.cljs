(ns ix.omut.component.textarea
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.component.common-input :as common-input]
            [ix.omut.component.helper-p :as helper-p]))




(def app-init
  {:value ""})

(defn value [app] (get app :value))

(defn component [app owner {:keys [class+
                                   onChange-valid?-fn
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
                                        (let [v (.. e -target -value)]
                                          (when (onChange-valid?-fn app v)
                                            (om/update! app :value v))))
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
                                               label-class+  "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                                               input-class+  "col-xs-12 col-sm-8 col-md-8 col-lg-8"
                                               spec-textarea {}}}]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className (str "form-group " (common-input/input-css-string-has? app))}
               (dom/label #js {:className (str "control-label " label-class+) } label)
               (dom/div #js {:className input-class+ :style #js {}}
                        (om/build component app {:opts spec-textarea})
                        (om/build helper-p/component app {}))))))