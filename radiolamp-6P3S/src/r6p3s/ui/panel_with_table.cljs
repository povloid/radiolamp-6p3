(ns r6p3s.ui.panel-with-table
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.ui.table :as table]
            [r6p3s.ui.thead-tr :as thead-tr]
            [r6p3s.ui.panel :as panel]))




(defn render [{:keys [cols rows
                      striped? bordered? hover? responsive?]
               :or   {cols        [] rows []
                      striped?    true
                      bordered?   true
                      hover?      true
                      responsive? true}
               :as   options}]
  (panel/render
   (assoc options
          :after-body
          (table/render
           {:hover?      hover?
            :bordered?   bordered?
            :striped?    striped?
            :responsive? responsive?
            :thead       (->> cols
                              (map #(dom/th nil %))
                              thead-tr/render)
            :tbody       (->> rows
                              (map (fn [row]
                                     (apply
                                      dom/tr nil
                                      (vec (map #(dom/td nil %) row)))))
                              (apply dom/tbody nil))
            }))))
