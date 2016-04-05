(ns r6p3s.ui.cdate-udate
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.core :as rc]
            [r6p3s.ui.glyphicon :as gicon]
            [r6p3s.ui.datetime :as datetime]))


(defn render [{:keys [cdate udate]}]
  (dom/span
   #js {:className "text-muted"}
   (gicon/render "time") " создан "
   (rc/date-com-format-datetime cdate) " "
   (gicon/render "time") " обновлен "
   (rc/date-com-format-datetime udate)))
