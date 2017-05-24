(ns r6p3s.cpt.diff-panel
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [put! chan <!]]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [r6p3s.core :as rc]
            [r6p3s.ui.button :as button]
            [r6p3s.ui.glyphicon :as glyphicon]
            [r6p3s.ui.label :as label]
            [r6p3s.ui.media :as media]
            [r6p3s.ui.media-object :as media-object]
            [r6p3s.ui.table :as table]
            [r6p3s.ui.datetime :as datetime]
            [r6p3s.ui.group-box :as group-box]))




(def app-init
  {})


(defn component [app own
                 {:keys [] :as opts}]
  (reify
    om/IRender
    (render[_]
      (dom/div
       #js {:className ""}
       "OM: component"))))




