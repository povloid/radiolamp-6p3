(ns r6p3s.ui.datetime
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.core :as rc]
            [r6p3s.ui.glyphicon :as gicon]))

(defn render [d]
  (dom/span
   #js {:className "text-muted"}
   (gicon/render "time") " "
   (rc/date-com-format-datetime d)))
