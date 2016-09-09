(ns r6p3s.ui.th-title
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.ui.glyphicon :as glyphicon]))


(defn render [{:keys [colspan icon title style class-name]}]
  (dom/th #js {:className class-name
               :colSpan   colspan
               :style     style}
          (when icon (glyphicon/render icon ""))
          (when icon " ")
          title))
