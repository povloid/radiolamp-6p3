(ns r6p3s.ui.panel-with-table
  (:require [om.dom :as dom :include-macros true]
            [r6p3s.ui.table :as table]
            [r6p3s.ui.thead-tr :as thead-tr]
            [r6p3s.ui.panel :as panel]))





(defn render [{:keys [cols rows
                      striped? bordered? hover? responsive?]
               :or   {cols        []
                      rows        []
                      striped?    true
                      bordered?   true
                      hover?      true
                      responsive? true}
               :as   options}]
  (let [empty-cols? (empty? cols)
        t-width (when empty-cols?
                  (when-let [c (first rows)]
                    (str (quot 100 (count c)) "%")))]
    (panel/render
     (assoc options
            :after-body
            (table/render
             {:hover?      hover?
              :bordered?   bordered?
              :striped?    striped?
              :responsive? responsive?
              :thead       (->> cols
                                (map #(dom/th #js {:width t-width} %))
                                thead-tr/render)
              :tbody       (->> rows
                                (map (fn [row]
                                       (apply
                                        dom/tr nil
                                        (vec (map #(dom/td
                                                    (when empty-cols?
                                                      #js {:width t-width})
                                                    %)
                                                  row)))))
                                (apply dom/tbody nil))
              })))))
