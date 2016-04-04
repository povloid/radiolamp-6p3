(ns ix.omut.ui.form-group
  (:require [om.dom :as dom :include-macros true]))


(defn render [{:keys [label label-class+ input-class+ body]
               :or   {label        "метка"
                      label-class+ "col-xs-12 col-sm-4 col-md-4 col-lg-4"
                      input-class+ "col-xs-12 col-sm-8 col-md-8 col-lg-8"}}]
  (dom/div #js {:className "form-group"}
           (dom/label #js {:className (str "control-label " label-class+) } label)
           (dom/div #js {:className input-class+ :style #js {}}
                    body)))
