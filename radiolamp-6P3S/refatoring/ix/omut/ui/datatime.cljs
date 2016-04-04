(ns ix.omut.ui.datetime
  (:require [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.ui.glyphicon :as gicon]))

(defn render [d]
  (dom/span
   #js {:className "text-muted"}
   (gicon/render "time") " "
   (c/date-com-format-datetime d)))
