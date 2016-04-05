(ns ix.omut.ui.table
  (:require [om.dom :as dom :include-macros true]))





(defn render [{:keys [striped?
                      bordered?
                      condensed?
                      hover?
                      responsive?
                      responsive-class+
                      class+
                      style+
                      thead
                      tbody]
               :or   {responsive-class+ ""
                      class+            ""}}]
  (let [table (dom/table #js {:className (str "table "
                                              (if striped? "table-striped " "")
                                              (if bordered? "table-bordered " "")
                                              (if condensed? "table-condensed " "")
                                              (if hover? "table-hover " "")
                                              class+)
                              :style     style+}
                         thead
                         tbody)]
    (if responsive?
      (dom/div #js {:className (str "table-responsive " responsive-class+)} table)
      table)))
