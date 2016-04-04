(ns ix.omut.ui.cdate-udate
  (:require [om.dom :as dom :include-macros true]
            [ix.omut.core :as c]
            [ix.omut.ui.glyphicon :as gicon]
            [ix.omut.ui.datetime :as datetime]))


(defn render [{:keys [cdate udate]}]
  (dom/span
   #js {:className "text-muted"}
   (gicon/render "time") " создан "
   (c/date-com-format-datetime cdate) " "
   (gicon/render "time") " обновлен "
   (c/date-com-format-datetime udate)))
