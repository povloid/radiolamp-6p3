(ns r6p3s.ui.form-group
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.common-form :as common-form]))


(defn render [{:keys [label label-class+ input-class+ body]
               :or   {label        "метка"
                      label-class+ common-form/label-class
                      input-class+ common-form/input-class}}]
  (dom/div #js {:className "form-group"}
           (dom/label #js {:className (str "control-label " label-class+) } label)
           (dom/div #js {:className input-class+ :style #js {}}
                    body)))
